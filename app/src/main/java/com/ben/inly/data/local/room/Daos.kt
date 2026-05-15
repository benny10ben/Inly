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

    // Grabs all normal notes while ignoring anything sitting in the trash
    @Query("SELECT * FROM notes_metadata WHERE isDaily = 0 AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllStandaloneNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE folderId = :folderId AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND dateString = :date LIMIT 1")
    suspend fun getDailyNoteMetadata(date: String): NoteMetadataEntity?

    @Query("SELECT * FROM notes_metadata WHERE isDaily = 1 AND snippet LIKE '%' || :query || '%' ORDER BY dateString DESC")
    fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE isFavorite = 1 AND trashedAt IS NULL ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>>

    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteMetadataEntity>>

    @Query("DELETE FROM notes_metadata WHERE noteId = :noteId")
    suspend fun deleteNoteMetadata(noteId: String)

    @Query("SELECT * FROM notes_metadata WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteMetadataEntity?

    // --- TRASH MANAGEMENT ---

    @Query("UPDATE notes_metadata SET trashedAt = NULL WHERE noteId = :noteId")
    suspend fun restoreNote(noteId: String)

    // Useful for automatically permanently deleting notes that have been in the trash too long
    @Query("SELECT * FROM notes_metadata WHERE trashedAt IS NOT NULL AND trashedAt < :cutoffTime")
    suspend fun getOldTrashedNotes(cutoffTime: Long): List<NoteMetadataEntity>
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