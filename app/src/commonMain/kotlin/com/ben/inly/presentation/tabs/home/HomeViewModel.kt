package com.ben.inly.presentation.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.VoiceTaskEventBus
import com.ben.inly.domain.util.VoiceRecognizer
import com.ben.inly.domain.util.TaskExtractor
import com.ben.inly.presentation.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.util.UUID

enum class SortType { LAST_EDITED, DATE_CREATED, NAME }
enum class SortOrder { ASCENDING, DESCENDING }

class HomeViewModel constructor(
    private val repository: NoteRepository,
    private val settingsManager: SettingsManager,
    private val reminderScheduler: ReminderScheduler,
    private val taskExtractor: TaskExtractor,
    private val voiceRecognizer: VoiceRecognizer
) : ViewModel() {

    val sortType: StateFlow<SortType> = settingsManager.sortTypeFlow
        .map { SortType.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, SortType.LAST_EDITED)

    val sortOrder: StateFlow<SortOrder> = settingsManager.sortOrderFlow
        .map { SortOrder.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, SortOrder.DESCENDING)

    fun updateSort(type: SortType, order: SortOrder) {
        settingsManager.saveSortSettings(type.name, order.name)
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    private val _selectedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderIds: StateFlow<Set<String>> = _selectedFolderIds.asStateFlow()

    private val _remindersCount = MutableStateFlow(0)
    val remindersCount: StateFlow<Int> = _remindersCount.asStateFlow()

    private val _bookmarksCount = MutableStateFlow(0)
    val bookmarksCount: StateFlow<Int> = _bookmarksCount.asStateFlow()

    private val _imagesCount = MutableStateFlow(0)
    val imagesCount: StateFlow<Int> = _imagesCount.asStateFlow()

    private val _documentsCount = MutableStateFlow(0)
    val documentsCount: StateFlow<Int> = _documentsCount.asStateFlow()

    private val _allFolders = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<FolderEntity>())

    val recentNotes = repository.getAllLinkableNotes()
        .map { notes ->
            notes.filter { !it.title.equals("Inbox", ignoreCase = true) }
                .sortedByDescending { it.updatedAt }.take(4)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<NoteMetadataEntity>())

    val favoriteNotes = repository.getFavoriteNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<NoteMetadataEntity>())

    val currentSubFolders = combine(_allFolders, _selectedFolderId) { all, currentParent ->
        all.filter { !it.isDeleted && it.parentFolderId == currentParent }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<FolderEntity>())

    val breadcrumbs = combine(_allFolders, _selectedFolderId) { all, currentId ->
        val path = mutableListOf<FolderEntity>()
        var curr = currentId
        while (curr != null) {
            val folder = all.find { it.folderId == curr }
            if (folder != null) {
                path.add(0, folder)
                curr = folder.parentFolderId
            } else {
                break
            }
        }
        path
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<FolderEntity>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteMetadataEntity>> = combine(
        _selectedFolderId.flatMapLatest { folderId ->
            if (folderId == null) repository.getAllNotes()
            else repository.getNotesInFolder(folderId)
        },
        _searchQuery,
        sortType,
        sortOrder
    ) { noteList, query, activeSortType, activeSortOrder ->
        val visibleNotes = noteList.filter { !it.title.equals("Inbox", ignoreCase = true) }
        val folderFiltered = if (query.isNotBlank()) visibleNotes else visibleNotes.filter { it.folderId == _selectedFolderId.value }

        val finalFilteredList = if (query.isBlank()) {
            folderFiltered
        } else {
            val q = query.lowercase()
            val filteredList = mutableListOf<NoteMetadataEntity>()

            for (note in folderFiltered) {
                if (note.title.lowercase().contains(q) || note.snippet.lowercase().contains(q)) {
                    filteredList.add(note)
                    continue
                }

                val content = repository.getNoteContent(note.noteId)
                if (content != null) {
                    val matches = content.blocks.any { block ->
                        when (block) {
                            is TextBlock -> block.text.lowercase().contains(q)
                            is HeadingBlock -> block.text.lowercase().contains(q)
                            is CheckboxBlock -> block.text.lowercase().contains(q)
                            is BulletedListBlock -> block.text.lowercase().contains(q)
                            is NumberedListBlock -> block.text.lowercase().contains(q)
                            is ToggleBlock -> block.text.lowercase().contains(q)
                            is CodeBlock -> block.code.lowercase().contains(q)
                            is BookmarkBlock -> {
                                block.url.lowercase().contains(q) ||
                                        block.title?.lowercase()?.contains(q) == true ||
                                        block.description?.lowercase()?.contains(q) == true
                            }
                            is DocumentBlock -> block.fileName.lowercase().contains(q)
                            is ImageBlock -> block.localFilePath?.lowercase()?.contains(q) == true
                            else -> false
                        }
                    }
                    if (matches) {
                        filteredList.add(note)
                    }
                }
            }
            filteredList
        }

        when (activeSortType) {
            SortType.LAST_EDITED -> if (activeSortOrder == SortOrder.DESCENDING) finalFilteredList.sortedByDescending { it.updatedAt } else finalFilteredList.sortedBy { it.updatedAt }
            SortType.DATE_CREATED -> if (activeSortOrder == SortOrder.DESCENDING) finalFilteredList.sortedByDescending { it.createdAt } else finalFilteredList.sortedBy { it.createdAt }
            SortType.NAME -> if (activeSortOrder == SortOrder.DESCENDING) finalFilteredList.sortedByDescending { it.title.lowercase() } else finalFilteredList.sortedBy { it.title.lowercase() }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<NoteMetadataEntity>())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupOldTrashedNotes()
            _isLoading.value = false
        }
        viewModelScope.launch {
            repository.getIncompleteTasksCount().collect { count ->
                _remindersCount.value = count
            }
        }
        viewModelScope.launch {
            repository.getImagesCount().collect { _imagesCount.value = it }
        }
        viewModelScope.launch {
            repository.getDocumentsCount().collect { _documentsCount.value = it }
        }
        viewModelScope.launch {
            repository.getBookmarksCount().collect { _bookmarksCount.value = it }
        }
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        clearSelection()
    }

    fun navigateUp() {
        val currentId = _selectedFolderId.value ?: return
        val currentFolder = _allFolders.value.find { it.folderId == currentId }
        _selectedFolderId.value = currentFolder?.parentFolderId
        clearSelection()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createNewFolder(name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.insertFolder(
                FolderEntity(
                    folderId = UUID.randomUUID().toString(),
                    name = name,
                    parentFolderId = _selectedFolderId.value,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleNoteSelection(noteId: String) {
        _selectedNoteIds.update { if (it.contains(noteId)) it - noteId else it + noteId }
    }

    fun toggleFolderSelection(folderId: String) {
        _selectedFolderIds.update { if (it.contains(folderId)) it - folderId else it + folderId }
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
    }

    private suspend fun deleteFolderRecursively(folderId: String) {
        val notesInFolder = repository.getNotesInFolder(folderId).first()
        notesInFolder.forEach { note ->
            repository.deleteNote(note.noteId, note.filePath)
        }

        val subFolders = _allFolders.value.filter { it.parentFolderId == folderId }
        subFolders.forEach { subFolder ->
            deleteFolderRecursively(subFolder.folderId)
        }

        repository.deleteFolder(folderId)
    }

    fun deleteSelectedItems() {
        val toDeleteNotes = _selectedNoteIds.value
        val toDeleteFolders = _selectedFolderIds.value
        val now = System.currentTimeMillis()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            toDeleteNotes.forEach { noteId ->
                val meta = repository.getNoteById(noteId)
                if (meta != null) {
                    val content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
                    repository.saveNote(meta.copy(trashedAt = now), content)
                }
            }

            toDeleteFolders.forEach { folderId ->
                trashFolderContentsRecursively(folderId, now)
            }

            clearSelection()
        }
    }

    private suspend fun trashFolderContentsRecursively(folderId: String, trashTime: Long) {
        val notesInFolder = repository.getNotesInFolder(folderId).first()
        notesInFolder.forEach { note ->
            val content = repository.getNoteContent(note.noteId) ?: NoteContent(blocks = emptyList())
            repository.saveNote(note.copy(trashedAt = trashTime), content)
        }

        val subFolders = _allFolders.value.filter { it.parentFolderId == folderId }
        subFolders.forEach { subFolder ->
            trashFolderContentsRecursively(subFolder.folderId, trashTime)
        }

        val folder = _allFolders.value.find { it.folderId == folderId }
        if (folder != null) {
            repository.insertFolder(folder.copy(isDeleted = true, createdAt = trashTime))
        }
    }

    fun createNewNote(title: String = "", forceHomeFolder: Boolean = false, onNoteCreated: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val newNoteId = UUID.randomUUID().toString()
            val targetFolder = if (forceHomeFolder) null else _selectedFolderId.value
            val fileName = "note_$newNoteId.json"

            val metadata = NoteMetadataEntity(
                noteId = newNoteId,
                title = title,
                icon = null,
                folderId = targetFolder,
                isDaily = false,
                dateString = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                filePath = fileName,
                snippet = ""
            )

            repository.saveNote(metadata, NoteContent(blocks = emptyList()))

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onNoteCreated(newNoteId)
            }
        }
    }

    private val _isVoiceTaskListening = MutableStateFlow(false)
    val isVoiceTaskListening: StateFlow<Boolean> = _isVoiceTaskListening.asStateFlow()

    private val _voiceTaskPartialText = MutableStateFlow("")
    val voiceTaskPartialText: StateFlow<String> = _voiceTaskPartialText.asStateFlow()

    private fun processVoiceTask(transcript: String) {
        if (transcript.isBlank()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val parsedTasks = taskExtractor.extractTasks(transcript)
            if (parsedTasks.isEmpty()) return@launch

            val systemTZ = TimeZone.currentSystemDefault()

            val tasksByDate = parsedTasks.groupBy { task ->
                if (task.timestamp != null) {
                    Instant.fromEpochMilliseconds(task.timestamp)
                        .toLocalDateTime(systemTZ)
                        .date
                        .toString()
                } else {
                    Clock.System.todayIn(systemTZ).toString()
                }
            }

            for ((targetDateString, tasks) in tasksByDate) {
                val content = repository.getDailyNote(targetDateString)
                val currentBlocks = mutableListOf<NoteBlock>()

                if (content != null && content.blocks.isNotEmpty()) {
                    currentBlocks.addAll(content.blocks)
                } else {
                    currentBlocks.add(TextBlock(id = "root_$targetDateString", text = ""))
                }

                for (task in tasks) {
                    val newVoiceTaskBlock = CheckboxBlock(
                        id = UUID.randomUUID().toString(),
                        text = task.taskText,
                        isChecked = false,
                        reminderTimestamp = task.timestamp,
                        indentationLevel = 0
                    )

                    currentBlocks.add(newVoiceTaskBlock)

                    VoiceTaskEventBus.emitTaskAdded(targetDateString, newVoiceTaskBlock)

                    task.timestamp?.let { timeInMillis ->
                        reminderScheduler.schedule(
                            blockId = newVoiceTaskBlock.id,
                            noteTitle = "Daily: $targetDateString",
                            text = task.taskText,
                            timestamp = timeInMillis
                        )
                    }
                }

                repository.saveDailyNote(targetDateString, NoteContent(blocks = currentBlocks))
            }
        }
    }

    fun startVoiceTaskListening(onPermissionNeeded: () -> Unit = {}) {
        _isVoiceTaskListening.value = true
        _voiceTaskPartialText.value = "Listening..."

        voiceRecognizer?.startListening(
            onPartial = { _voiceTaskPartialText.value = it },
            onResult = { result ->
                _isVoiceTaskListening.value = false
                processVoiceTask(result)
                _voiceTaskPartialText.value = ""
            },
            onError = { error ->
                _isVoiceTaskListening.value = false
                if (error == "No match") {
                    _voiceTaskPartialText.value = ""
                } else {
                    _voiceTaskPartialText.value = error
                }
            },
            onPermissionNeeded = {
                _isVoiceTaskListening.value = false
                _voiceTaskPartialText.value = ""
                onPermissionNeeded()
            }
        )
    }

    fun stopVoiceTaskListening() {
        voiceRecognizer?.stopListening()
        _isVoiceTaskListening.value = false
        _voiceTaskPartialText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizer?.destroy()
    }
}