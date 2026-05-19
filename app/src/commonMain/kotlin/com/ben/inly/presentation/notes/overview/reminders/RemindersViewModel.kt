package com.ben.inly.presentation.notes.overview.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.FocusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Aggregates all checkbox tasks from across the application.
 * When a task is modified here, it traces the block back to its original note and updates it there.
 */
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

    private val blockSourceMap = mutableMapOf<String, String>()

    private var initialLoad = true
    private var typingJob: Job? = null
    private val dirtyBlocks = mutableSetOf<String>()

    /**
     * Scans every standalone note for checkboxes, separating them into active and completed lists.
     * Sorts pending tasks by creation date, and completed tasks by the time they were finished.
     */
    fun loadAllTasks() {
        viewModelScope.launch {
            repository.getAllStandaloneNotes().collectLatest { allNotes ->
                if (initialLoad) _isLoading.value = true

                val activeList = mutableListOf<Triple<NoteBlock, Long, Int>>()
                val completedList = mutableListOf<Triple<NoteBlock, Long, Int>>()
                blockSourceMap.clear()

                for (note in allNotes) {
                    val content = repository.getNoteContent(note.noteId)
                    content?.blocks?.forEachIndexed { index, block ->
                        if (block is CheckboxBlock) {
                            val mappedBlock = block.copy(indentationLevel = 0)
                            blockSourceMap[block.id] = note.noteId

                            if (block.isChecked) {
                                val timeToRank = block.completedAt ?: note.updatedAt
                                completedList.add(Triple(mappedBlock, timeToRank, index))
                            } else {
                                activeList.add(Triple(mappedBlock, note.createdAt, index))
                            }
                        }
                    }
                }

                _activeBlocks.value = activeList
                    .sortedWith(compareByDescending<Triple<NoteBlock, Long, Int>> { it.second }.thenBy { it.third })
                    .map { it.first }

                _completedBlocks.value = completedList
                    .sortedWith(compareByDescending<Triple<NoteBlock, Long, Int>> { it.second }.thenByDescending { it.third })
                    .map { it.first }

                if (initialLoad) {
                    _isLoading.value = false
                    initialLoad = false
                }
            }
        }
    }

    fun toggleCompletedView() {
        _isShowingCompleted.value = !_isShowingCompleted.value
        clearSelection()
    }

    /**
     * When a task is added directly from this screen, it needs a parent note to live in.
     * This grabs the default 'Inbox' note, creating it if it doesn't exist yet.
     */
    private suspend fun getOrCreateInbox(): Pair<NoteMetadataEntity, NoteContent> {
        val allNotes = repository.getAllStandaloneNotes().first()
        var inboxNote = allNotes.find { it.title.equals("Inbox", ignoreCase = true) }
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
        viewModelScope.launch(Dispatchers.IO) {
            val (inboxMeta, content) = getOrCreateInbox()
            val newId = UUID.randomUUID().toString()
            val newBlock = CheckboxBlock(id = newId, text = "", isChecked = false, indentationLevel = 0, completedAt = null)

            val updatedBlocks = listOf(newBlock) + content.blocks
            repository.saveStandaloneNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            _focusRequest.value = FocusRequest(id = newId)
        }
    }

    /**
     * Marks a task as complete locally, cancels any pending notifications,
     * and saves the new state back to the original source note.
     */
    fun toggleCheckbox(blockId: String, isChecked: Boolean) {
        val originalNoteId = blockSourceMap[blockId] ?: return
        val timestamp = if (isChecked) System.currentTimeMillis() else null

        if (isChecked) reminderScheduler.cancel(blockId)

        if (isChecked) {
            var movedBlock: NoteBlock? = null
            _activeBlocks.update { list ->
                val target = list.find { it.id == blockId } as? CheckboxBlock
                if (target != null) movedBlock = target.copy(isChecked = true, completedAt = timestamp)
                list.filterNot { it.id == blockId }
            }
            if (movedBlock != null) {
                _completedBlocks.update { list -> listOf(movedBlock!!) + list.filterNot { it.id == blockId } }
            }
        } else {
            var movedBlock: NoteBlock? = null
            _completedBlocks.update { list ->
                val target = list.find { it.id == blockId } as? CheckboxBlock
                if (target != null) movedBlock = target.copy(isChecked = false, completedAt = timestamp)
                list.filterNot { it.id == blockId }
            }
            if (movedBlock != null) {
                _activeBlocks.update { list -> listOf(movedBlock!!) + list.filterNot { it.id == blockId } }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
            val content = repository.getNoteContent(originalNoteId) ?: return@launch

            val updatedBlocks = content.blocks.map {
                if (it.id == blockId && it is CheckboxBlock) {
                    it.copy(isChecked = isChecked, completedAt = timestamp)
                } else it
            }
            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
        }
    }

    fun updateReminder(blockId: String, timestamp: Long?) {
        val originalNoteId = blockSourceMap[blockId] ?: return
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
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
            val content = repository.getNoteContent(originalNoteId) ?: return@launch

            val updatedBlocks = content.blocks.map {
                if (it.id == blockId && it is CheckboxBlock) {
                    it.copy(reminderTimestamp = timestamp)
                } else it
            }
            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            if (timestamp != null) {
                reminderScheduler.schedule(
                    blockId = blockId,
                    noteTitle = meta.title.ifBlank { "Task Reminder" },
                    text = blockText.ifBlank { "Unfinished task" },
                    timestamp = timestamp
                )
            } else {
                reminderScheduler.cancel(blockId)
            }
        }
    }

    /**
     * Updates the text of a task as the user types.
     * Uses a short delay to debounce rapid keystrokes, ensuring it doesn't slam
     * the database with save requests for every single letter typed.
     */
    fun updateBlockText(blockId: String, newText: String) {
        _activeBlocks.update { list -> list.map { if (it.id == blockId && it is CheckboxBlock) it.copy(text = newText) else it } }
        _completedBlocks.update { list -> list.map { if (it.id == blockId && it is CheckboxBlock) it.copy(text = newText) else it } }

        dirtyBlocks.add(blockId)

        typingJob?.cancel()
        typingJob = viewModelScope.launch(Dispatchers.IO) {
            delay(800)

            val currentBlocks = if (_isShowingCompleted.value) _completedBlocks.value else _activeBlocks.value
            val blocksToSave = dirtyBlocks.toList()
            dirtyBlocks.clear()

            val byNote = blocksToSave.groupBy { blockSourceMap[it] }

            byNote.forEach { (noteId, bIds) ->
                if (noteId != null) {
                    val meta = repository.getAllStandaloneNotes().first().find { it.noteId == noteId }
                    if (meta != null) {
                        val content = repository.getNoteContent(noteId)
                        if (content != null) {
                            val updatedBlocks = content.blocks.map { dbBlock ->
                                if (dbBlock.id in bIds && dbBlock is CheckboxBlock) {
                                    val latestText = (currentBlocks.find { it.id == dbBlock.id } as? CheckboxBlock)?.text ?: dbBlock.text
                                    dbBlock.copy(text = latestText)
                                } else dbBlock
                            }
                            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                        }
                    }
                }
            }
        }
    }

    fun handleEnter(id: String, textBefore: String, textAfter: String) {
        val originalNoteId = blockSourceMap[id] ?: return

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
        blockSourceMap[newId] = originalNoteId

        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
            val content = repository.getNoteContent(originalNoteId) ?: return@launch

            val dbIdx = content.blocks.indexOfFirst { it.id == id }
            if (dbIdx != -1) {
                val newList = content.blocks.toMutableList()
                val originalIndent = newList[dbIdx].indentationLevel
                newList[dbIdx] = (newList[dbIdx] as CheckboxBlock).copy(text = textBefore)
                newList.add(dbIdx + 1, newBlock.copy(indentationLevel = originalIndent))

                repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = newList))
                _focusRequest.value = FocusRequest(id = newId)
            }
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

        val originalNoteId = blockSourceMap[id] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
            val content = repository.getNoteContent(originalNoteId) ?: return@launch

            val updatedBlocks = content.blocks.filterNot { it.id == id }
            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
        }
    }

    fun toggleSelection(id: String) { _selectedBlockIds.update { if (it.contains(id)) it - id else it + id } }
    fun clearSelection() { _selectedBlockIds.value = emptySet() }
    fun clearFocusRequest() { _focusRequest.value = null }

    fun getSelectedText(): String {
        val currentBlocks = if (_isShowingCompleted.value) _completedBlocks.value else _activeBlocks.value
        return currentBlocks.filter { it.id in _selectedBlockIds.value }.joinToString("\n") { (it as? CheckboxBlock)?.text ?: "" }
    }

    fun deleteSelectedBlocks() {
        val toDelete = _selectedBlockIds.value
        if (toDelete.isEmpty()) return

        val blocksByNote = toDelete.groupBy { blockSourceMap[it] }
        viewModelScope.launch(Dispatchers.IO) {
            blocksByNote.forEach { (noteId, blockIdsToDelete) ->
                if (noteId != null) {
                    val meta = repository.getAllStandaloneNotes().first().find { it.noteId == noteId }
                    if (meta != null) {
                        val content = repository.getNoteContent(noteId)
                        if (content != null) {
                            val updatedBlocks = content.blocks.filterNot { it.id in blockIdsToDelete }
                            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                        }
                    }
                }
            }
            clearSelection()
        }
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