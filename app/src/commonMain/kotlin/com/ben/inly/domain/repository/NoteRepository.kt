package com.ben.inly.domain.repository

import com.ben.inly.data.local.room.BookmarkBlockEntity
import com.ben.inly.data.local.room.CalendarTaskEntity
import com.ben.inly.data.local.room.DatabaseTemplateEntity
import com.ben.inly.data.local.room.DocumentBlockEntity
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.ImageBlockEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.NoteSearchResult
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for accessing and modifying note data.
 * ViewModels should only talk to this interface, never directly to the database or file manager.
 */
interface NoteRepository {

    fun getCalendarTasksForMonth(yearMonth: String): Flow<List<CalendarTaskEntity>>

    // Daily Tab operations
    suspend fun getDailyNoteMetadata(dateString: String): NoteMetadataEntity?
    suspend fun getDailyNote(dateString: String): NoteContent?
    suspend fun saveDailyNote(dateString: String, content: NoteContent, updatedAt: Long? = null, remoteMeta: NoteMetadataEntity? = null)

    // Notes operations
    fun getAllNotes(): Flow<List<NoteMetadataEntity>>
    fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>>
    suspend fun getNoteContent(noteId: String): NoteContent?
    suspend fun saveNote(metadata: NoteMetadataEntity, content: NoteContent)
    suspend fun deleteNote(noteId: String, filePath: String)

    // Favorites and Trash management
    fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>>
    fun getTrashedNotes(): Flow<List<NoteMetadataEntity>>
    suspend fun restoreNote(noteId: String)
    suspend fun cleanupOldTrashedNotes()

    // Folder management
    fun getAllFolders(): Flow<List<FolderEntity>>
    suspend fun insertFolder(folder: FolderEntity)
    suspend fun deleteFolder(folderId: String)
    suspend fun getNoteById(noteId: String): NoteMetadataEntity?

    // Database
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertOrUpdateTag(tagId: String, name: String, colorHex: String)
    suspend fun deleteTag(tagId: String)

    // Database templates (saved schemas: columns + views, never rows)
    fun getAllDatabaseTemplates(): Flow<List<DatabaseTemplateEntity>>
    suspend fun insertDatabaseTemplate(template: DatabaseTemplateEntity)
    suspend fun deleteDatabaseTemplate(templateId: String)

    // sync
    suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity>
    fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>>

    // Cross-note search
    suspend fun searchNotes(query: String): List<NoteSearchResult>

    suspend fun indexNote(metadata: NoteMetadataEntity, content: NoteContent)
    suspend fun indexDailyNote(dateString: String, content: NoteContent, metadata: NoteMetadataEntity)

    fun getAllTasksFlow(): Flow<List<CalendarTaskEntity>>
    fun getIncompleteTasksCount(): Flow<Int>

    fun getAllImagesFlow(): Flow<List<ImageBlockEntity>>
    fun getAllDocumentsFlow(): Flow<List<DocumentBlockEntity>>
    fun getAllBookmarksFlow(): Flow<List<BookmarkBlockEntity>>
    fun getImagesCount(): Flow<Int>
    fun getDocumentsCount(): Flow<Int>
    fun getBookmarksCount(): Flow<Int>

    fun getAllLinkableNotes(): Flow<List<NoteMetadataEntity>>

    fun observeNoteContent(noteId: String): Flow<NoteContent?>
    fun observeDailyNote(dateString: String): Flow<NoteContent?>

    suspend fun updateNoteSortOrder(noteId: String, order: Int)
    suspend fun updateFolderSortOrder(folderId: String, order: Int)

    // clear cache after import
    fun clearCaches()
}