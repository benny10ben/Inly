package com.ben.inly.presentation.shared.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.FormulaEngine
import com.ben.inly.domain.util.HtmlMetadataFetcher
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A standard data structure for passing focus requests down to the UI.
 * Keeping it outside the class allows UI components to import it easily.
 */
data class FocusRequest(
    val id: String,
    val placeCursorAtEnd: Boolean = false,
    val nonce: String = UUID.randomUUID().toString()
)

/**
 * The core engine behind the block-based editor.
 * Provides all the shared state, block manipulation logic, and media handling
 * required by both standalone notes and daily notes.
 */
abstract class BaseEditorViewModel(
    protected val repository: NoteRepository,
    protected val mediaStorageHelper: MediaStorageHelper,
    protected val reminderScheduler: ReminderScheduler,
    protected val audioRecorder: AudioRecorder
) : ViewModel() {

    protected val _blocks = MutableStateFlow<List<NoteBlock>>(emptyList())

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

    protected abstract suspend fun performSave()
    protected abstract fun getNoteTitleForReminder(): String

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val undoStack = mutableListOf<List<NoteBlock>>()
    private val redoStack = mutableListOf<List<NoteBlock>>()
    private var lastHistorySaveTime = 0L
    private val MAX_HISTORY_SIZE = 50

    open fun scheduleAutosave() {
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

    fun startHardwareRecording() {
        audioRecorder.startRecording()
    }

    fun stopHardwareRecording(blockId: String, cancel: Boolean = false) {
        val result = audioRecorder.stopRecording(cancel)
        if (result != null && !cancel) {
            handleVoiceRecorded(blockId, result.first, result.second)
        }
    }

    fun playAudio(fileName: String, onComplete: () -> Unit) {
        audioRecorder.play(fileName, onComplete)
    }

    fun stopAudio() {
        audioRecorder.stopPlaying()
    }

    fun toggleCheckbox(blockId: String, isChecked: Boolean) {
        modifyBlocks { list ->
            list.map {
                if (it.id == blockId && it is CheckboxBlock) {
                    if (isChecked) reminderScheduler.cancel(blockId)
                    it.copy(isChecked = isChecked)
                } else it
            }
        }
        scheduleAutosave()
    }

    fun toggleToggleBlock(blockId: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is ToggleBlock) it.copy(isExpanded = !it.isExpanded) else it } }
        scheduleAutosave()
    }

    fun toggleFormat(format: String) {
        val id = currentlyFocusedBlockId ?: return
        modifyBlocks { list ->
            list.map { b ->
                if (b.id != id) b else when (format) {
                    "bold" -> updateFormat(b, !b.isBold, b.isItalic, b.isStrikeThrough, b.isUnderlined)
                    "italic" -> updateFormat(b, b.isBold, !b.isItalic, b.isStrikeThrough, b.isUnderlined)
                    "strike" -> updateFormat(b, b.isBold, b.isItalic, !b.isStrikeThrough, b.isUnderlined)
                    "underline" -> updateFormat(b, b.isBold, b.isItalic, b.isStrikeThrough, !b.isUnderlined)
                    else -> b
                }
            }
        }
        scheduleAutosave()
    }

    private fun updateFormat(b: NoteBlock, bld: Boolean, itl: Boolean, stk: Boolean, und: Boolean) = when (b) {
        is TextBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is HeadingBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is CheckboxBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is BulletedListBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is NumberedListBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is ToggleBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        is CodeBlock -> b
        is QuoteBlock -> b.copy(isBold = bld, isItalic = itl, isStrikeThrough = stk, isUnderlined = und)
        else -> b
    }

    fun adjustIndentation(increment: Boolean) {
        val id = currentlyFocusedBlockId ?: return
        modifyBlocks { list ->
            list.map { b ->
                if (b.id != id) b
                else {
                    val newLevel = if (increment) b.indentationLevel + 1 else maxOf(0, b.indentationLevel - 1)
                    when (b) {
                        is TextBlock -> b.copy(indentationLevel = newLevel)
                        is HeadingBlock -> b.copy(indentationLevel = newLevel)
                        is CheckboxBlock -> b.copy(indentationLevel = newLevel)
                        is BulletedListBlock -> b.copy(indentationLevel = newLevel)
                        is NumberedListBlock -> b.copy(indentationLevel = newLevel)
                        is ToggleBlock -> b.copy(indentationLevel = newLevel)
                        is QuoteBlock -> b.copy(indentationLevel = newLevel)
                        else -> b
                    }
                }
            }
        }
        scheduleAutosave()
    }

    fun changeFocusedBlockType(type: String) {
        val id = currentlyFocusedBlockId ?: return
        modifyBlocks { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@modifyBlocks list

            val b = list[idx]
            val text = getBlockText(b)

            val newBlock = when (type) {
                "text" -> TextBlock(id, text, b.indentationLevel)
                "h1" -> HeadingBlock(id, text, 1, b.indentationLevel)
                "h2" -> HeadingBlock(id, text, 2, b.indentationLevel)
                "checkbox" -> CheckboxBlock(id, text, false, b.indentationLevel)
                "quote" -> QuoteBlock(id, text, b.indentationLevel)
                "bullet" -> BulletedListBlock(id, text, b.indentationLevel)
                "number" -> NumberedListBlock(id, text, 1, b.indentationLevel)
                "toggle" -> ToggleBlock(id, text, true, b.indentationLevel)
                "code" -> CodeBlock(id, text)
                "voice" -> VoiceBlock(id, indentationLevel = b.indentationLevel)
                else -> b
            }

            val newList = list.toMutableList()
            newList[idx] = newBlock

            if (type == "toggle") {
                val nextBlock = newList.getOrNull(idx + 1)
                if (nextBlock == null || nextBlock.indentationLevel <= b.indentationLevel) {
                    newList.add(idx + 1, TextBlock(UUID.randomUUID().toString(), "", b.indentationLevel + 1))
                }
            }
            newList
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
        modifyBlocks { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@modifyBlocks list
            val cur = list[idx]

            val newId = UUID.randomUUID().toString()
            blockToFocusId = newId
            var insertIdx = idx + 1

            val updatedCurrent = when (cur) {
                is TextBlock -> cur.copy(text = textBefore)
                is HeadingBlock -> cur.copy(text = textBefore)
                is CheckboxBlock -> cur.copy(text = textBefore)
                is BulletedListBlock -> cur.copy(text = textBefore)
                is NumberedListBlock -> cur.copy(text = textBefore)
                is ToggleBlock -> cur.copy(text = textBefore)
                is CodeBlock -> cur.copy(code = textBefore)
                is QuoteBlock -> cur.copy(text = textBefore)
                else -> cur
            }

            val newBlock = when (cur) {
                is CheckboxBlock -> CheckboxBlock(newId, textAfter, false, cur.indentationLevel)
                is BulletedListBlock -> BulletedListBlock(newId, textAfter, cur.indentationLevel)
                is NumberedListBlock -> NumberedListBlock(newId, textAfter, cur.number + 1, cur.indentationLevel)
                is HeadingBlock -> TextBlock(newId, textAfter, 0)
                is QuoteBlock -> QuoteBlock(newId, textAfter, cur.indentationLevel)
                is ToggleBlock -> {
                    if (cur.isExpanded) {
                        TextBlock(newId, textAfter, cur.indentationLevel + 1)
                    } else {
                        var i = idx + 1
                        while (i < list.size && list[i].indentationLevel > cur.indentationLevel) i++
                        insertIdx = i
                        ToggleBlock(newId, textAfter, false, cur.indentationLevel)
                    }
                }
                else -> TextBlock(newId, textAfter, cur.indentationLevel)
            }

            val newList = list.toMutableList()
            newList[idx] = updatedCurrent
            newList.add(insertIdx, newBlock)
            newList
        }

        if (blockToFocusId.isNotEmpty()) {
            _focusRequest.value = FocusRequest(id = blockToFocusId)
            scheduleAutosave()
        }
    }

    fun handleBackspaceOnEmpty(id: String) {
        var focusPrevId: String? = null
        modifyBlocks { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@modifyBlocks list
            val cur = list[idx]

            if (cur !is TextBlock) {
                val newList = list.toMutableList()
                newList[idx] = TextBlock(cur.id, "", cur.indentationLevel)
                return@modifyBlocks newList
            }

            if (list.size <= 1) return@modifyBlocks list

            focusPrevId = list.subList(0, idx).lastOrNull { !it.isDeleted }?.id

            val newList = list.toMutableList()
            newList[idx] = cur.markDeleted()
            newList
        }

        if (focusPrevId != null) {
            _focusRequest.value = FocusRequest(id = focusPrevId!!, placeCursorAtEnd = true)
        }
        scheduleAutosave()
    }

    fun addBlankBlockBelowFocused() {
        val targetId = currentlyFocusedBlockId ?: _blocks.value.lastOrNull()?.id ?: return
        val newId = UUID.randomUUID().toString()
        modifyBlocks { list ->
            val idx = list.indexOfFirst { it.id == targetId }
            val indent = if (idx != -1) list[idx].indentationLevel else 0
            val new = TextBlock(id = newId, text = "", indentationLevel = indent)
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

    fun getSelectedText() = _blocks.value.filter { it.id in _selectedBlockIds.value }.joinToString("\n") { getBlockText(it) }

    fun deleteSelectedBlocks() {
        val toDelete = _selectedBlockIds.value
        modifyBlocks { list ->
            val updatedList = list.map { if (it.id in toDelete) it.markDeleted() else it }

            if (updatedList.none { !it.isDeleted }) {
                updatedList + listOf(TextBlock(id = UUID.randomUUID().toString(), text = ""))
            } else {
                updatedList
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
        modifyBlocks { list ->
            val firstIndex = list.indexOfFirst { it.id in selected }
            if (firstIndex != -1) {
                val targetLevel = list[firstIndex].indentationLevel
                val newBlock = TextBlock(id = UUID.randomUUID().toString(), text = "", indentationLevel = targetLevel)
                val mutableList = list.toMutableList().apply { add(firstIndex, newBlock) }
                mutableList
            } else list
        }
        clearSelection()
        scheduleAutosave()
    }

    fun addBlockBelowSelection() {
        val selected = _selectedBlockIds.value
        if (selected.isEmpty()) return
        modifyBlocks { list ->
            val lastIndex = list.indexOfLast { it.id in selected }
            if (lastIndex != -1) {
                val targetLevel = list[lastIndex].indentationLevel
                val newBlock = TextBlock(id = UUID.randomUUID().toString(), text = "", indentationLevel = targetLevel)
                val mutableList = list.toMutableList().apply { add(lastIndex + 1, newBlock) }
                mutableList
            } else list
        }
        clearSelection()
        scheduleAutosave()
    }

    private var lastWasStructural = false

    protected fun modifyBlocks(forceSave: Boolean = false, action: (List<NoteBlock>) -> List<NoteBlock>) {
        _blocks.update { currentList ->
            val newList = recalculateNumberedLists(action(currentList))

            if (currentList != newList) {
                val now = System.currentTimeMillis()
                val isStructuralChange = currentList.size != newList.size ||
                        currentList.map { it::class } != newList.map { it::class } ||
                        currentList.map { it.id } != newList.map { it.id }

                if (isStructuralChange || now - lastHistorySaveTime > 1500 || lastWasStructural || forceSave) {
                    undoStack.add(currentList)
                    if (undoStack.size > MAX_HISTORY_SIZE) undoStack.removeAt(0)
                    redoStack.clear()
                    lastHistorySaveTime = now
                }

                lastWasStructural = isStructuralChange
                updateHistoryState()
            }
            newList
        }
    }

    fun updateBlockText(blockId: String, newText: String) {
        var forceSave = false
        val currentBlock = _blocks.value.find { it.id == blockId }

        if (currentBlock != null) {
            val oldText = getBlockText(currentBlock)
            val diff = newText.length - oldText.length

            if (diff > 1 || diff < -1) {
                forceSave = true
            } else if (diff == 1) {
                val lastChar = newText.lastOrNull()
                if (lastChar != null && (lastChar.isWhitespace() || lastChar in listOf('.', ',', ';', ':', '!', '?'))) {
                    forceSave = true
                }
            }
        }

        modifyBlocks(forceSave = forceSave) { list ->
            list.map { b ->
                if (b.id != blockId) b else when (b) {
                    is TextBlock -> b.copy(text = newText)
                    is HeadingBlock -> b.copy(text = newText)
                    is CheckboxBlock -> b.copy(text = newText)
                    is BulletedListBlock -> b.copy(text = newText)
                    is NumberedListBlock -> b.copy(text = newText)
                    is ToggleBlock -> b.copy(text = newText)
                    is CodeBlock -> b.copy(code = newText)
                    is QuoteBlock -> b.copy(text = newText)
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
        if (undoStack.isEmpty()) return
        val previousList = undoStack.last()

        val focusId = currentlyFocusedBlockId
        var targetFocusId: String? = null

        if (focusId != null && previousList.any { !it.isDeleted && it.id == focusId }) {
            targetFocusId = focusId
        } else if (focusId != null) {
            val currentList = _blocks.value
            val removedIndex = currentList.indexOfFirst { it.id == focusId }
            if (removedIndex > 0) {
                targetFocusId = currentList.subList(0, removedIndex).lastOrNull { !it.isDeleted }?.id
            }
        }

        if (targetFocusId == null) {
            targetFocusId = previousList.lastOrNull { !it.isDeleted }?.id
        }

        val needsFocusShift = targetFocusId != null && targetFocusId != focusId

        viewModelScope.launch {
            if (needsFocusShift) {
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
                currentlyFocusedBlockId = targetFocusId
                delay(50)
            }

            redoStack.add(_blocks.value)
            undoStack.removeAt(undoStack.lastIndex)
            _blocks.value = previousList
            lastWasStructural = true
            lastHistorySaveTime = System.currentTimeMillis()
            updateHistoryState()
            scheduleAutosave()

            if (targetFocusId != null) {
                delay(50)
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextList = redoStack.last()

        val focusId = currentlyFocusedBlockId
        var targetFocusId: String? = null

        if (focusId != null && nextList.any { !it.isDeleted && it.id == focusId }) {
            targetFocusId = focusId
        } else if (focusId != null) {
            val currentList = _blocks.value
            val removedIndex = currentList.indexOfFirst { it.id == focusId }
            if (removedIndex > 0) {
                targetFocusId = currentList.subList(0, removedIndex).lastOrNull { !it.isDeleted }?.id
            }
        }

        if (targetFocusId == null) {
            targetFocusId = nextList.lastOrNull { !it.isDeleted }?.id
        }

        val needsFocusShift = targetFocusId != null && targetFocusId != focusId

        viewModelScope.launch {
            if (needsFocusShift) {
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
                currentlyFocusedBlockId = targetFocusId
                delay(50)
            }

            undoStack.add(_blocks.value)
            redoStack.removeAt(redoStack.lastIndex)
            _blocks.value = nextList
            lastWasStructural = true
            lastHistorySaveTime = System.currentTimeMillis()
            updateHistoryState()
            scheduleAutosave()

            if (targetFocusId != null) {
                delay(50)
                _focusRequest.value = FocusRequest(id = targetFocusId!!, placeCursorAtEnd = true)
            }
        }
    }

    protected fun recalculateNumberedLists(blocks: List<NoteBlock>): List<NoteBlock> {
        val counters = mutableMapOf<Int, Int>()
        return blocks.map { block ->
            if (block is NumberedListBlock) {
                val currentNum = counters.getOrDefault(block.indentationLevel, 1)
                counters[block.indentationLevel] = currentNum + 1
                block.copy(number = currentNum)
            } else {
                val keysToReset = counters.keys.filter { it >= block.indentationLevel }
                keysToReset.forEach { counters.remove(it) }
                block
            }
        }
    }

    fun updateReminder(blockId: String, timestamp: Long?) {
        var blockText = ""
        modifyBlocks { list ->
            list.map { b ->
                if (b.id == blockId && b is CheckboxBlock) {
                    blockText = b.text
                    b.copy(reminderTimestamp = timestamp)
                } else b
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
        val activeBlockId = _focusRequest.value?.id ?: _selectedBlockIds.value.firstOrNull()
        var newIdToFocus: String? = null

        modifyBlocks { list ->
            val mutableList = list.toMutableList()
            val newId = UUID.randomUUID().toString()
            newIdToFocus = newId

            val activeIndex = if (activeBlockId != null) mutableList.indexOfFirst { it.id == activeBlockId } else mutableList.size - 1
            val indent = if (activeIndex != -1) mutableList[activeIndex].indentationLevel else 0

            val newBlock = when (type) {
                "image" -> ImageBlock(id = newId, indentationLevel = indent)
                "document" -> DocumentBlock(id = newId, indentationLevel = indent)
                "bookmark" -> BookmarkBlock(id = newId, indentationLevel = indent)
                "voice" -> VoiceBlock(id = newId, indentationLevel = indent)
                "database" -> DatabaseBlock(id = newId, columns = listOf(DatabaseColumn(UUID.randomUUID().toString(), "Name", ColumnType.TEXT)), rows = emptyList(), indentationLevel = indent)
                else -> return@modifyBlocks list
            }

            if (activeIndex != -1) {
                val activeBlock = mutableList[activeIndex]
                if (activeBlock is TextBlock && activeBlock.text.isEmpty()) mutableList[activeIndex] = newBlock
                else mutableList.add(activeIndex + 1, newBlock)
            } else mutableList.add(newBlock)

            mutableList
        }
        newIdToFocus?.let { _focusRequest.value = FocusRequest(id = it) }
        scheduleAutosave()
    }

    fun handleUrlSubmit(blockId: String, url: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is BookmarkBlock) it.copy(url = url, title = "Loading...") else it } }
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = HtmlMetadataFetcher.fetchMetadata(url)
            modifyBlocks { list ->
                list.map {
                    if (it.id == blockId && it is BookmarkBlock)
                        it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl)
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
                modifyBlocks { list -> list.map { if (it.id == blockId && it is ImageBlock) it.copy(localFilePath = mediaInfo.localFileName) else it } }
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
                            it.copy(localFilePath = mediaInfo.localFileName, fileName = mediaInfo.originalName, mimeType = mediaInfo.mimeType, fileSizeString = mediaInfo.formattedSize)
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
        modifyBlocks { list -> list.map { if (it.id == blockId && it is VoiceBlock) it.copy(localFilePath = filePath, durationSeconds = duration) else it } }
        scheduleAutosave()
    }

    fun handleRemoveVoice(blockId: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is VoiceBlock) it.copy(localFilePath = null, durationSeconds = 0) else it } }
        scheduleAutosave()
    }

    fun updateDbTitle(blockId: String, newTitle: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(title = newTitle) else it } }
        scheduleAutosave()
    }

    fun addDbRow(blockId: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(rows = it.rows + DatabaseRow(id = UUID.randomUUID().toString(), cells = emptyMap())) else it } }
        scheduleAutosave()
    }

    fun addDbColumn(blockId: String) {
        modifyBlocks { list -> list.map { if (it.id == blockId && it is DatabaseBlock) it.copy(columns = it.columns + DatabaseColumn(id = UUID.randomUUID().toString(), name = "New Column", type = ColumnType.TEXT)) else it } }
        scheduleAutosave()
    }

    fun updateDbCell(blockId: String, rowId: String, colId: String, newValue: String) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedRows = db.rows.map { row ->
                        if (row.id == rowId) {
                            val newMap = row.cells.toMutableMap()
                            newMap[colId] = newValue
                            db.columns.filter { it.type == ColumnType.FORMULA }.forEach { formulaCol ->
                                formulaCol.formulaExpression?.let { expr -> newMap[formulaCol.id] = FormulaEngine.evaluate(expr, newMap, db.columns) }
                            }
                            row.copy(cells = newMap)
                        } else row
                    }
                    db.copy(rows = updatedRows)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun updateDbFormula(blockId: String, colId: String, expression: String) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedCols = db.columns.map { col -> if (col.id == colId) col.copy(formulaExpression = expression) else col }
                    val updatedRows = db.rows.map { row ->
                        val newMap = row.cells.toMutableMap()
                        newMap[colId] = FormulaEngine.evaluate(expression, newMap, updatedCols)
                        row.copy(cells = newMap)
                    }
                    db.copy(columns = updatedCols, rows = updatedRows)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun updateDbColumn(blockId: String, colId: String, newName: String, newType: ColumnType) {
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(columns = db.columns.map { col -> if (col.id == colId) col.copy(name = newName, type = newType) else col }) else db } }
        scheduleAutosave()
    }

    fun updateDbColumnWidth(blockId: String, colId: String, newWidth: Int) {
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(columns = db.columns.map { col -> if (col.id == colId) col.copy(width = newWidth.coerceIn(40, 600)) else col }) else db } }
        scheduleAutosave()
    }

    fun updateDbSort(blockId: String, colId: String, isAscending: Boolean?) {
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(activeSorts = if (isAscending == null) emptyList() else listOf(SortConfig(colId, isAscending))) else db } }
        scheduleAutosave()
    }

    fun addDbFilter(blockId: String, colId: String, operator: String, value: String) {
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(activeFilters = db.activeFilters + FilterConfig(colId, operator, value)) else db } }
        scheduleAutosave()
    }

    fun removeDbFilter(blockId: String, filter: FilterConfig) {
        modifyBlocks { list -> list.map { db -> if (db.id == blockId && db is DatabaseBlock) db.copy(activeFilters = db.activeFilters - filter) else db } }
        scheduleAutosave()
    }

    fun reorderDbColumns(blockId: String, fromIndex: Int, toIndex: Int) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val cols = db.columns.toMutableList()
                    val moved = cols.removeAt(fromIndex)
                    cols.add(toIndex, moved)
                    db.copy(columns = cols)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun deleteDbColumn(blockId: String, colId: String) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedCols = db.columns.map { col ->
                        if (col.id == colId) col.copy(isDeleted = true) else col
                    }
                    db.copy(columns = updatedCols)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun deleteDbRow(blockId: String, rowId: String) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val updatedRows = db.rows.map { row ->
                        if (row.id == rowId) row.copy(isDeleted = true) else row
                    }
                    db.copy(rows = updatedRows)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun addDbRowAt(blockId: String, index: Int) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val rows = db.rows.toMutableList()
                    rows.add(index.coerceIn(0, rows.size), DatabaseRow(id = UUID.randomUUID().toString(), cells = emptyMap()))
                    db.copy(rows = rows)
                } else db
            }
        }
        scheduleAutosave()
    }

    fun addDbColumnAt(blockId: String, index: Int) {
        modifyBlocks { list ->
            list.map { db ->
                if (db.id == blockId && db is DatabaseBlock) {
                    val cols = db.columns.toMutableList()
                    cols.add(index.coerceIn(0, cols.size), DatabaseColumn(id = UUID.randomUUID().toString(), name = "New Column", type = ColumnType.TEXT))
                    db.copy(columns = cols)
                } else db
            }
        }
        scheduleAutosave()
    }

    override fun onCleared() {
        super.onCleared()
        autosaveJob?.cancel()

        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            performSave()
        }
    }

    // Databse tags
    val globalTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createGlobalTag(name: String, colorHex: String): String {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateTag(newId, name, colorHex)
        }
        return newId
    }
}