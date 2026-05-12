package com.ben.inly.presentation.notes.overview.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.HtmlMetadataFetcher
import com.ben.inly.presentation.shared.editor.FocusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class BookmarkGroup(
    val monthYear: String,
    val timestamp: Long,
    val blocks: List<BookmarkBlock>
)

/**
 * Backs the Bookmarks screen.
 * Extracts bookmark blocks from all saved notes, groups them chronologically,
 * and handles background metadata fetching for newly added links.
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
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

    /**
     * Scans through all notes and pulls out just the bookmark blocks,
     * sorting them into groups by month and year.
     */
    fun loadAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllStandaloneNotes().collectLatest { notes ->
                val allBookmarks = mutableListOf<Pair<BookmarkBlock, Long>>()
                blockSourceMap.clear()

                for (note in notes) {
                    val content = repository.getNoteContent(note.noteId) ?: continue
                    content.blocks.filterIsInstance<BookmarkBlock>().forEach { block ->
                        allBookmarks.add(Pair(block, note.updatedAt))
                        blockSourceMap[block.id] = note.noteId
                    }
                }

                val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val grouped = allBookmarks
                    .sortedByDescending { it.second }
                    .groupBy { formatter.format(Date(it.second)) }
                    .map { (monthYear, pairs) ->
                        BookmarkGroup(
                            monthYear = monthYear,
                            timestamp = pairs.first().second,
                            blocks = pairs.map { it.first }
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

    /**
     * Takes a URL from the input bar, saves it instantly to the Inbox,
     * and then quietly fetches the rich metadata in the background.
     */
    fun insertBookmarkWithUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var inboxMeta = repository.getAllStandaloneNotes().first().find { it.title.equals("Inbox", ignoreCase = true) }
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
                repository.saveStandaloneNote(inboxMeta, NoteContent(blocks = emptyList()))
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
            repository.saveStandaloneNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            withContext(NonCancellable) {
                try {
                    val metadata = HtmlMetadataFetcher.fetchMetadata(url)
                    val currentContent = repository.getNoteContent(inboxMeta.noteId) ?: return@withContext

                    updatedBlocks = currentContent.blocks.map {
                        if (it.id == newId && it is BookmarkBlock) {
                            it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl)
                        } else it
                    }
                    repository.saveStandaloneNote(inboxMeta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Refreshes an existing bookmark block with new URL data and re-fetches its metadata.
     */
    fun handleUrlSubmit(blockId: String, url: String) {
        val noteId = blockSourceMap[blockId] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val meta = repository.getAllStandaloneNotes().first().find { it.noteId == noteId } ?: return@launch
            var content = repository.getNoteContent(noteId) ?: return@launch

            var updatedBlocks = content.blocks.map {
                if (it.id == blockId && it is BookmarkBlock) it.copy(url = url, title = "Loading...") else it
            }
            repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))

            withContext(NonCancellable) {
                try {
                    val metadata = HtmlMetadataFetcher.fetchMetadata(url)
                    content = repository.getNoteContent(noteId) ?: return@withContext
                    updatedBlocks = content.blocks.map {
                        if (it.id == blockId && it is BookmarkBlock) {
                            it.copy(title = metadata.title, description = metadata.description, previewImageUrl = metadata.imageUrl)
                        } else it
                    }
                    repository.saveStandaloneNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = updatedBlocks))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}