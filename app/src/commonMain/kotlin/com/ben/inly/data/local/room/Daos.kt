package com.ben.inly.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Handles all local database operations for notes.
 * This mostly deals with filtering metadata, like checking if a note is trashed,
 * favorited, or belongs to a specific folder before trying to load its actual content.
 */
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMetadata(metadata: NoteMetadataEntity)

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 0 AND trashedAt IS NULL AND isSubNote = 0 AND isTemplate = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE folderId = :folderId AND trashedAt IS NULL AND isSubNote = 0 AND isTemplate = 0 ORDER BY updatedAt DESC")
    fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND dateString = :date AND isTemplate = 0 LIMIT 1")
    suspend fun getDailyNoteMetadata(date: String): NoteMetadataEntity?

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND isTemplate = 0 AND snippet LIKE '%' || :query || '%' ORDER BY dateString DESC")
    fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>>

    // Cross-note search
    @Query(
        """
        SELECT * FROM notes_metadata
        WHERE trashedAt IS NULL AND isSubNote = 0 AND isTemplate = 0
        AND (title LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        """
    )
    fun searchNotesByTitleOrSnippet(query: String): Flow<List<NoteMetadataEntity>>

    // isTemplate = 0 here too: this is also used to resolve cross-note *content* search hits
    // (NoteRepositoryImpl.searchNotes), and that content match comes from a raw block-JSON scan
    // that has no idea what a template is, so the filter has to be enforced on this side instead.
    @Query("SELECT * FROM notes_metadata WHERE noteId IN (:ids) AND isTemplate = 0")
    suspend fun getNotesByIds(ids: List<String>): List<NoteMetadataEntity>

    @Query("SELECT * FROM notes_metadata WHERE isFavorite = 1 AND trashedAt IS NULL AND isTemplate = 0")
    fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL AND isTemplate = 0 ORDER BY trashedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteMetadataEntity>>

    @Query("DELETE FROM notes_metadata WHERE noteId = :noteId")
    suspend fun deleteNoteMetadata(noteId: String)

    // Deliberately NOT filtered by isTemplate - this is the generic single-row lookup used both
    // for regular notes (rename/trash/move) and internally to fetch a template's own metadata
    // (HomeViewModel.createNoteFromTemplate). Templates never surface through this method unless
    // the caller already has their id, so it doesn't violate the "no leaking into lists" rule.
    @Query("SELECT * FROM notes_metadata WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteMetadataEntity?

    @Query("UPDATE notes_metadata SET trashedAt = NULL WHERE noteId = :noteId")
    suspend fun restoreNote(noteId: String)

    // Deliberately NOT filtered by isTemplate - this is what actually purges a soft-deleted
    // template's row (see NoteRepositoryImpl.deleteTemplate) once every paired device has had
    // 30 days to receive its tombstone via sync, exactly like a regular trashed note.
    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL AND trashedAt < :cutoffTime")
    suspend fun getOldTrashedNotes(cutoffTime: Long): List<NoteMetadataEntity>

    // Deliberately NOT filtered by isTemplate - sync needs to propagate template
    // creation/edits/deletion across paired devices exactly like any other note.
    @Query("SELECT * FROM notes_metadata WHERE updatedAt > :timestamp")
    suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity>

    @Query("SELECT COUNT(*) FROM calendar_tasks WHERE isChecked = 0")
    fun getIncompleteTasksCount(): Flow<Int>

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 0 AND trashedAt IS NULL AND isTemplate = 0")
    fun getAllLinkableNotes(): Flow<List<NoteMetadataEntity>>

    // Deliberately NOT filtered by isTemplate - a backup is a full snapshot, so templates must
    // round-trip through export/import exactly like every other note.
    @Query("SELECT * FROM notes_metadata")
    suspend fun getAllNotesForBackup(): List<NoteMetadataEntity>

    @Query("UPDATE notes_metadata SET sortOrder = :order WHERE noteId = :noteId")
    suspend fun updateNoteSortOrder(noteId: String, order: Int)

    // Templates menu: every reusable template (predefined + user-saved), alphabetical so the
    // search/filter UI has a stable starting order.
    @Query("SELECT * FROM notes_metadata WHERE isTemplate = 1 AND trashedAt IS NULL ORDER BY title ASC")
    fun getAllTemplates(): Flow<List<NoteMetadataEntity>>
}

/**
 * Simple DAO to manage the creation and deletion of folders.
 */
@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY CASE WHEN sortOrder = 0 THEN 1 ELSE 0 END, sortOrder ASC, createdAt ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("DELETE FROM folders WHERE folderId = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Query("UPDATE folders SET sortOrder = :order WHERE folderId = :folderId")
    suspend fun updateFolderSortOrder(folderId: String, order: Int)
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTag(tag: TagEntity)

    @Query("SELECT * FROM global_tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("DELETE FROM global_tags WHERE tagId = :tagId")
    suspend fun deleteTag(tagId: String)
}

@Dao
interface CalendarTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<CalendarTaskEntity>)

    // Fast query for the Calendar Pager to render the dot indicators.
    // Pass yearMonth as "2026-07" to get everything in July.
    @Query("SELECT * FROM calendar_tasks WHERE targetDate LIKE :yearMonth || '%'")
    fun getTasksForMonth(yearMonth: String): Flow<List<CalendarTaskEntity>>

    // Precise query for the Bottom Sheet when a user clicks a specific day.
    @Query("SELECT * FROM calendar_tasks WHERE targetDate = :dateString")
    fun getTasksForDate(dateString: String): Flow<List<CalendarTaskEntity>>

    // Allows the Bottom Sheet to instantly toggle a checkbox without loading the full note.
    @Query("UPDATE calendar_tasks SET isChecked = :isChecked WHERE blockId = :blockId")
    suspend fun updateTaskStatus(blockId: String, isChecked: Boolean)

    // Used by the NoteEditorViewModel/DailyEditorViewModel to clear old tasks before saving new ones.
    @Query("DELETE FROM calendar_tasks WHERE noteId = :noteId")
    suspend fun deleteTasksByNoteId(noteId: String)

    // For when a user backspaces/deletes a single task in the editor.
    @Query("DELETE FROM calendar_tasks WHERE blockId = :blockId")
    suspend fun deleteTaskById(blockId: String)

    @Query("SELECT * FROM calendar_tasks")
    fun getAllTasksFlow(): Flow<List<CalendarTaskEntity>>
}

@Dao
interface ImageBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImages(images: List<ImageBlockEntity>)

    @Query("DELETE FROM image_blocks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Query("SELECT * FROM image_blocks ORDER BY noteCreatedAt DESC")
    fun getAllImagesFlow(): Flow<List<ImageBlockEntity>>

    @Query("SELECT COUNT(*) FROM image_blocks")
    fun getImagesCount(): Flow<Int>
}

@Dao
interface DocumentBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentBlockEntity>)

    @Query("DELETE FROM document_blocks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Query("SELECT * FROM document_blocks ORDER BY noteCreatedAt DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentBlockEntity>>

    @Query("SELECT COUNT(*) FROM document_blocks")
    fun getDocumentsCount(): Flow<Int>
}

@Dao
interface BookmarkBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmarks(bookmarks: List<BookmarkBlockEntity>)

    @Query("DELETE FROM bookmark_blocks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Query("SELECT * FROM bookmark_blocks ORDER BY noteUpdatedAt DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkBlockEntity>>

    @Query("SELECT COUNT(*) FROM bookmark_blocks")
    fun getBookmarksCount(): Flow<Int>
}

/**
 * Manages saved DatabaseBlock schemas (columns/views) that users can reuse when
 * creating a new database block.
 */
@Dao
interface DatabaseTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: DatabaseTemplateEntity)

    @Query("SELECT * FROM database_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<DatabaseTemplateEntity>>

    @Query("DELETE FROM database_templates WHERE templateId = :templateId")
    suspend fun deleteTemplate(templateId: String)
}
