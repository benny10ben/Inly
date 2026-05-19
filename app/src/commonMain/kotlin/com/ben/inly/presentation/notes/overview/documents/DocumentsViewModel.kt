package com.ben.inly.presentation.notes.overview.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.presentation.shared.editor.FocusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

data class DocumentGroup(
    val monthYear: String,
    val timestamp: Long,
    val blocks: List<DocumentBlock>
)

/**
 * Backs the Documents screen.
 * Extracts document blocks from all saved notes and groups them chronologically.
 */
class DocumentsViewModel constructor(
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groupedBlocks = MutableStateFlow<List<DocumentGroup>>(emptyList())
    val groupedBlocks: StateFlow<List<DocumentGroup>> = _groupedBlocks.asStateFlow()

    private val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    private val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    private val blockSourceMap = mutableMapOf<String, String>()
    private var initialLoad = true

    /**
     * Loops through every standalone note, finds DocumentBlocks,
     * and sorts them into date-based groups for the UI.
     */
    fun loadAllDocuments() {
        viewModelScope.launch {
            repository.getAllStandaloneNotes().collectLatest { allNotes ->
                if (initialLoad) _isLoading.value = true

                val monthGroups = mutableMapOf<String, MutableList<DocumentBlock>>()
                val monthTimestamps = mutableMapOf<String, Long>()
                val months = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

                blockSourceMap.clear()

                for (note in allNotes) {
                    val content = repository.getNoteContent(note.noteId)
                    val isInbox = note.title.equals("Inbox", ignoreCase = true)

                    val instant = Instant.fromEpochMilliseconds(note.createdAt)
                    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val monthYearString = "${months[localDate.monthNumber]} ${localDate.year}"

                    content?.blocks?.forEach { block ->
                        if (block is DocumentBlock) {
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
                    DocumentGroup(
                        monthYear = month,
                        timestamp = monthTimestamps[month] ?: 0L,
                        blocks = blocks.reversed()
                    )
                }.sortedByDescending { it.timestamp }

                _groupedBlocks.value = sortedGroups

                if (initialLoad) {
                    _isLoading.value = false
                    initialLoad = false
                }
            }
        }
    }

    /**
     * When a document is added directly from this screen, it needs a parent note.
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
     * Copies a newly picked file from the OS to internal storage and prepends it as a document block to the Inbox.
     */
    fun createNewDocumentWithFile(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                val (inboxMeta, content) = getOrCreateInbox()
                val newId = UUID.randomUUID().toString()

                val newBlock = DocumentBlock(
                    id = newId,
                    indentationLevel = 0,
                    localFilePath = mediaInfo.localFileName,
                    fileName = mediaInfo.originalName,
                    mimeType = mediaInfo.mimeType,
                    fileSizeString = mediaInfo.formattedSize
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

    fun handleDocumentPicked(blockId: String, uriString: String) {
        val originalNoteId = blockSourceMap[blockId] ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                val meta = repository.getAllStandaloneNotes().first().find { it.noteId == originalNoteId } ?: return@launch
                val content = repository.getNoteContent(originalNoteId) ?: return@launch

                val updatedBlocks = content.blocks.map {
                    if (it.id == blockId && it is DocumentBlock) {
                        it.copy(
                            localFilePath = mediaInfo.localFileName,
                            fileName = mediaInfo.originalName,
                            mimeType = mediaInfo.mimeType,
                            fileSizeString = mediaInfo.formattedSize
                        )
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
}