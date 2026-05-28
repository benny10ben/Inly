package com.ben.inly.presentation.notes.notes

import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.BaseEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Handles the logic for standard, free-form notes.
 * Manages the core editor blocks alongside the note's title, cover image, icon, and trash status.
 */
class StandaloneEditorViewModel constructor(
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

    private var currentMetadata: NoteMetadataEntity? = null

    init {
        viewModelScope.launch {
            com.ben.inly.domain.util.SyncEventBus.syncCompletedEvent.collect { syncedEntityId ->
                val currentId = currentMetadata?.noteId
                if (currentId != null && syncedEntityId == currentId) {

                    val updatedMeta = withContext(Dispatchers.IO) { repository.getNoteById(currentId) }
                    if (updatedMeta != null) {
                        currentMetadata = updatedMeta
                        _noteTitle.value = updatedMeta.title
                        _noteIcon.value = updatedMeta.icon
                        _isFavorite.value = updatedMeta.isFavorite
                        _coverImagePath.value = updatedMeta.coverImagePath
                    }

                    val content = withContext(Dispatchers.IO) { repository.getNoteContent(currentId) }
                    val newBlocks = content?.blocks ?: emptyList()
                    _blocks.value = recalculateNumberedLists(
                        if (newBlocks.isEmpty()) listOf(TextBlock(id = "root_$currentId", text = ""))
                        else newBlocks
                    )
                }
            }
        }
    }

    override suspend fun performSave() {
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        // Generates a quick text preview for the dashboard UI
        val previewText = snapshot.joinToString(" ") { block ->
            when(block) {
                is TextBlock -> block.text
                is HeadingBlock -> block.text
                is CheckboxBlock -> block.text
                is BulletedListBlock -> block.text
                is NumberedListBlock -> block.text
                is ToggleBlock -> block.text
                is CodeBlock -> block.code
                else -> ""
            }
        }.trim().take(120)

        withContext(Dispatchers.IO) {
            val updatedMeta = meta.copy(
                title = _noteTitle.value,
                icon = _noteIcon.value,
                isFavorite = _isFavorite.value,
                coverImagePath = _coverImagePath.value,
                updatedAt = System.currentTimeMillis(),
                snippet = previewText
            )
            repository.saveStandaloneNote(updatedMeta, NoteContent(blocks = snapshot))
            currentMetadata = updatedMeta
        }
    }

    override fun getNoteTitleForReminder(): String {
        return _noteTitle.value.ifBlank { "Standalone Note" }
    }

    fun loadNote(noteId: String) {
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            currentMetadata = repository.getNoteById(noteId)

            if (currentMetadata != null) {
                _noteTitle.value = currentMetadata?.title ?: ""
                _noteIcon.value = currentMetadata?.icon
                _isFavorite.value = currentMetadata?.isFavorite ?: false
                _coverImagePath.value = currentMetadata?.coverImagePath

                val content = repository.getNoteContent(noteId)
                val existingBlocks = content?.blocks ?: emptyList()

                _blocks.value = recalculateNumberedLists(
                    if (existingBlocks.isEmpty()) listOf(TextBlock(id = "root_$noteId", text = ""))
                    else existingBlocks
                )
            }
            _isLoading.value = false
        }
    }

    fun updateTitle(newTitle: String) {
        _noteTitle.value = newTitle
        scheduleAutosave()
    }

    fun updateIcon(newIcon: String?) {
        _noteIcon.value = newIcon
        scheduleAutosave()
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        scheduleAutosave()
    }

    fun handleCoverImagePicked(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                _coverImagePath.value = mediaInfo.localFileName
                scheduleAutosave()
            }
        }
    }

    fun removeCoverImage() {
        _coverImagePath.value = null
        scheduleAutosave()
    }

    fun moveToTrash(onMoved: () -> Unit) {
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        viewModelScope.launch(Dispatchers.IO) {
            val trashedMeta = meta.copy(trashedAt = System.currentTimeMillis())
            repository.saveStandaloneNote(trashedMeta, NoteContent(blocks = snapshot))
            currentMetadata = trashedMeta

            withContext(Dispatchers.Main) {
                onMoved()
            }
        }
    }
}