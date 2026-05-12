package com.ben.inly.presentation.notes.overview.images

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.shared.editor.FocusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ImageGroup(
    val monthYear: String,
    val timestamp: Long,
    val blocks: List<ImageBlock>
)

/**
 * Backs the Images gallery screen.
 * Extracts image blocks from all saved notes and groups them chronologically.
 */
@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groupedBlocks = MutableStateFlow<List<ImageGroup>>(emptyList())
    val groupedBlocks: StateFlow<List<ImageGroup>> = _groupedBlocks.asStateFlow()

    private val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    private val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    private val blockSourceMap = mutableMapOf<String, String>()

    /**
     * Loops through every standalone note, finds ImageBlocks containing actual files,
     * and sorts them into date-based groups for the UI.
     */
    fun loadAllImages() {
        viewModelScope.launch {
            repository.getAllStandaloneNotes().collectLatest { allNotes ->
                _isLoading.value = true

                val monthGroups = mutableMapOf<String, MutableList<ImageBlock>>()
                val monthTimestamps = mutableMapOf<String, Long>()
                val dateFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

                blockSourceMap.clear()

                for (note in allNotes) {
                    val content = repository.getNoteContent(note.noteId)
                    val isInbox = note.title.equals("Inbox", ignoreCase = true)

                    val monthYearString = dateFormatter.format(Date(note.createdAt))

                    content?.blocks?.forEach { block ->
                        if (block is ImageBlock) {
                            if (block.localFilePath != null || isInbox) {
                                monthGroups.getOrPut(monthYearString) { mutableListOf() }
                                    .add(block.copy(indentationLevel = 0))

                                blockSourceMap[block.id] = note.noteId
                                monthTimestamps[monthYearString] = note.createdAt
                            }
                        }
                    }
                }

                val sortedGroups = monthGroups.map { (month, blocks) ->
                    ImageGroup(
                        monthYear = month,
                        timestamp = monthTimestamps[month] ?: 0L,
                        blocks = blocks.reversed()
                    )
                }.sortedByDescending { it.timestamp }

                _groupedBlocks.value = sortedGroups
                _isLoading.value = false
            }
        }
    }

    /**
     * When an image is added directly from the gallery, it needs a parent note.
     * This fetches the default 'Inbox' note, or creates it if missing.
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
                updatedAt = System.currentTimeMillis(), filePath = "note_$noteId.json", snippet = "Saved media and tasks."
            )
            content = NoteContent(blocks = emptyList())
        } else {
            noteId = inboxNote.noteId
            content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
        }
        return Pair(inboxNote, content)
    }

    /**
     * Copies a newly picked image from the OS to internal storage and prepends it to the Inbox.
     */
    fun createNewImageWithFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uri)
            if (mediaInfo != null) {
                val (inboxMeta, content) = getOrCreateInbox()
                val newId = UUID.randomUUID().toString()

                val newBlock = ImageBlock(
                    id = newId,
                    indentationLevel = 0,
                    localFilePath = mediaInfo.localFileName
                )

                val updatedBlocks = listOf(newBlock) + content.blocks
                repository.saveStandaloneNote(
                    inboxMeta.copy(updatedAt = System.currentTimeMillis()),
                    NoteContent(blocks = updatedBlocks)
                )

                _focusRequest.value = FocusRequest(id = newId)
            }
        }
    }

    fun handleImagePicked(blockId: String, uri: Uri) {
        val originalNoteId = blockSourceMap[blockId] ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uri)
            if (mediaInfo != null) {
                val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
                val content = repository.getNoteContent(originalNoteId) ?: return@launch

                val updatedBlocks = content.blocks.map {
                    if (it.id == blockId && it is ImageBlock) {
                        it.copy(localFilePath = mediaInfo.localFileName)
                    } else it
                }
                repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
            }
        }
    }

    fun toggleSelection(id: String) { _selectedBlockIds.update { if (it.contains(id)) it - id else it + id } }
    fun clearSelection() { _selectedBlockIds.value = emptySet() }
    fun clearFocusRequest() { _focusRequest.value = null }

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

    fun getSelectedText() = ""
    fun cutSelectedBlocks() = ""
    fun setFocusedBlock(id: String) {}
    fun handleBackspaceOnEmpty(id: String) {
        toggleSelection(id)
        deleteSelectedBlocks()
    }

    fun selectAllBlocks() {
        _selectedBlockIds.value = groupedBlocks.value
            .flatMap { group -> group.blocks }
            .map { block -> block.id }
            .toSet()
    }

    /**
     * Tracks down the original note that holds the image, removes the block, and updates the database.
     */
    fun deleteImageBlock(blockId: String) {
        val originalNoteId = blockSourceMap[blockId] ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
            val content = repository.getNoteContent(originalNoteId) ?: return@launch

            val updatedBlocks = content.blocks.filterNot { it.id == blockId }
            repository.saveStandaloneNote(
                meta.copy(updatedAt = System.currentTimeMillis()),
                NoteContent(blocks = updatedBlocks)
            )
        }
    }
}