package com.ben.inly.presentation.mobile.home.overview.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TaskSource
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.markDeleted
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.sync.AutoSyncTrigger
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.FocusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.UUID

data class BlockLocation(val noteId: String, val isDaily: Boolean)

class RemindersViewModel constructor(
    private val repository: NoteRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeBlocks = MutableStateFlow<List<NoteBlock>>(emptyList())
    private val _completedBlocks = MutableStateFlow<List<NoteBlock>>(emptyList())

    private val _isShowingCompleted = MutableStateFlow(false)
    val isShowingCompleted: StateFlow<Boolean> = _isShowingCompleted.asStateFlow()

    val visibleBlocks = combine(_activeBlocks, _completedBlocks, _isShowingCompleted) { active, completed, isShowing ->
        if (isShowing) completed else active
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    private val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    private val blockSourceMap = mutableMapOf<String, BlockLocation>()
    private val sessionBlockCache = mutableMapOf<String, BlockLocation>()

    private val localEditTimestamps = mutableMapOf<String, Long>()
    private val localToggleTimestamps = mutableMapOf<String, Long>()

    private var typingJob: Job? = null
    private val dirtyBlocks = mutableSetOf<String>()

    val allLinkableNotes = repository.getAllLinkableNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createLinkedNote(title: String): String {
        val newNoteId = java.util.UUID.randomUUID().toString()

        val metadata = NoteMetadataEntity(
            noteId = newNoteId,
            title = title,
            icon = null,
            folderId = null,
            isDaily = false,
            dateString = null,
            isSubNote = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            filePath = "note_$newNoteId.json",
            snippet = ""
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(metadata, NoteContent(blocks = emptyList()))
        }

        return newNoteId
    }

    suspend fun getNoteTitle(noteId: String): String {
        return repository.getNoteById(noteId)?.title ?: "Unknown Note"
    }

    init {
        loadAllTasks()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        if (dirtyBlocks.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO + NonCancellable) {
                flushDirtyBlocks()
            }
        }
    }

    fun loadAllTasks() {
        viewModelScope.launch {
            _isLoading.value = true

            repository.getAllTasksFlow().collectLatest { allTasks ->
                val dbTasksMap = allTasks.associateBy { it.blockId }
                val currentTime = System.currentTimeMillis()

                allTasks.forEach { task ->
                    blockSourceMap[task.blockId] = BlockLocation(task.noteId, task.sourceType == TaskSource.DAILY)
                    sessionBlockCache.remove(task.blockId)
                }

                blockSourceMap.putAll(sessionBlockCache)

                _activeBlocks.update { currentList ->
                    val updatedList = currentList.mapNotNull { block ->
                        val lastToggle = localToggleTimestamps[block.id] ?: 0L
                        val isRecentToggle = (currentTime - lastToggle) < 2000L

                        if (isRecentToggle) {
                            block
                        } else {
                            val dbTask = dbTasksMap[block.id]
                            if (dbTask != null) {
                                if (dbTask.isChecked) null
                                else {
                                    val lastEdit = localEditTimestamps[block.id] ?: 0L
                                    val isRecentEdit = (currentTime - lastEdit) < 3000L || dirtyBlocks.contains(block.id)
                                    val textToUse = if (isRecentEdit) (block as CheckboxBlock).text else dbTask.text
                                    (block as CheckboxBlock).copy(text = textToUse, isChecked = false)
                                }
                            } else {
                                if (sessionBlockCache.containsKey(block.id)) block else null
                            }
                        }
                    }.toMutableList()

                    allTasks.forEach { task ->
                        val lastToggle = localToggleTimestamps[task.blockId] ?: 0L
                        if ((currentTime - lastToggle) >= 2000L && !task.isChecked && updatedList.none { it.id == task.blockId }) {
                            updatedList.add(CheckboxBlock(task.blockId, task.text, false, 0))
                        }
                    }
                    updatedList
                }

                _completedBlocks.update { currentList ->
                    val updatedList = currentList.mapNotNull { block ->
                        val lastToggle = localToggleTimestamps[block.id] ?: 0L
                        val isRecentToggle = (currentTime - lastToggle) < 2000L

                        if (isRecentToggle) {
                            block
                        } else {
                            val dbTask = dbTasksMap[block.id]
                            if (dbTask != null) {
                                if (!dbTask.isChecked) null
                                else {
                                    val lastEdit = localEditTimestamps[block.id] ?: 0L
                                    val isRecentEdit = (currentTime - lastEdit) < 3000L || dirtyBlocks.contains(block.id)
                                    val textToUse = if (isRecentEdit) (block as CheckboxBlock).text else dbTask.text
                                    (block as CheckboxBlock).copy(text = textToUse, isChecked = true)
                                }
                            } else {
                                if (sessionBlockCache.containsKey(block.id)) block else null
                            }
                        }
                    }.toMutableList()

                    allTasks.forEach { task ->
                        val lastToggle = localToggleTimestamps[task.blockId] ?: 0L
                        if ((currentTime - lastToggle) >= 2000L && task.isChecked && updatedList.none { it.id == task.blockId }) {
                            updatedList.add(CheckboxBlock(task.blockId, task.text, true, 0))
                        }
                    }
                    updatedList
                }

                _isLoading.value = false
            }
        }
    }

    fun toggleCompletedView() {
        _isShowingCompleted.value = !_isShowingCompleted.value
        clearSelection()
    }

    private suspend fun getOrCreateInbox(): Pair<NoteMetadataEntity, NoteContent> {
        val allNotes = repository.getAllNotes().first()
        var inboxNote = allNotes.find { it.title.equals("Inbox", ignoreCase = true) && !it.isDaily }
        val noteId: String
        val content: NoteContent

        if (inboxNote == null) {
            noteId = UUID.randomUUID().toString()
            inboxNote = NoteMetadataEntity(
                noteId = noteId, title = "Inbox", icon = "📥", folderId = null,
                isDaily = false, dateString = null, createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), filePath = "note_$noteId.json", snippet = "Saved tasks and links."
            )
            content = NoteContent(blocks = emptyList())
        } else {
            noteId = inboxNote.noteId
            content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
        }
        return Pair(inboxNote, content)
    }

    fun insertNewReminder() {
        _isShowingCompleted.value = false
        viewModelScope.launch(Dispatchers.IO) {
            val (inboxMeta, content) = getOrCreateInbox()
            val newId = UUID.randomUUID().toString()
            val newBlock = CheckboxBlock(id = newId, text = "", isChecked = false, indentationLevel = 0, completedAt = null)

            val location = BlockLocation(noteId = inboxMeta.noteId, isDaily = false)
            blockSourceMap[newId] = location
            sessionBlockCache[newId] = location

            _activeBlocks.update { currentList ->
                listOf(newBlock) + currentList
            }

            val updatedBlocks = listOf(newBlock) + content.blocks
            repository.saveNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            _focusRequest.value = FocusRequest(id = newId)
        }
    }

    fun toggleCheckbox(blockId: String, isChecked: Boolean) {
        val loc = blockSourceMap[blockId] ?: return
        val timestamp = if (isChecked) System.currentTimeMillis() else null

        localToggleTimestamps[blockId] = System.currentTimeMillis()

        if (isChecked) reminderScheduler.cancel(blockId)

        if (isChecked) {
            var movedBlock: NoteBlock? = null
            _activeBlocks.update { list ->
                val target = list.find { it.id == blockId } as? CheckboxBlock
                if (target != null) movedBlock = target.copy(isChecked = true, completedAt = timestamp)
                list.filterNot { it.id == blockId }
            }
            if (movedBlock != null) _completedBlocks.update { list -> listOf(movedBlock!!) + list }
        } else {
            var movedBlock: NoteBlock? = null
            _completedBlocks.update { list ->
                val target = list.find { it.id == blockId } as? CheckboxBlock
                if (target != null) movedBlock = target.copy(isChecked = false, completedAt = timestamp)
                list.filterNot { it.id == blockId }
            }
            if (movedBlock != null) _activeBlocks.update { list -> listOf(movedBlock!!) + list }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (loc.isDaily) {
                val content = repository.getDailyNote(loc.noteId) ?: return@launch
                val updatedBlocks = updateBlockInList(content.blocks, blockId) {
                    it.copy(isChecked = isChecked, completedAt = timestamp)
                }
                repository.saveDailyNote(loc.noteId, NoteContent(blocks = updatedBlocks))
            } else {
                val meta = repository.getNoteById(loc.noteId) ?: return@launch
                val content = repository.getNoteContent(loc.noteId) ?: return@launch
                val updatedBlocks = updateBlockInList(content.blocks, blockId) {
                    it.copy(isChecked = isChecked, completedAt = timestamp)
                }
                repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
            }
        }
        AutoSyncTrigger.requestSync()
    }

    fun updateReminder(blockId: String, timestamp: Long?) {
        val loc = blockSourceMap[blockId] ?: return
        var blockText = ""

        _activeBlocks.update { list ->
            list.map {
                if (it.id == blockId && it is CheckboxBlock) {
                    blockText = it.text
                    it.copy(reminderTimestamp = timestamp)
                } else it
            }
        }
        _completedBlocks.update { list ->
            list.map {
                if (it.id == blockId && it is CheckboxBlock) {
                    blockText = it.text
                    it.copy(reminderTimestamp = timestamp)
                } else it
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var notificationTitle = "Task Reminder"

            if (loc.isDaily) {
                val content = repository.getDailyNote(loc.noteId) ?: return@launch
                val updatedBlocks = updateBlockInList(content.blocks, blockId) {
                    it.copy(reminderTimestamp = timestamp)
                }
                repository.saveDailyNote(loc.noteId, NoteContent(blocks = updatedBlocks))
                notificationTitle = repository.getDailyNoteMetadata(loc.noteId)?.title?.ifBlank { "Daily Note" } ?: "Daily Note"
            } else {
                val meta = repository.getNoteById(loc.noteId) ?: return@launch
                val content = repository.getNoteContent(loc.noteId) ?: return@launch
                val updatedBlocks = updateBlockInList(content.blocks, blockId) {
                    it.copy(reminderTimestamp = timestamp)
                }
                repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                notificationTitle = meta.title.ifBlank { "Task Reminder" }
            }

            if (timestamp != null) {
                reminderScheduler.schedule(blockId, notificationTitle, blockText.ifBlank { "Unfinished task" }, timestamp)
            } else {
                reminderScheduler.cancel(blockId)
            }
        }
    }

    fun updateBlockText(blockId: String, newText: String) {
        val loc = blockSourceMap[blockId]
        if (loc == null) { /* Possibly reload tasks if block is unknown */ }

        localEditTimestamps[blockId] = System.currentTimeMillis()
        dirtyBlocks.add(blockId)

        _activeBlocks.update { list -> list.map { if (it.id == blockId && it is CheckboxBlock) it.copy(text = newText) else it } }
        _completedBlocks.update { list -> list.map { if (it.id == blockId && it is CheckboxBlock) it.copy(text = newText) else it } }

        typingJob?.cancel()
        typingJob = viewModelScope.launch(Dispatchers.IO) {
            delay(800)
            flushDirtyBlocks()
        }
    }

    private suspend fun flushDirtyBlocks() {
        val blocksToSave = dirtyBlocks.toList()
        if (blocksToSave.isEmpty()) return

        dirtyBlocks.removeAll(blocksToSave.toSet())

        val currentBlocks = if (_isShowingCompleted.value) _completedBlocks.value else _activeBlocks.value
        val byNote = blocksToSave.groupBy { blockSourceMap[it] }

        byNote.forEach { (loc, bIds) ->
            if (loc != null) {
                val content = if (loc.isDaily) repository.getDailyNote(loc.noteId)
                else repository.getNoteContent(loc.noteId)

                if (content == null) return@forEach

                var updatedBlocks = content.blocks
                bIds.forEach { bId ->
                    updatedBlocks = updateBlockInList(updatedBlocks, bId) { target ->
                        val latestText = (currentBlocks.find { it.id == bId } as? CheckboxBlock)?.text ?: target.text
                        target.copy(text = latestText)
                    }
                }

                if (loc.isDaily) {
                    repository.saveDailyNote(loc.noteId, NoteContent(blocks = updatedBlocks))
                } else {
                    val meta = repository.getNoteById(loc.noteId) ?: return@forEach
                    repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                }
            }
        }
    }

    private fun updateBlockInList(
        blocks: List<NoteBlock>,
        targetId: String,
        updater: (CheckboxBlock) -> CheckboxBlock
    ): List<NoteBlock> {
        val now = System.currentTimeMillis()

        return blocks.map { block ->
            if (block.id == targetId && block is CheckboxBlock) {
                updater(block).copy(updatedAt = now)
            } else if (block is com.ben.inly.domain.model.RowContainerBlock) {
                val updatedColumns = block.columns.map { column ->
                    column.copy(blocks = updateBlockInList(column.blocks, targetId, updater))
                }

                if (updatedColumns != block.columns) {
                    block.copy(columns = updatedColumns, updatedAt = now)
                } else {
                    block
                }
            } else {
                block
            }
        }
    }

    fun handleEnter(id: String, textBefore: String, textAfter: String) {
        val originalLoc = blockSourceMap[id] ?: return

        val newId = UUID.randomUUID().toString()
        val newBlock = CheckboxBlock(newId, textAfter, false, 0)

        _activeBlocks.update { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@update list
            val newList = list.toMutableList()
            newList[idx] = (list[idx] as CheckboxBlock).copy(text = textBefore)
            newList.add(idx + 1, newBlock)
            newList
        }

        viewModelScope.launch(Dispatchers.IO) {
            val (inboxMeta, inboxContent) = getOrCreateInbox()
            val isParentInbox = (originalLoc.noteId == inboxMeta.noteId)

            val inboxLoc = BlockLocation(noteId = inboxMeta.noteId, isDaily = false)
            blockSourceMap[newId] = inboxLoc
            sessionBlockCache[newId] = inboxLoc
            localEditTimestamps[newId] = System.currentTimeMillis()

            if (isParentInbox) {
                val dbIdx = inboxContent.blocks.indexOfFirst { it.id == id }
                val updatedInboxBlocks = if (dbIdx != -1) {
                    val mutableInbox = inboxContent.blocks.toMutableList()
                    mutableInbox[dbIdx] = (mutableInbox[dbIdx] as CheckboxBlock).copy(text = textBefore)
                    mutableInbox.add(dbIdx + 1, newBlock)
                    mutableInbox
                } else {
                    listOf(newBlock) + inboxContent.blocks
                }
                repository.saveNote(
                    inboxMeta.copy(updatedAt = System.currentTimeMillis()),
                    NoteContent(blocks = updatedInboxBlocks)
                )
            } else {
                if (originalLoc.isDaily) {
                    val content = repository.getDailyNote(originalLoc.noteId)
                    if (content != null) {
                        val updatedBlocks = updateBlockInList(content.blocks, id) { it.copy(text = textBefore) }
                        repository.saveDailyNote(originalLoc.noteId, NoteContent(blocks = updatedBlocks))
                    }
                } else {
                    val meta = repository.getNoteById(originalLoc.noteId)
                    val content = repository.getNoteContent(originalLoc.noteId)
                    if (meta != null && content != null) {
                        val updatedBlocks = updateBlockInList(content.blocks, id) { it.copy(text = textBefore) }
                        repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                    }
                }

                val updatedInboxBlocks = listOf(newBlock) + inboxContent.blocks
                repository.saveNote(
                    inboxMeta.copy(updatedAt = System.currentTimeMillis()),
                    NoteContent(blocks = updatedInboxBlocks)
                )
            }

            _focusRequest.value = FocusRequest(id = newId)
        }
    }

    fun handleBackspaceOnEmpty(id: String) {
        var focusPrevId: String? = null

        _activeBlocks.update { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@update list

            if (list.size > 1) {
                focusPrevId = if (idx > 0) list[idx - 1].id else list[idx + 1].id
            }

            val newList = list.toMutableList()
            newList.removeAt(idx)
            newList
        }

        if (focusPrevId != null) {
            _focusRequest.value = null
            _focusRequest.value = FocusRequest(id = focusPrevId!!, placeCursorAtEnd = true)
        }

        val loc = blockSourceMap[id] ?: return
        sessionBlockCache.remove(id)

        viewModelScope.launch(Dispatchers.IO) {
            if (loc.isDaily) {
                val content = repository.getDailyNote(loc.noteId) ?: return@launch
                val updatedBlocks = content.blocks.map { if (it.id == id) it.markDeleted() else it }
                repository.saveDailyNote(loc.noteId, NoteContent(blocks = updatedBlocks))
            } else {
                val meta = repository.getNoteById(loc.noteId) ?: return@launch
                val content = repository.getNoteContent(loc.noteId) ?: return@launch
                val updatedBlocks = content.blocks.map { if (it.id == id) it.markDeleted() else it }
                repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
            }
        }
    }

    fun deleteSelectedBlocks() {
        val toDelete = _selectedBlockIds.value
        if (toDelete.isEmpty()) return

        val blocksByNote = toDelete.groupBy { blockSourceMap[it] }
        viewModelScope.launch(Dispatchers.IO) {
            blocksByNote.forEach { (loc, blockIdsToDelete) ->
                blockIdsToDelete.forEach { sessionBlockCache.remove(it) }
                if (loc != null) {
                    if (loc.isDaily) {
                        val content = repository.getDailyNote(loc.noteId) ?: return@forEach
                        val updatedBlocks = content.blocks.map { block ->
                            if (block.id in blockIdsToDelete) block.markDeleted() else block
                        }
                        repository.saveDailyNote(loc.noteId, NoteContent(blocks = updatedBlocks))
                    } else {
                        val meta = repository.getNoteById(loc.noteId) ?: return@forEach
                        val content = repository.getNoteContent(loc.noteId) ?: return@forEach
                        val updatedBlocks = content.blocks.map { block ->
                            if (block.id in blockIdsToDelete) block.markDeleted() else block
                        }
                        repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                    }
                }
            }
            clearSelection()
        }
    }

    fun toggleSelection(id: String) { _selectedBlockIds.update { if (it.contains(id)) it - id else it + id } }
    fun clearSelection() { _selectedBlockIds.value = emptySet() }
    fun clearFocusRequest() { _focusRequest.value = null }

    fun getSelectedText(): String {
        val currentBlocks = if (_isShowingCompleted.value) _completedBlocks.value else _activeBlocks.value
        return currentBlocks.filter { it.id in _selectedBlockIds.value }.joinToString("\n") { (it as? CheckboxBlock)?.text ?: "" }
    }

    fun cutSelectedBlocks(): String {
        val text = getSelectedText()
        deleteSelectedBlocks()
        return text
    }

    fun setFocusedBlock(id: String) {}

    fun selectAllBlocks() {
        val currentBlocks = if (_isShowingCompleted.value) _completedBlocks.value else _activeBlocks.value
        _selectedBlockIds.value = currentBlocks.map { it.id }.toSet()
    }
}