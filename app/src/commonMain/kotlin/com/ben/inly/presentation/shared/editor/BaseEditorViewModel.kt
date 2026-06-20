package com.ben.inly.presentation.shared.editor

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AiEventBus
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.FormulaEngine
import com.ben.inly.domain.util.HtmlMetadataFetcher
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.components.DropTargetZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@Stable
data class FocusRequest(
    val id: String,
    val placeCursorAtEnd: Boolean = false,
    val nonce: String = UUID.randomUUID().toString()
)

abstract class BaseEditorViewModel(
    protected val repository: NoteRepository,
    protected val mediaStorageHelper: MediaStorageHelper,
    protected val reminderScheduler: ReminderScheduler,
    protected val audioRecorder: AudioRecorder
) : ViewModel() {

    // AI event bus
    init {
        viewModelScope.launch {
            AiEventBus.indexRequest.collect {
                forceSyncAndIndexForAi()
            }
        }
    }

    fun forceSyncAndIndexForAi() {
        GlobalScope.launch(Dispatchers.IO) {
            val currentHash = computeBlocksHash()
            if (currentHash != lastIndexedContentHash) {
                performSave()
                try {
                    performIndexing()
                    lastIndexedContentHash = currentHash
                    isAiIndexDirty = false

                    AiEventBus.notifyIndexComplete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    // ----

    protected val _blocks = MutableStateFlow<List<NoteBlock>>(emptyList())
    var lastIndexedContentHash: Int = 0
    fun computeBlocksHash(): Int {
        return _blocks.value
            .filter { !it.isDeleted }
            .joinToString(separator = "|") { block ->
                when (block) {
                    is TextBlock -> "${block.id}:${block.text}"
                    is HeadingBlock -> "${block.id}:${block.text}"
                    is CheckboxBlock -> "${block.id}:${block.text}:${block.isChecked}"
                    is BulletedListBlock -> "${block.id}:${block.text}"
                    is NumberedListBlock -> "${block.id}:${block.text}"
                    is ToggleBlock -> "${block.id}:${block.text}"
                    is CodeBlock -> "${block.id}:${block.code}"
                    is QuoteBlock -> "${block.id}:${block.text}"
                    is DatabaseBlock -> "${block.id}:${block.rows.size}:${block.columns.size}"
                    else -> block.id
                }
            }
            .hashCode()
    }

    val visibleBlocks: StateFlow<List<NoteBlock>> = _blocks.map { allBlocks ->
        val visible = mutableListOf<NoteBlock>()
        var skipUntilLevel: Int? = null
        for (block in allBlocks) {
            if (block.isDeleted) continue

            if (skipUntilLevel != null) {
                if (block.indentationLevel > skipUntilLevel) continue
                else skipUntilLevel = null
            }
            visible.add(block)
            if (block is ToggleBlock && !block.isExpanded) skipUntilLevel = block.indentationLevel
        }
        visible
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    protected val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    protected val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    protected var currentlyFocusedBlockId: String? = null
    protected var autosaveJob: Job? = null
    protected var indexingJob: Job? = null

    protected abstract suspend fun performSave()
    protected abstract suspend fun performIndexing()
    protected abstract fun getNoteTitleForReminder(): String

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val undoStack = mutableListOf<List<NoteBlock>>()
    private val redoStack = mutableListOf<List<NoteBlock>>()
    private var lastHistorySaveTime = 0L
    private val MAX_HISTORY_SIZE = 50

    private val historyLock = Any()

    protected var isAiIndexDirty = false

    open fun scheduleAutosave() {
        isAiIndexDirty = true

        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1000L)
            performSave()
        }
    }

    protected fun isNoteActuallyEmpty(blocks: List<NoteBlock>): Boolean {
        if (blocks.isEmpty()) return true
        if (blocks.size == 1) {
            val first = blocks.first()
            return first is TextBlock && first.text.isBlank()
        }
        return false
    }

    fun togglePinSelectedBlocks() {
        val toToggle = _selectedBlockIds.value
        if (toToggle.isEmpty()) return
        val now = System.currentTimeMillis()
        modifyBlocks(forceSave = true) { list ->
            fun pinRecursive(blocks: List<NoteBlock>): List<NoteBlock> =
                blocks.map { b ->
                    when {
                        b.id in toToggle -> b.withPin(!b.isPinned, now)
                        b is RowContainerBlock -> b.copy(
                            columns = b.columns.map { col ->
                                col.copy(blocks = pinRecursive(col.blocks))
                            },
                            updatedAt = now
                        )
                        else -> b
                    }
                }
            pinRecursive(list)
        }
        clearSelection()
        scheduleAutosave()
    }

    private var lastHistoryFocusedBlockId: String? = null

    protected fun modifyBlocks(forceSave: Boolean = false, action: (List<NoteBlock>) -> List<NoteBlock>) {
        lateinit var newList: List<NoteBlock>
        val currentList = _blocks.getAndUpdate { list ->
            val rawList = action(list)

            val segregatedList = if (rawList.any { it.isPinned }) {
                val pinned = rawList.filter { it.isPinned }
                val unpinned = rawList.filter { !it.isPinned }
                pinned + unpinned
            } else {
                rawList
            }

            val renumbered = if (segregatedList.any { it is NumberedListBlock }) {
                recalculateNumberedLists(segregatedList)
            } else {
                segregatedList
            }

            val withTrailingBlock = renumbered.toMutableList()
            val lastVisible = withTrailingBlock.lastOrNull { !it.isDeleted }
            val needsTrailing = lastVisible == null ||
                    lastVisible !is TextBlock ||
                    lastVisible.text.isNotEmpty() ||
                    lastVisible.isPinned
            if (needsTrailing) {
                withTrailingBlock.add(
                    TextBlock(
                        id = UUID.randomUUID().toString(),
                        text = "",
                        indentationLevel = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            newList = withTrailingBlock
            newList
        }

        if (currentList == newList) return

        synchronized(historyLock) {
            val now = System.currentTimeMillis()
            val isStructuralChange = currentList.size != newList.size ||
                    currentList.zip(newList).any { (old, new) ->
                        old.id != new.id || old::class != new::class
                    }
            val isTextOnlyChange = !isStructuralChange
            val enoughTimePassed = now - lastHistorySaveTime > 2500
            val blockChanged = lastHistoryFocusedBlockId != currentlyFocusedBlockId

            val shouldSave = forceSave || isStructuralChange || (isTextOnlyChange && (enoughTimePassed || blockChanged))

            if (shouldSave) {
                undoStack.add(currentList)
                if (undoStack.size > MAX_HISTORY_SIZE) undoStack.removeAt(0)
                redoStack.clear()
                lastHistorySaveTime = now
                lastHistoryFocusedBlockId = currentlyFocusedBlockId
            }

            lastWasStructural = isStructuralChange
            updateHistoryState()
        }
    }

    fun startHardwareRecording() {
        audioRecorder.startRecording()
    }

    fun stopHardwareRecording(blockId: String, cancel: Boolean = false) {
        val result = audioRecorder.stopRecording(cancel)
        if (result != null && !cancel) {
            handleVoiceRecorded(blockId, result.first, result.second)
        }
    }

    fun handleDbFilePicked(blockId: String, rowId: String, colId: String, uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                val cleanFileName = mediaInfo.localFileName.substringAfterLast("/")
                val now = System.currentTimeMillis()
                modifyBlocks { list ->
                    list.map { db ->
                        if (db.id == blockId && db is DatabaseBlock) {
                            val updatedRows = db.rows.map { row ->
                                if (row.id == rowId) {
                                    val currentFiles = row.cells[colId]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                                    val newEntry = "${cleanFileName}|${mediaInfo.originalName}"
                                    val combined = (currentFiles + newEntry).joinToString(",")

                                    val newMap = row.cells.toMutableMap()
                                    newMap[colId] = combined
                                    row.copy(cells = newMap, updatedAt = now)
                                } else row
                            }
                            db.copy(rows = updatedRows, updatedAt = now)
                        } else db
                    }
                }
                scheduleAutosave()
            }
        }
    }

    fun stopDbHardwareRecording(blockId: String, rowId: String, colId: String, cancel: Boolean = false) {
        val result = audioRecorder.stopRecording(cancel)
        if (result != null && !cancel) {
            val cleanFileName = result.first.substringAfterLast("/")
            val now = System.currentTimeMillis()
            modifyBlocks { list ->
                list.map { db ->
                    if (db.id == blockId && db is DatabaseBlock) {
                        val updatedRows = db.rows.map { row ->
                            if (row.id == rowId) {
                                val currentFiles = row.cells[colId]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

                                val newEntry = "${cleanFileName}|Audio Recording.m4a"
                                val combined = (currentFiles + newEntry).joinToString(",")

                                val newMap = row.cells.toMutableMap()
                                newMap[colId] = combined
                                row.copy(cells = newMap, updatedAt = now)
                            } else row
                        }
                        db.copy(rows = updatedRows, updatedAt = now)
                    } else db
                }
            }
            scheduleAutosave()
        }
    }

    fun playAudio(fileName: String, onComplete: () -> Unit) {
        audioRecorder.play(fileName, onComplete)
    }

    fun stopAudio() {
        audioRecorder.stopPlaying()
    }

    fun toggleCheckbox(blockId: String, isChecked: Boolean) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            mapBlockById(list, blockId, now) {
                if (it is CheckboxBlock) {
                    if (isChecked) reminderScheduler.cancel(blockId)
                    it.copy(isChecked = isChecked, updatedAt = now)
                } else it
            }
        }
        scheduleAutosave()
    }

    fun toggleToggleBlock(blockId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            mapBlockById(list, blockId, now) {
                if (it is ToggleBlock) it.copy(isExpanded = !it.isExpanded, updatedAt = now) else it
            }
        }
        scheduleAutosave()
    }

    fun toggleFormat(format: String) {
        val id = currentlyFocusedBlockId ?: return
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            mapBlockById(list, id, now) { b ->
                when (format) {
                    "bold" -> updateFormat(b, !b.isBold, b.isItalic, b.isStrikeThrough, b.isUnderlined, now)
                    "italic" -> updateFormat(b, b.isBold, !b.isItalic, b.isStrikeThrough, b.isUnderlined, now)
                    "strike" -> updateFormat(b, b.isBold, b.isItalic, !b.isStrikeThrough, b.isUnderlined, now)
                    "underline" -> updateFormat(b, b.isBold, b.isItalic, b.isStrikeThrough, !b.isUnderlined, now)
                    else -> b
                }
            }
        }
        scheduleAutosave()
    }

    private fun updateFormat(b: NoteBlock, bld: Boolean, itl: Boolean, stk: Boolean, und: Boolean, now: Long) = when (b) {
        is TextBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is HeadingBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is CheckboxBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is BulletedListBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is NumberedListBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is ToggleBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        is CodeBlock -> b
        is QuoteBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und, updatedAt = now)
        else -> b
    }

    fun adjustIndentation(increment: Boolean) {
        val id = currentlyFocusedBlockId ?: return
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            mapBlockById(list, id, now) { b ->
                val newLevel = if (increment) b.indentationLevel + 1 else maxOf(0, b.indentationLevel - 1)
                when (b) {
                    is TextBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is HeadingBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is CheckboxBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is BulletedListBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is NumberedListBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is ToggleBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    is QuoteBlock -> b.copy(indentationLevel = newLevel, updatedAt = now)
                    else -> b
                }
            }
        }
        scheduleAutosave()
    }

    fun changeFocusedBlockType(type: String) {
        val id = currentlyFocusedBlockId ?: return
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            spliceAtBlock(list, id, now) { mutable, idx ->
                val b = mutable[idx]
                val rawText = getBlockText(b)

                val slashIndex = rawText.lastIndexOf('/')
                val isActivelySearching = slashIndex != -1 && !rawText.substring(slashIndex).contains(" ")
                val cleanedText = if (isActivelySearching) rawText.substring(0, slashIndex) else rawText

                val isNonTextBlock = type == "divider_solid" || type == "divider_dots" || type == "voice"

                if (isNonTextBlock && cleanedText.isNotEmpty()) {
                    val updatedText = when (b) {
                        is TextBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is HeadingBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is CheckboxBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is BulletedListBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is NumberedListBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is ToggleBlock -> b.copy(text = cleanedText, updatedAt = now)
                        is CodeBlock -> b.copy(code = cleanedText, updatedAt = now)
                        is QuoteBlock -> b.copy(text = cleanedText, updatedAt = now)
                        else -> b
                    }
                    mutable[idx] = updatedText

                    val newId = UUID.randomUUID().toString()
                    val newBlock = when (type) {
                        "divider_solid" -> SolidDividerBlock(id = newId, indentationLevel = b.indentationLevel, updatedAt = now)
                        "divider_dots" -> ThreeDotDividerBlock(id = newId, indentationLevel = b.indentationLevel, updatedAt = now)
                        "voice" -> VoiceBlock(id = newId, indentationLevel = b.indentationLevel, updatedAt = now)
                        else -> b
                    }
                    mutable.add(idx + 1, newBlock.withPin(b.isPinned, now))
                } else {
                    val newBlock = when (type) {
                        "text" -> TextBlock(id, cleanedText, b.indentationLevel, updatedAt = now)
                        "h1" -> HeadingBlock(id, cleanedText, 1, b.indentationLevel, updatedAt = now)
                        "h2" -> HeadingBlock(id, cleanedText, 2, b.indentationLevel, updatedAt = now)
                        "checkbox" -> CheckboxBlock(id, cleanedText, false, b.indentationLevel, updatedAt = now)
                        "quote" -> QuoteBlock(id, cleanedText, b.indentationLevel, updatedAt = now)
                        "bullet" -> BulletedListBlock(id, cleanedText, b.indentationLevel, updatedAt = now)
                        "number" -> NumberedListBlock(id, cleanedText, 1, b.indentationLevel, updatedAt = now)
                        "toggle" -> ToggleBlock(id, cleanedText, true, b.indentationLevel, updatedAt = now)
                        "code" -> CodeBlock(id, cleanedText, updatedAt = now)
                        "voice" -> VoiceBlock(id, indentationLevel = b.indentationLevel, updatedAt = now)
                        "divider_solid" -> SolidDividerBlock(id = id, indentationLevel = b.indentationLevel, updatedAt = now)
                        "divider_dots" -> ThreeDotDividerBlock(id = id, indentationLevel = b.indentationLevel, updatedAt = now)
                        else -> b
                    }

                    mutable[idx] = newBlock.withPin(b.isPinned, now)

                    if (type == "toggle") {
                        val nextBlock = mutable.getOrNull(idx + 1)
                        if (nextBlock == null || nextBlock.indentationLevel <= b.indentationLevel) {
                            mutable.add(idx + 1, TextBlock(UUID.randomUUID().toString(), "", b.indentationLevel + 1, updatedAt = now))
                        }
                    }
                }
            }
        }
        scheduleAutosave()
    }

    private fun getBlockText(b: NoteBlock) = when (b) {
        is TextBlock -> b.text
        is HeadingBlock -> b.text
        is CheckboxBlock -> b.text
        is BulletedListBlock -> b.text
        is NumberedListBlock -> b.text
        is ToggleBlock -> b.text
        is CodeBlock -> b.code
        is QuoteBlock -> b.text
        else -> ""
    }

    fun handleEnter(id: String, textBefore: String, textAfter: String) {
        var blockToFocusId = ""
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            spliceAtBlock(list, id, now) { mutable, idx ->
                val cur = mutable[idx]
                val newId = UUID.randomUUID().toString()
                blockToFocusId = newId
                var insertIdx = idx + 1

                val updatedCurrent = when (cur) {
                    is TextBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is HeadingBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is CheckboxBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is BulletedListBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is NumberedListBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is ToggleBlock -> cur.copy(text = textBefore, updatedAt = now)
                    is CodeBlock -> cur.copy(code = textBefore, updatedAt = now)
                    is QuoteBlock -> cur.copy(text = textBefore, updatedAt = now)
                    else -> cur
                }

                val newBlock = when (cur) {
                    is CheckboxBlock -> CheckboxBlock(newId, textAfter, false, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                    is BulletedListBlock -> BulletedListBlock(newId, textAfter, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                    is NumberedListBlock -> NumberedListBlock(newId, textAfter, cur.number + 1, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                    is HeadingBlock -> TextBlock(newId, textAfter, 0, isPinned = cur.isPinned, updatedAt = now)
                    is QuoteBlock -> QuoteBlock(newId, textAfter, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                    is ToggleBlock -> {
                        if (cur.isExpanded) {
                            TextBlock(newId, textAfter, cur.indentationLevel + 1, isPinned = cur.isPinned, updatedAt = now)
                        } else {
                            var i = idx + 1
                            while (i < mutable.size && mutable[i].indentationLevel > cur.indentationLevel) i++
                            insertIdx = i
                            ToggleBlock(newId, textAfter, false, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                        }
                    }
                    else -> TextBlock(newId, textAfter, cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                }

                mutable[idx] = updatedCurrent
                mutable.add(insertIdx, newBlock)
            }
        }

        if (blockToFocusId.isNotEmpty()) {
            _focusRequest.value = FocusRequest(id = blockToFocusId)
            scheduleAutosave()
        }
    }

    fun handleBackspaceOnEmpty(id: String) {
        val currentBlocks = _blocks.value
        val idx = currentBlocks.indexOfFirst { it.id == id }
        if (idx == -1) return

        val cur = currentBlocks[idx]
        val now = System.currentTimeMillis()

        if (cur !is TextBlock) {
            modifyBlocks { list ->
                spliceAtBlock(list, id, now) { mutable, i ->
                    mutable[i] = TextBlock(cur.id, "", cur.indentationLevel, isPinned = cur.isPinned, updatedAt = now)
                }
            }
            scheduleAutosave()
            return
        }

        if (currentBlocks.size <= 1) return

        val prevBlock = currentBlocks.subList(0, idx).lastOrNull { !it.isDeleted }

        if (prevBlock != null) {
            val isMediaOrDivider = prevBlock is ImageBlock || prevBlock is DocumentBlock ||
                    prevBlock is DatabaseBlock || prevBlock is SolidDividerBlock ||
                    prevBlock is ThreeDotDividerBlock || prevBlock is BookmarkBlock ||
                    prevBlock is SketchBlock || prevBlock is VoiceBlock

            if (isMediaOrDivider) {
                modifyBlocks { list ->
                    spliceAtBlock(list, prevBlock.id, now) { mutable, i ->
                        mutable[i] = mutable[i].markDeleted()
                    }
                }
                scheduleAutosave()
            } else {
                _focusRequest.value = FocusRequest(id = prevBlock.id, placeCursorAtEnd = true)
                viewModelScope.launch {
                    delay(50)
                    modifyBlocks { list ->
                        spliceAtBlock(list, id, now) { mutable, i ->
                            mutable[i] = mutable[i].markDeleted()
                        }
                    }
                    scheduleAutosave()
                }
            }
        }
    }

    fun addBlankBlockBelowFocused() {
        val targetId = currentlyFocusedBlockId ?: _blocks.value.lastOrNull()?.id ?: return
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            val idx = list.indexOfFirst { it.id == targetId }
            val indent = if (idx != -1) list[idx].indentationLevel else 0
            val isPinnedContext = if (idx != -1) list[idx].isPinned else false
            val new = TextBlock(id = newId, text = "", indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
            list.toMutableList().apply {
                if (idx != -1) add(idx + 1, new) else add(new)
            }
        }
        _focusRequest.value = FocusRequest(id = newId)
        scheduleAutosave()
    }

    fun setFocusedBlock(id: String) { currentlyFocusedBlockId = id }
    fun clearFocusRequest() { _focusRequest.value = null }
    fun toggleSelection(id: String) { _selectedBlockIds.update { if (it.contains(id)) it - id else it + id } }
    fun clearSelection() { _selectedBlockIds.value = emptySet() }
    fun selectAllBlocks() { _selectedBlockIds.value = _blocks.value.map { it.id }.toSet() }

    fun getSelectedText(): String {
        val ids = _selectedBlockIds.value
        val found = mutableListOf<NoteBlock>()
        fun collect(blocks: List<NoteBlock>) {
            for (b in blocks) {
                if (b.id in ids) found.add(b)
                if (b is RowContainerBlock) b.columns.forEach { collect(it.blocks) }
            }
        }
        collect(_blocks.value)
        return found.joinToString("\n") { getBlockText(it) }
    }

    fun deleteSelectedBlocks() {
        val toDelete = _selectedBlockIds.value
        val now = System.currentTimeMillis()

        modifyBlocks { list ->
            fun deleteRecursive(blocks: List<NoteBlock>): List<NoteBlock> {
                return blocks.map { b ->
                    when {
                        b.id in toDelete -> b.markDeleted()
                        b is RowContainerBlock -> {
                            val newCols = b.columns.map { col ->
                                col.copy(blocks = deleteRecursive(col.blocks))
                            }.filter { col -> col.blocks.any { !it.isDeleted } }

                            when {
                                newCols.isEmpty() -> b.markDeleted()
                                newCols.size == 1 -> RowContainerBlock(
                                    id = "FLATTEN:${b.id}",
                                    columns = newCols,
                                    updatedAt = now
                                )
                                else -> b.copy(columns = newCols, updatedAt = now)
                            }
                        }
                        else -> b
                    }
                }
            }

            fun unwrapFlatten(blocks: List<NoteBlock>): List<NoteBlock> {
                val result = mutableListOf<NoteBlock>()
                for (b in blocks) {
                    if (b is RowContainerBlock && b.id.startsWith("FLATTEN:")) {
                        val surviving = b.columns.firstOrNull()?.blocks
                            ?.filter { !it.isDeleted }
                            ?: emptyList()
                        result.addAll(surviving)
                    } else if (b is RowContainerBlock) {
                        result.add(b.copy(columns = b.columns.map { col ->
                            col.copy(blocks = unwrapFlatten(col.blocks))
                        }))
                    } else {
                        result.add(b)
                    }
                }
                return result
            }

            val afterDelete = deleteRecursive(list)
            val afterUnwrap = unwrapFlatten(afterDelete)

            val hasVisible = afterUnwrap.any { !it.isDeleted }
            if (!hasVisible) {
                afterUnwrap + listOf(TextBlock(id = UUID.randomUUID().toString(), text = "", updatedAt = now))
            } else {
                afterUnwrap
            }
        }

        clearSelection()
        scheduleAutosave()
    }

    fun cutSelectedBlocks(): String {
        val text = getSelectedText()
        deleteSelectedBlocks()
        return text
    }

    fun addBlockAboveSelection() {
        val selected = _selectedBlockIds.value
        if (selected.isEmpty()) return
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            spliceAtBlock(list, selected.first(), now) { mutable, idx ->
                val targetLevel = mutable[idx].indentationLevel
                mutable.add(idx, TextBlock(id = UUID.randomUUID().toString(), text = "", indentationLevel = targetLevel, updatedAt = now))
            }
        }
        clearSelection()
        scheduleAutosave()
    }

    fun addBlockBelowSelection() {
        val selected = _selectedBlockIds.value
        if (selected.isEmpty()) return
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            spliceAtBlock(list, selected.last(), now) { mutable, idx ->
                val targetLevel = mutable[idx].indentationLevel
                mutable.add(idx + 1, TextBlock(id = UUID.randomUUID().toString(), text = "", indentationLevel = targetLevel, updatedAt = now))
            }
        }
        clearSelection()
        scheduleAutosave()
    }

    private var lastWasStructural = false

    fun updateBlockText(blockId: String, newText: String) {
        val now = System.currentTimeMillis()

        modifyBlocks(forceSave = false) { list ->
            mapBlockById(list, blockId, now) { b ->
                when (b) {
                    is TextBlock -> b.copy(text = newText, updatedAt = now)
                    is HeadingBlock -> b.copy(text = newText, updatedAt = now)
                    is CheckboxBlock -> b.copy(text = newText, updatedAt = now)
                    is BulletedListBlock -> b.copy(text = newText, updatedAt = now)
                    is NumberedListBlock -> b.copy(text = newText, updatedAt = now)
                    is ToggleBlock -> b.copy(text = newText, updatedAt = now)
                    is CodeBlock -> b.copy(code = newText, updatedAt = now)
                    is QuoteBlock -> b.copy(text = newText, updatedAt = now)
                    else -> b
                }
            }
        }
        scheduleAutosave()
    }

    private fun updateHistoryState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        val previousList = synchronized(historyLock) {
            if (undoStack.isEmpty()) return
            undoStack.last()
        }

        val focusId = currentlyFocusedBlockId
        var targetFocusId: String? = null

        fun isFocusable(b: NoteBlock) = b is TextBlock || b is HeadingBlock || b is CheckboxBlock ||
                b is BulletedListBlock || b is NumberedListBlock || b is ToggleBlock ||
                b is QuoteBlock || b is CodeBlock

        if (focusId != null && previousList.any { !it.isDeleted && it.id == focusId && isFocusable(it) }) {
            targetFocusId = focusId
        } else if (focusId != null) {
            val currentList = _blocks.value
            val removedIndex = currentList.indexOfFirst { it.id == focusId }
            if (removedIndex > 0) {
                targetFocusId = currentList.subList(0, removedIndex).lastOrNull { !it.isDeleted && isFocusable(it) }?.id
            }
        }

        if (targetFocusId == null) {
            targetFocusId = previousList.lastOrNull { !it.isDeleted && isFocusable(it) }?.id
        }

        val needsFocusShift = targetFocusId != null && targetFocusId != focusId

        viewModelScope.launch {
            if (needsFocusShift) {
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
                currentlyFocusedBlockId = targetFocusId
                delay(50)
            }

            synchronized(historyLock) {
                redoStack.add(_blocks.value)
                if (undoStack.isNotEmpty()) undoStack.removeAt(undoStack.lastIndex)
                _blocks.value = previousList
                lastWasStructural = true
                lastHistorySaveTime = System.currentTimeMillis()
                updateHistoryState()
            }
            scheduleAutosave()

            if (targetFocusId != null) {
                delay(50)
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
            }
        }
    }

    fun redo() {
        val nextList = synchronized(historyLock) {
            if (redoStack.isEmpty()) return
            redoStack.last()
        }

        val focusId = currentlyFocusedBlockId
        var targetFocusId: String? = null

        fun isFocusable(b: NoteBlock) = b is TextBlock || b is HeadingBlock || b is CheckboxBlock ||
                b is BulletedListBlock || b is NumberedListBlock || b is ToggleBlock ||
                b is QuoteBlock || b is CodeBlock

        if (focusId != null && nextList.any { !it.isDeleted && it.id == focusId && isFocusable(it) }) {
            targetFocusId = focusId
        } else if (focusId != null) {
            val currentList = _blocks.value
            val removedIndex = currentList.indexOfFirst { it.id == focusId }
            if (removedIndex > 0) {
                targetFocusId = currentList.subList(0, removedIndex).lastOrNull { !it.isDeleted && isFocusable(it) }?.id
            }
        }

        if (targetFocusId == null) {
            targetFocusId = nextList.lastOrNull { !it.isDeleted && isFocusable(it) }?.id
        }

        val needsFocusShift = targetFocusId != null && targetFocusId != focusId

        viewModelScope.launch {
            if (needsFocusShift) {
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
                currentlyFocusedBlockId = targetFocusId
                delay(50)
            }

            synchronized(historyLock) {
                undoStack.add(_blocks.value)
                if (redoStack.isNotEmpty()) redoStack.removeAt(redoStack.lastIndex)
                _blocks.value = nextList
                lastWasStructural = true
                lastHistorySaveTime = System.currentTimeMillis()
                updateHistoryState()
            }
            scheduleAutosave()

            if (targetFocusId != null) {
                delay(50)
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
            }
        }
    }

    protected fun findBlockById(blocks: List<NoteBlock>, id: String): NoteBlock? {
        for (b in blocks) {
            if (b.id == id) return b
            if (b is RowContainerBlock) {
                b.columns.forEach { col -> findBlockById(col.blocks, id)?.let { return it } }
            }
        }
        return null
    }

    protected fun mapBlockById(
        blocks: List<NoteBlock>,
        id: String,
        now: Long,
        transform: (NoteBlock) -> NoteBlock
    ): List<NoteBlock> = blocks.map { b ->
        when {
            b.id == id -> transform(b)
            b is RowContainerBlock -> {
                var changed = false
                val newCols = b.columns.map { col ->
                    val newBlocks = mapBlockById(col.blocks, id, now, transform)
                    if (newBlocks != col.blocks) { changed = true; col.copy(blocks = newBlocks) } else col
                }
                if (changed) b.copy(columns = newCols, updatedAt = now) else b
            }
            else -> b
        }
    }

    protected fun spliceAtBlock(
        blocks: List<NoteBlock>,
        id: String,
        now: Long,
        onFound: (MutableList<NoteBlock>, Int) -> Unit
    ): List<NoteBlock> {
        val idx = blocks.indexOfFirst { it.id == id }
        if (idx != -1) {
            val mutable = blocks.toMutableList()
            onFound(mutable, idx)
            return mutable
        }
        return blocks.map { b ->
            if (b is RowContainerBlock) {
                var changed = false
                val newCols = b.columns.map { col ->
                    val newBlocks = spliceAtBlock(col.blocks, id, now, onFound)
                    if (newBlocks != col.blocks) { changed = true; col.copy(blocks = newBlocks) } else col
                }
                if (changed) b.copy(columns = newCols, updatedAt = now) else b
            } else b
        }
    }

    protected fun recalculateNumberedLists(blocks: List<NoteBlock>): List<NoteBlock> {
        val uniqueBlocks = blocks.distinctBy { it.id }
        val counters = mutableMapOf<Int, Int>()
        val now = System.currentTimeMillis()
        return uniqueBlocks.map { block ->
            if (block is NumberedListBlock) {
                val currentNum = counters.getOrDefault(block.indentationLevel, 1)
                counters[block.indentationLevel] = currentNum + 1
                if (block.number != currentNum) {
                    block.copy(number = currentNum, updatedAt = now)
                } else block
            } else {
                val keysToReset = counters.keys.filter { it >= block.indentationLevel }
                keysToReset.forEach { counters.remove(it) }
                block
            }
        }
    }

    fun updateReminder(blockId: String, timestamp: Long?) {
        val now = System.currentTimeMillis()
        val blockText = (findBlockById(_blocks.value, blockId) as? CheckboxBlock)?.text ?: ""
        modifyBlocks { list ->
            mapBlockById(list, blockId, now) {
                if (it is CheckboxBlock) it.copy(reminderTimestamp = timestamp, updatedAt = now) else it
            }
        }
        scheduleAutosave()

        if (timestamp != null) {
            reminderScheduler.schedule(
                blockId = blockId,
                noteTitle = getNoteTitleForReminder(),
                text = blockText.ifBlank { "Unfinished task" },
                timestamp = timestamp
            )
        } else {
            reminderScheduler.cancel(blockId)
        }
    }

    fun insertNewMediaBlock(type: String) {
        val activeBlockId = currentlyFocusedBlockId ?: _focusRequest.value?.id ?: _selectedBlockIds.value.firstOrNull()
        var newIdToFocus: String? = null
        val now = System.currentTimeMillis()

        modifyBlocks { list ->
            val mutableList = list.toMutableList()
            val newId = UUID.randomUUID().toString()
            newIdToFocus = newId

            val activeIndex = if (activeBlockId != null) mutableList.indexOfFirst { it.id == activeBlockId } else mutableList.size - 1
            val indent = if (activeIndex != -1) mutableList[activeIndex].indentationLevel else 0
            val isPinnedContext = if (activeIndex != -1) mutableList[activeIndex].isPinned else false

            val newBlock = when (type) {
                "image" -> ImageBlock(id = newId, indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                "document" -> DocumentBlock(id = newId, indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                "bookmark" -> BookmarkBlock(id = newId, indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                "voice" -> VoiceBlock(id = newId, indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                "database" -> DatabaseBlock(id = newId, columns = listOf(DatabaseColumn(UUID.randomUUID().toString(), "Name", ColumnType.TEXT, updatedAt = now)), rows = emptyList(), indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                "sketch" -> com.ben.inly.domain.model.SketchBlock(id = newId, indentationLevel = indent, isPinned = isPinnedContext, updatedAt = now)
                else -> return@modifyBlocks list
            }

            if (activeIndex != -1) {
                val activeBlock = mutableList[activeIndex]
                if (activeBlock is TextBlock) {
                    val text = activeBlock.text
                    val slashIndex = text.lastIndexOf('/')
                    val isActivelySearching = slashIndex != -1 && !text.substring(slashIndex).contains(" ")

                    val cleanedText = if (isActivelySearching) text.substring(0, slashIndex) else text

                    if (cleanedText.isEmpty()) {
                        mutableList[activeIndex] = newBlock
                    } else {
                        mutableList[activeIndex] = activeBlock.copy(text = cleanedText, updatedAt = now)
                        mutableList.add(activeIndex + 1, newBlock)
                    }
                } else {
                    mutableList.add(activeIndex + 1, newBlock)
                }
            } else mutableList.add(newBlock)

            mutableList
        }
        newIdToFocus?.let { _focusRequest.value = FocusRequest(id = it) }
        scheduleAutosave()
    }

    fun updateSketchStrokes(blockId: String, strokes: List<com.ben.inly.domain.model.Stroke>) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map {
                if (it.id == blockId && it is com.ben.inly.domain.model.SketchBlock) {
                    it.copy(strokes = strokes, updatedAt = now)
                } else it
            }
        }
        scheduleAutosave()
    }

    fun handleUrlSubmit(blockId: String, url: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is BookmarkBlock) it.copy(url = url, title = "Loading...", updatedAt = now) else it } }
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = HtmlMetadataFetcher.fetchMetadata(url)
            modifyBlocks { list ->
                list.map {
                    if (it.id == blockId && it is BookmarkBlock)
                        it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl, updatedAt = System.currentTimeMillis())
                    else it
                }
            }
            scheduleAutosave()
        }
    }

    fun handleImagePicked(blockId: String, uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                modifyBlocks { list -> list.map { if (it.id == blockId && it is ImageBlock) it.copy(localFilePath = mediaInfo.localFileName, updatedAt = System.currentTimeMillis()) else it } }
                scheduleAutosave()
            }
        }
    }

    fun handleDocumentPicked(blockId: String, uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                modifyBlocks { list ->
                    list.map {
                        if (it.id == blockId && it is DocumentBlock) {
                            it.copy(localFilePath = mediaInfo.localFileName, fileName = mediaInfo.originalName, mimeType = mediaInfo.mimeType, fileSizeString = mediaInfo.formattedSize, updatedAt = System.currentTimeMillis())
                        } else it
                    }
                }
                scheduleAutosave()
            }
        }
    }

    fun deleteImageBlock(blockId: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId) it.markDeleted() else it } }
        scheduleAutosave()
    }

    fun handleVoiceRecorded(blockId: String, filePath: String, duration: Int) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is VoiceBlock) it.copy(localFilePath = filePath, durationSeconds = duration, updatedAt = now) else it } }
        scheduleAutosave()
    }

    fun handleRemoveVoice(blockId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is VoiceBlock) it.copy(localFilePath = null, durationSeconds = 0, updatedAt = now) else it } }
        scheduleAutosave()
    }

    fun updateDbTitle(blockId: String, newTitle: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(title = newTitle, updatedAt = now) else it } }
        scheduleAutosave()
    }

    fun addDbRow(blockId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(rows = it.rows + DatabaseRow(id = UUID.randomUUID().toString(), cells = emptyMap(), updatedAt = now), updatedAt = now) else it } }
        scheduleAutosave()
    }

    fun addDbColumn(blockId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(columns = it.columns + DatabaseColumn(id = UUID.randomUUID().toString(), name = "New Column", type = ColumnType.TEXT, updatedAt = now), updatedAt = now) else it } }
        scheduleAutosave()
    }

    suspend fun getNoteTitle(noteId: String): String {
        return repository.getNoteById(noteId)?.title ?: ""
    }

    fun updateDbCell(blockId: String, rowId: String, colId: String, newValue: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            mapBlockById(list, blockId, now) { block ->
                if (block is DatabaseBlock) {
                    val updatedRows = block.rows.map { row ->
                        if (row.id == rowId) {
                            val newMap = row.cells.toMutableMap()
                            newMap[colId] = newValue
                            block.columns.filter { it.type == ColumnType.FORMULA }.forEach { formulaCol ->
                                formulaCol.formulaExpression?.let { expr ->
                                    newMap[formulaCol.id] = FormulaEngine.evaluate(expr, newMap, block.columns)
                                }
                            }
                            row.copy(cells = newMap, updatedAt = now)
                        } else row
                    }
                    block.copy(rows = updatedRows, updatedAt = now)
                } else block
            }
        }
        scheduleAutosave()
    }

    fun updateDbFormula(blockId: String, colId: String, expression: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedCols = db.columns.map { col -> if (col.id == colId) col.copy(formulaExpression = expression, updatedAt = now) else col }
                    val updatedRows = db.rows.map { row ->
                        val newMap = row.cells.toMutableMap()
                        newMap[colId] = FormulaEngine.evaluate(expression, newMap, updatedCols)
                        row.copy(cells = newMap, updatedAt = now)
                    }
                    db.copy(columns = updatedCols, rows = updatedRows, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun updateDbColumn(blockId: String, colId: String, newName: String, newType: ColumnType) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(columns = db.columns.map { col -> if (col.id == colId) col.copy(name = newName, type = newType, updatedAt = now) else col }, updatedAt = now) else db } }
        scheduleAutosave()
    }

    fun updateDbColumnWidth(blockId: String, colId: String, newWidth: Int) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(columns = db.columns.map { col -> if (col.id == colId) col.copy(width = newWidth.coerceIn(40, 600), updatedAt = now) else col }, updatedAt = now) else db } }
        scheduleAutosave()
    }

    fun updateDbSort(blockId: String, colId: String, isAscending: Boolean?) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val modifiedSortList = db.activeSorts.toMutableList()
                    modifiedSortList.removeAll { it.columnId == colId }
                    if (isAscending != null) {
                        modifiedSortList.add(SortConfig(colId, isAscending))
                    }
                    db.copy(activeSorts = modifiedSortList, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun addDbFilter(blockId: String, colId: String, operator: String, value: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(activeFilters = db.activeFilters + FilterConfig(colId, operator, value), updatedAt = now) else db } }
        scheduleAutosave()
    }

    fun removeDbFilter(blockId: String, filter: FilterConfig) {
        val now = System.currentTimeMillis()
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(activeFilters = db.activeFilters - filter, updatedAt = now) else db } }
        scheduleAutosave()
    }

    fun reorderDbColumns(blockId: String, fromIndex: Int, toIndex: Int) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val cols = db.columns.toMutableList()
                    val moved = cols.removeAt(fromIndex)
                    cols.add(toIndex, moved)
                    db.copy(columns = cols, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun deleteDbColumn(blockId: String, colId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedCols = db.columns.map { col ->
                        if (col.id == colId) col.copy(isDeleted = true, updatedAt = now) else col
                    }
                    db.copy(columns = updatedCols, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun deleteDbRow(blockId: String, rowId: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedRows = db.rows.map { row ->
                        if (row.id == rowId) row.copy(isDeleted = true, updatedAt = now) else row
                    }
                    db.copy(rows = updatedRows, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun addDbRowAt(blockId: String, index: Int) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val rows = db.rows.toMutableList()
                    rows.add(index.coerceIn(0, rows.size), DatabaseRow(id = UUID.randomUUID().toString(), cells = emptyMap(), updatedAt = now))
                    db.copy(rows = rows, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun addDbColumnAt(blockId: String, index: Int) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val cols = db.columns.toMutableList()
                    cols.add(index.coerceIn(0, cols.size), DatabaseColumn(id = UUID.randomUUID().toString(), name = "New Column", type = ColumnType.TEXT, updatedAt = now))
                    db.copy(columns = cols, updatedAt = now)
                } else db
            }
        }
        scheduleAutosave()
    }

    override fun onCleared() {
        super.onCleared()
        autosaveJob?.cancel()

        GlobalScope.launch(Dispatchers.IO + NonCancellable) {
            performSave()
        }
    }

    // Database tags
    val globalTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createGlobalTag(name: String, colorHex: String): String {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateTag(newId, name, colorHex)
        }
        return newId
    }

    fun moveBlock(sourceId: String, targetId: String, zone: DropTargetZone) {
        if (sourceId == targetId) return
        val now = System.currentTimeMillis()

        modifyBlocks(forceSave = true) { list ->
            var extractedBlock: NoteBlock? = null
            fun extractBlockRecursive(blocks: List<NoteBlock>): List<NoteBlock> {
                val result = mutableListOf<NoteBlock>()
                for (b in blocks) {
                    if (b.id == sourceId) {
                        extractedBlock = b
                    } else if (b is RowContainerBlock) {
                        val newCols = b.columns.map { col ->
                            col.copy(blocks = extractBlockRecursive(col.blocks))
                        }.filter { it.blocks.isNotEmpty() }

                        when {
                            newCols.isEmpty() -> {
                                // entire row is gone
                            }
                            newCols.size == 1 -> {
                                result.addAll(newCols.first().blocks)
                            }
                            else -> {
                                result.add(b.copy(columns = newCols, updatedAt = now))
                            }
                        }
                    } else {
                        result.add(b)
                    }
                }
                return result
            }

            val listWithoutSource = extractBlockRecursive(list)
            val blockToInsert = extractedBlock ?: return@modifyBlocks list

            var insertionDone = false

            fun insertBlockRecursive(blocks: List<NoteBlock>): List<NoteBlock> {
                val result = mutableListOf<NoteBlock>()
                for (b in blocks) {
                    if (!insertionDone && b.id == targetId) {
                        insertionDone = true
                        when (zone) {
                            DropTargetZone.TOP -> {
                                result.add(blockToInsert)
                                result.add(b)
                            }
                            DropTargetZone.BOTTOM -> {
                                result.add(b)
                                result.add(blockToInsert)
                            }
                            DropTargetZone.LEFT -> {
                                result.add(RowContainerBlock(
                                    id = UUID.randomUUID().toString(),
                                    columns = listOf(
                                        ColumnBlock(UUID.randomUUID().toString(), listOf(blockToInsert), 1f),
                                        ColumnBlock(UUID.randomUUID().toString(), listOf(b), 1f)
                                    ),
                                    updatedAt = now
                                ))
                            }
                            DropTargetZone.RIGHT -> {
                                result.add(RowContainerBlock(
                                    id = UUID.randomUUID().toString(),
                                    columns = listOf(
                                        ColumnBlock(UUID.randomUUID().toString(), listOf(b), 1f),
                                        ColumnBlock(UUID.randomUUID().toString(), listOf(blockToInsert), 1f)
                                    ),
                                    updatedAt = now
                                ))
                            }
                            else -> result.add(b)
                        }
                    } else if (b is RowContainerBlock) {
                        val newCols = b.columns.map { col ->
                            col.copy(blocks = insertBlockRecursive(col.blocks))
                        }
                        result.add(b.copy(columns = newCols, updatedAt = now))
                    } else {
                        result.add(b)
                    }
                }
                return result
            }

            insertBlockRecursive(listWithoutSource)
        }
        scheduleAutosave()
    }

    fun updateColumnWeights(rowId: String, newWeights: List<Float>) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            fun updateRecursive(blocks: List<NoteBlock>): List<NoteBlock> {
                return blocks.map { b ->
                    if (b.id == rowId && b is RowContainerBlock) {
                        val updatedCols = b.columns.mapIndexed { index, col ->
                            col.copy(weight = newWeights.getOrElse(index) { col.weight })
                        }
                        b.copy(columns = updatedCols, updatedAt = now)
                    } else if (b is RowContainerBlock) {
                        b.copy(columns = b.columns.map { c -> c.copy(blocks = updateRecursive(c.blocks)) }, updatedAt = now)
                    } else {
                        b
                    }
                }
            }
            updateRecursive(list)
        }
        scheduleAutosave()
    }

    fun addBlockAbove(id: String) {
        val now = System.currentTimeMillis()
        val newId = UUID.randomUUID().toString()
        modifyBlocks { list ->
            spliceAtBlock(list, id, now) { mutable, idx ->
                val indent = mutable[idx].indentationLevel
                mutable.add(idx, TextBlock(id = newId, text = "", indentationLevel = indent, updatedAt = now))
            }
        }
        _focusRequest.value = FocusRequest(id = newId)
        scheduleAutosave()
    }

    fun addBlockBelow(id: String) {
        val now = System.currentTimeMillis()
        val newId = UUID.randomUUID().toString()
        modifyBlocks { list ->
            spliceAtBlock(list, id, now) { mutable, idx ->
                val indent = mutable[idx].indentationLevel
                mutable.add(idx + 1, TextBlock(id = newId, text = "", indentationLevel = indent, updatedAt = now))
            }
        }
        _focusRequest.value = FocusRequest(id = newId)
        scheduleAutosave()
    }

    // Aggregators
    fun updateDbAggregation(blockId: String, colId: String, aggregationType: String?) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock)
                    db.copy(columns = db.columns.map { c ->
                        if (c.id == colId) c.copy(aggregationType = aggregationType, updatedAt = now) else c
                    }, updatedAt = now)
                else db
            }
        }
        scheduleAutosave()
    }

    fun updateDbCurrency(blockId: String, colId: String, symbol: String) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock)
                    db.copy(columns = db.columns.map { c ->
                        if (c.id == colId) c.copy(currencySymbol = symbol, updatedAt = now) else c
                    }, updatedAt = now)
                else db
            }
        }
        scheduleAutosave()
    }

    fun updateDbFormulaCurrency(blockId: String, colId: String, enabled: Boolean) {
        val now = System.currentTimeMillis()
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock)
                    db.copy(columns = db.columns.map { c ->
                        if (c.id == colId) c.copy(isFormulaCurrency = enabled, updatedAt = now) else c
                    }, updatedAt = now)
                else db
            }
        }
        scheduleAutosave()
    }

    // Database table notes
    fun openDatabaseNote(
        blockId: String,
        rowId: String,
        colId: String,
        existingNoteId: String?,
        onNavigate: (String) -> Unit
    ) {
        if (!existingNoteId.isNullOrBlank()) {
            viewModelScope.launch {
                performSave()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onNavigate(existingNoteId)
                }
            }
            return
        }

        val newNoteId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val subNoteMeta = com.ben.inly.data.local.room.NoteMetadataEntity(
                noteId = newNoteId,
                title = "",
                folderId = null,
                isDaily = false,
                dateString = null,
                createdAt = now,
                updatedAt = now,
                filePath = "note_$newNoteId.json",
                isSubNote = true
            )

            repository.saveNote(subNoteMeta, com.ben.inly.domain.model.NoteContent(blocks = emptyList()))

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                updateDbCell(blockId, rowId, colId, newNoteId)
            }

            performSave()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onNavigate(newNoteId)
            }
        }
    }
}