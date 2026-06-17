package com.ben.inly.domain.repository

import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.NoteContent
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for accessing and modifying note data.
 * ViewModels should only talk to this interface, never directly to the database or file manager.
 */
interface NoteRepository {
    // Daily Tab operations
    suspend fun getDailyNoteMetadata(dateString: String): NoteMetadataEntity?
    suspend fun getDailyNote(dateString: String): NoteContent?
    suspend fun saveDailyNote(dateString: String, content: NoteContent, updatedAt: Long? = null, remoteMeta: NoteMetadataEntity? = null)

    // Standalone Notes operations
    fun getAllStandaloneNotes(): Flow<List<NoteMetadataEntity>>
    fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>>
    suspend fun getNoteContent(noteId: String): NoteContent?
    suspend fun saveStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent)
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

    // sync
    suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity>
    fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>>

    suspend fun indexStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent)
    suspend fun indexDailyNote(dateString: String, content: NoteContent, metadata: NoteMetadataEntity)
}