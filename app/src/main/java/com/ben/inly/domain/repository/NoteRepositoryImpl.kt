package com.ben.inly.data.repository

import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Bridges the gap between the Room database (which handles fast, searchable metadata)
 * and the FileStorageManager (which handles the heavy, encrypted block content).
 */
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val fileStorageManager: FileStorageManager,
    private val context: android.content.Context
) : NoteRepository {

    override suspend fun getDailyNote(dateString: String): NoteContent? = withContext(Dispatchers.IO) {
        val metadata = noteDao.getDailyNoteMetadata(dateString)
        if (metadata != null) {
            fileStorageManager.readNoteContent(metadata.filePath)
        } else {
            null
        }
    }

    override suspend fun saveDailyNote(dateString: String, content: NoteContent) = withContext(Dispatchers.IO) {
        val existing = noteDao.getDailyNoteMetadata(dateString)
        val noteId = existing?.noteId ?: UUID.randomUUID().toString()
        val fileName = "daily_$dateString.json"

        fileStorageManager.saveNoteContent(fileName, content)

        // Generate a quick text preview from the blocks to show in the search UI
        val previewText = content.blocks.joinToString(" ") { block ->
            when(block) {
                is com.ben.inly.domain.model.TextBlock -> block.text
                is com.ben.inly.domain.model.HeadingBlock -> block.text
                is com.ben.inly.domain.model.CheckboxBlock -> block.text
                is com.ben.inly.domain.model.BulletedListBlock -> block.text
                is com.ben.inly.domain.model.NumberedListBlock -> block.text
                is com.ben.inly.domain.model.ToggleBlock -> block.text
                is com.ben.inly.domain.model.CodeBlock -> block.code
                else -> {}
            }.toString()
        }.trim().take(120)

        val metadata = NoteMetadataEntity(
            noteId = noteId,
            title = "Daily: $dateString",
            folderId = null,
            isDaily = true,
            dateString = dateString,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            filePath = fileName,
            snippet = previewText,
            isFavorite = existing?.isFavorite ?: false,
            coverImagePath = existing?.coverImagePath,
            trashedAt = existing?.trashedAt
        )
        noteDao.insertOrUpdateMetadata(metadata)
    }

    override fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>> {
        return noteDao.searchDailyNotes(query)
    }

    override fun getAllStandaloneNotes(): Flow<List<NoteMetadataEntity>> {
        return noteDao.getAllStandaloneNotes()
    }

    override fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>> {
        return noteDao.getNotesInFolder(folderId)
    }

    override fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>> {
        return noteDao.getFavoriteNotes()
    }

    override fun getTrashedNotes(): Flow<List<NoteMetadataEntity>> {
        return noteDao.getTrashedNotes()
    }

    override suspend fun getNoteContent(noteId: String): NoteContent? = withContext(Dispatchers.IO) {
        val fileName = "note_$noteId.json"
        fileStorageManager.readNoteContent(fileName)
    }

    override suspend fun saveStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent) = withContext(Dispatchers.IO) {
        val fileName = "note_${metadata.noteId}.json"
        fileStorageManager.saveNoteContent(fileName, content)
        noteDao.insertOrUpdateMetadata(metadata.copy(filePath = fileName))
    }

    override suspend fun deleteNote(noteId: String, filePath: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteMetadata(noteId)
        val file = File(context.filesDir, filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun getNoteById(noteId: String): NoteMetadataEntity? {
        return noteDao.getNoteById(noteId)
    }

    override fun getAllFolders(): Flow<List<FolderEntity>> {
        return folderDao.getAllFolders()
    }

    override suspend fun insertFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.insertFolder(folder)
    }

    override suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folderId)
    }

    override suspend fun restoreNote(noteId: String) = withContext(Dispatchers.IO) {
        noteDao.restoreNote(noteId)
    }

    /**
     * Automatically wipes out notes and their files if they've been sitting in the trash for more than 30 days.
     */
    override suspend fun cleanupOldTrashedNotes() = withContext(Dispatchers.IO) {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis

        val oldNotes = noteDao.getOldTrashedNotes(cutoffTime)
        for (note in oldNotes) {
            deleteNote(note.noteId, note.filePath)
        }
    }

    // Databse tags
    override fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun insertOrUpdateTag(tagId: String, name: String, colorHex: String) = withContext(Dispatchers.IO) {
        tagDao.insertOrUpdateTag(
            TagEntity(
                tagId = tagId,
                name = name,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tagId)
    }
}