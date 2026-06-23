package com.ben.inly.presentation.tabs.home.note

import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.BaseEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModel constructor(
    repository: NoteRepository,
    mediaStorageHelper: MediaStorageHelper,
    reminderScheduler: ReminderScheduler,
    audioRecorder: AudioRecorder
) : BaseEditorViewModel(repository, mediaStorageHelper, reminderScheduler, audioRecorder) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    val noteTitle: StateFlow<String> = _noteTitle.asStateFlow()

    private val _noteIcon = MutableStateFlow<String?>(null)
    val noteIcon: StateFlow<String?> = _noteIcon.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _coverImagePath = MutableStateFlow<String?>(null)
    val coverImagePath: StateFlow<String?> = _coverImagePath.asStateFlow()

    private val _noteUpdatedAt = MutableStateFlow(0L)
    val noteUpdatedAt: StateFlow<Long> = _noteUpdatedAt.asStateFlow()

    private var currentMetadata: NoteMetadataEntity? = null
    private val _currentlyLoadedNoteId = MutableStateFlow<String?>(null)
    private var currentlyLoadedNoteId: String?
        get() = _currentlyLoadedNoteId.value
        set(value) { _currentlyLoadedNoteId.value = value }

    init {
        viewModelScope.launch {
            com.ben.inly.domain.util.SyncEventBus.syncCompletedEvent.collect { syncedEntityId ->
                val currentId = currentMetadata?.noteId
                if (currentId != null && syncedEntityId == currentId) {
                    if (autosaveJob?.isActive == true) return@collect

                    val updatedMeta = withContext(Dispatchers.IO) { repository.getNoteById(currentId) }
                    if (updatedMeta != null) {
                        currentMetadata = updatedMeta
                        _noteTitle.value = updatedMeta.title
                        _noteIcon.value = updatedMeta.icon
                        _isFavorite.value = updatedMeta.isFavorite
                        _coverImagePath.value = updatedMeta.coverImagePath
                        _noteUpdatedAt.value = updatedMeta.updatedAt
                    }

                    val content = withContext(Dispatchers.IO) { repository.getNoteContent(currentId) }
                    val newBlocks = content?.blocks ?: emptyList()
                    val resolved = recalculateNumberedLists(
                        if (newBlocks.isEmpty()) listOf(TextBlock(id = java.util.UUID.randomUUID().toString(), text = ""))
                        else newBlocks
                    )
                    if (resolved != _blocks.value) {
                        _blocks.value = resolved
                    }
                }
            }
        }
        viewModelScope.launch {
            _currentlyLoadedNoteId
                .filterNotNull()
                .flatMapLatest { noteId -> repository.observeNoteContent(noteId) }
                .filterNotNull()
                .collect { freshContent ->
                    if (_isLoading.value) return@collect
                    if (autosaveJob?.isActive == true) return@collect

                    val final = recalculateNumberedLists(
                        if (freshContent.blocks.isEmpty())
                            listOf(TextBlock(id = java.util.UUID.randomUUID().toString(), text = ""))
                        else freshContent.blocks
                    )
                    if (final != _blocks.value) {
                        _blocks.value = final
                    }
                }
        }
    }

    override suspend fun performSave() {
        if (_isLoading.value) return
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        val updatedMeta = meta.copy(
            title = _noteTitle.value,
            icon = _noteIcon.value,
            isFavorite = _isFavorite.value,
            coverImagePath = _coverImagePath.value,
            snippet = generateSnippet(snapshot),
            updatedAt = System.currentTimeMillis()
        )
        currentMetadata = updatedMeta
        _noteUpdatedAt.value = updatedMeta.updatedAt

        withContext(Dispatchers.IO) {
            repository.saveNote(
                updatedMeta,
                NoteContent(blocks = snapshot)
            )
        }
    }

    override suspend fun performIndexing() {
        if (_isLoading.value) return
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        withContext(Dispatchers.IO) {
            repository.indexNote(
                meta,
                NoteContent(blocks = snapshot)
            )
        }
    }

    override fun getNoteTitleForReminder(): String {
        return _noteTitle.value.ifBlank { "Note" }
    }

    fun loadNote(noteId: String) {
        if (currentlyLoadedNoteId == noteId) return
        currentlyLoadedNoteId = noteId

        com.ben.inly.domain.util.AiEventBus.activeNoteId = noteId

        autosaveJob?.cancel()
        indexingJob?.cancel()

        val previousMeta = currentMetadata
        val snapshot = _blocks.value.toList()

        val flushedMeta = previousMeta?.copy(
            title = _noteTitle.value,
            icon = _noteIcon.value,
            isFavorite = _isFavorite.value,
            coverImagePath = _coverImagePath.value,
            snippet = generateSnippet(snapshot),
            updatedAt = System.currentTimeMillis()
        )

        clearSelection()

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            if (flushedMeta != null && previousMeta != null) {
                val contentToSave = NoteContent(blocks = snapshot)
                repository.saveNote(flushedMeta, contentToSave)
                repository.indexNote(flushedMeta, contentToSave)
            }

            currentMetadata = repository.getNoteById(noteId)

            if (currentMetadata != null) {
                _noteTitle.value = currentMetadata?.title ?: ""
                _noteIcon.value = currentMetadata?.icon
                _isFavorite.value = currentMetadata?.isFavorite ?: false
                _coverImagePath.value = currentMetadata?.coverImagePath
                _noteUpdatedAt.value = currentMetadata?.updatedAt ?: 0L

                val content = repository.getNoteContent(noteId)
                val existingBlocks = content?.blocks ?: emptyList()

                _blocks.value = recalculateNumberedLists(
                    if (existingBlocks.isEmpty()) listOf(TextBlock(id = java.util.UUID.randomUUID().toString(), text = ""))
                    else existingBlocks
                )
            }
            _isLoading.value = false
            isAiIndexDirty = false
            lastIndexedContentHash = 0
        }
    }

    fun updateTitle(newTitle: String) {
        _noteTitle.value = newTitle
        scheduleAutosave()
    }

    fun updateIcon(newIcon: String?) {
        _noteIcon.value = newIcon
        viewModelScope.launch { performSave() }
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        viewModelScope.launch { performSave() }
    }

    fun handleCoverImagePicked(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                _coverImagePath.value = mediaInfo.localFileName
                performSave()
            }
        }
    }

    fun removeCoverImage() {
        _coverImagePath.value = null
        viewModelScope.launch { performSave() }
    }

    fun moveToTrash(onMoved: () -> Unit) {
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        viewModelScope.launch(Dispatchers.IO) {
            val trashedMeta = meta.copy(
                title = _noteTitle.value,
                icon = _noteIcon.value,
                isFavorite = _isFavorite.value,
                coverImagePath = _coverImagePath.value,
                snippet = generateSnippet(snapshot),
                trashedAt = System.currentTimeMillis()
            )

            val content = NoteContent(blocks = snapshot)

            repository.saveNote(trashedMeta, content)
            repository.indexNote(trashedMeta, content)

            currentMetadata = trashedMeta

            withContext(Dispatchers.Main) {
                onMoved()
            }
        }
    }

    private fun generateSnippet(blocks: List<NoteBlock>): String {
        return blocks.asSequence()
            .mapNotNull { extractTextFromBlock(it) }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
            .take(120)
    }

    private fun extractTextFromBlock(block: NoteBlock): String? {
        if (block.isDeleted) return null
        return when (block) {
            is TextBlock -> block.text
            is HeadingBlock -> block.text
            is QuoteBlock -> block.text
            is CheckboxBlock -> block.text
            is BulletedListBlock -> block.text
            is NumberedListBlock -> block.text
            is ToggleBlock -> block.text
            is RowContainerBlock -> {
                block.columns
                    .flatMap { it.blocks }
                    .mapNotNull { extractTextFromBlock(it) }
                    .joinToString(" ")
            }
            else -> null
        }
    }
}