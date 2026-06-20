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

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 0 AND trashedAt IS NULL AND isSubNote = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE folderId = :folderId AND trashedAt IS NULL AND isSubNote = 0 ORDER BY updatedAt DESC")
    fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND dateString = :date LIMIT 1")
    suspend fun getDailyNoteMetadata(date: String): NoteMetadataEntity?

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND snippet LIKE '%' || :query || '%' ORDER BY dateString DESC")
    fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE isFavorite = 1 AND trashedAt IS NULL AND isSubNote = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL AND isSubNote = 0 ORDER BY trashedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteMetadataEntity>>

    @Query("DELETE FROM notes_metadata WHERE noteId = :noteId")
    suspend fun deleteNoteMetadata(noteId: String)

    @Query("SELECT * FROM notes_metadata WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteMetadataEntity?

    @Query("UPDATE notes_metadata SET trashedAt = NULL WHERE noteId = :noteId")
    suspend fun restoreNote(noteId: String)

    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL AND trashedAt < :cutoffTime")
    suspend fun getOldTrashedNotes(cutoffTime: Long): List<NoteMetadataEntity>

    @Query("SELECT * FROM notes_metadata WHERE updatedAt > :timestamp")
    suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity>

    @Query("SELECT * FROM calendar_tasks")
    fun getAllTasksFlow(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT COUNT(*) FROM calendar_tasks WHERE isChecked = 0")
    fun getIncompleteTasksCount(): Flow<Int>
}

/**
 * Simple DAO to manage the creation and deletion of folders.
 */
@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("DELETE FROM folders WHERE folderId = :folderId")
    suspend fun deleteFolder(folderId: String)
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
