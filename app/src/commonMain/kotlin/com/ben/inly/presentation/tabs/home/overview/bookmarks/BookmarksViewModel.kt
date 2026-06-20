package com.ben.inly.presentation.tabs.home.overview.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.markDeleted
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.HtmlMetadataFetcher
import com.ben.inly.presentation.shared.editor.FocusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

data class BookmarkGroup(
    val monthYear: String,
    val timestamp: Long,
    val blocks: List<BookmarkBlock>
)

class BookmarksViewModel constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groupedBlocks = MutableStateFlow<List<BookmarkGroup>>(emptyList())
    val groupedBlocks: StateFlow<List<BookmarkGroup>> = _groupedBlocks.asStateFlow()

    private val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    private val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    private val blockSourceMap = mutableMapOf<String, String>()

    fun loadAllBookmarks() {
        viewModelScope.launch {
            repository.getAllBookmarksFlow().collectLatest { allBookmarks ->
                blockSourceMap.clear()

                val months = arrayOf("", "January", "February", "March", "April", "May",
                    "June", "July", "August", "September", "October", "November", "December")

                val grouped = allBookmarks
                    .onEach { blockSourceMap[it.blockId] = it.noteId }
                    .groupBy {
                        val localDate = Instant.fromEpochMilliseconds(it.noteUpdatedAt)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        "${months[localDate.monthNumber]} ${localDate.year}"
                    }
                    .map { (monthYear, entities) ->
                        BookmarkGroup(
                            monthYear = monthYear,
                            timestamp = entities.first().noteUpdatedAt,
                            blocks    = entities.map { e ->
                                BookmarkBlock(
                                    id              = e.blockId,
                                    url             = e.url,
                                    title           = e.title,
                                    description     = e.description,
                                    previewImageUrl = e.previewImageUrl
                                )
                            }
                        )
                    }

                _groupedBlocks.value = grouped
                _isLoading.value = false
            }
        }
    }

    fun toggleSelection(id: String) {
        _selectedBlockIds.update { if (it.contains(id)) it - id else it + id }
    }

    fun clearSelection() {
        _selectedBlockIds.value = emptySet()
    }

    fun selectAllBlocks() {
        _selectedBlockIds.value = _groupedBlocks.value.flatMap { it.blocks }.map { it.id }.toSet()
    }

    fun clearFocusRequest() {
        _focusRequest.value = null
    }

    fun getSelectedText(): String {
        return _groupedBlocks.value.flatMap { it.blocks }
            .filter { it.id in _selectedBlockIds.value }
            .joinToString("\n") { it.url }
    }

    fun cutSelectedBlocks(): String {
        val text = getSelectedText()
        deleteSelectedBlocks()
        return text
    }

    fun deleteSelectedBlocks() {
        val toDelete = _selectedBlockIds.value
        if (toDelete.isEmpty()) return

        val blocksByNote = toDelete.groupBy { blockSourceMap[it] }
        viewModelScope.launch(Dispatchers.IO) {
            blocksByNote.forEach { (noteId, blockIdsToDelete) ->
                if (noteId != null) {
                    val meta = repository.getAllNotes().first().find { it.noteId == noteId }
                    if (meta != null) {
                        val content = repository.getNoteContent(noteId)
                        if (content != null) {

                            val updatedBlocks = content.blocks.map { block ->
                                if (block.id in blockIdsToDelete) block.markDeleted() else block
                            }

                            repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                        }
                    }
                }
            }
            clearSelection()
        }
    }

    fun insertBookmarkWithUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var inboxMeta = repository.getAllNotes().first().find { it.title.equals("Inbox", ignoreCase = true) }
            if (inboxMeta == null) {
                inboxMeta = NoteMetadataEntity(
                    noteId = UUID.randomUUID().toString(),
                    title = "Inbox",
                    folderId = null,
                    isDaily = false,
                    dateString = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    filePath = "note_${UUID.randomUUID()}.json",
                    snippet = ""
                )
                repository.saveNote(inboxMeta, NoteContent(blocks = emptyList()))
            }

            val content = repository.getNoteContent(inboxMeta.noteId) ?: NoteContent(blocks = emptyList())
            val newId = UUID.randomUUID().toString()
            val newBlock = BookmarkBlock(
                id = newId,
                url = url,
                title = "Loading preview...",
                description = null,
                previewImageUrl = null
            )

            var updatedBlocks = content.blocks + newBlock
            repository.saveNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            withContext(NonCancellable) {
                try {
                    val metadata = HtmlMetadataFetcher.fetchMetadata(url)
                    val currentContent = repository.getNoteContent(inboxMeta.noteId) ?: return@withContext

                    updatedBlocks = currentContent.blocks.map {
                        if (it.id == newId && it is BookmarkBlock) {
                            it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl)
                        } else it
                    }
                    repository.saveNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun handleUrlSubmit(blockId: String, url: String) {
        val noteId = blockSourceMap[blockId] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllNotes().first().find { it.noteId == noteId } ?: return@launch
            var content = repository.getNoteContent(noteId) ?: return@launch

            var updatedBlocks = content.blocks.map {
                if (it.id == blockId && it is BookmarkBlock) it.copy(url = url, title = "Loading...") else it
            }
            repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            withContext(NonCancellable) {
                try {
                    val metadata = HtmlMetadataFetcher.fetchMetadata(url)
                    content = repository.getNoteContent(noteId) ?: return@withContext
                    updatedBlocks = content.blocks.map {
                        if (it.id == blockId && it is BookmarkBlock) {
                            it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl)
                        } else it
                    }
                    repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}