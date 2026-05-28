package com.ben.inly.domain.repository

import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val fileStorageManager: FileStorageManager
) : NoteRepository {

    override suspend fun getDailyNote(dateString: String): NoteContent? =
        withContext(Dispatchers.IO) {
            val metadata = noteDao.getDailyNoteMetadata(dateString)
            if (metadata != null) {
                fileStorageManager.readNoteContent(metadata.filePath)
            } else {
                null
            }
        }

    override suspend fun getDailyNoteMetadata(dateString: String): NoteMetadataEntity? =
        withContext(Dispatchers.IO) {
            noteDao.getDailyNoteMetadata(dateString)
        }

    override suspend fun saveDailyNote(dateString: String, content: NoteContent, updatedAt: Long?, remoteMeta: NoteMetadataEntity?) =
        withContext(Dispatchers.IO) {
            val existing = noteDao.getDailyNoteMetadata(dateString)
            val noteId = existing?.noteId ?: remoteMeta?.noteId ?: UUID.randomUUID().toString()
            val fileName = "daily_$dateString.json"

            fileStorageManager.saveNoteContent(fileName, content)

            val previewText = content.blocks.joinToString(" ") { block ->
                when (block) {
                    is TextBlock -> block.text
                    is HeadingBlock -> block.text
                    is CheckboxBlock -> block.text
                    is BulletedListBlock -> block.text
                    is NumberedListBlock -> block.text
                    is ToggleBlock -> block.text
                    is CodeBlock -> block.code
                    else -> ""
                }
            }.trim().take(120)

            // THE FIX: Unify local and remote metadata!
            val baseMeta = remoteMeta ?: existing

            val metadata = NoteMetadataEntity(
                noteId = noteId,
                title = "Daily: $dateString",
                folderId = baseMeta?.folderId,
                isDaily = true,
                dateString = dateString,
                createdAt = baseMeta?.createdAt ?: System.currentTimeMillis(),
                updatedAt = updatedAt ?: System.currentTimeMillis(),
                filePath = fileName,
                snippet = previewText,
                isFavorite = baseMeta?.isFavorite ?: false,
                coverImagePath = baseMeta?.coverImagePath,
                trashedAt = baseMeta?.trashedAt
            )
            noteDao.insertOrUpdateMetadata(metadata)
        }

    override fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>> = noteDao.searchDailyNotes(query)
    override fun getAllStandaloneNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getAllStandaloneNotes()
    override fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>> = noteDao.getNotesInFolder(folderId)
    override fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getFavoriteNotes()
    override fun getTrashedNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getTrashedNotes()

    override suspend fun getNoteContent(noteId: String): NoteContent? =
        withContext(Dispatchers.IO) {
            fileStorageManager.readNoteContent("note_$noteId.json")
        }

    override suspend fun saveStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent) =
        withContext(Dispatchers.IO) {
            val fileName = "note_${metadata.noteId}.json"
            fileStorageManager.saveNoteContent(fileName, content)
            noteDao.insertOrUpdateMetadata(metadata.copy(filePath = fileName))
        }

    override suspend fun deleteNote(noteId: String, filePath: String) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteMetadata(noteId)
            fileStorageManager.deleteNoteContent(filePath)
        }
    }

    override suspend fun getNoteById(noteId: String): NoteMetadataEntity? = noteDao.getNoteById(noteId)
    override fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()
    override suspend fun insertFolder(folder: FolderEntity) =
        withContext(Dispatchers.IO) { folderDao.insertFolder(folder) }
    override suspend fun deleteFolder(folderId: String) =
        withContext(Dispatchers.IO) { folderDao.deleteFolder(folderId) }
    override suspend fun restoreNote(noteId: String) =
        withContext(Dispatchers.IO) { noteDao.restoreNote(noteId) }

    override suspend fun cleanupOldTrashedNotes() = withContext(Dispatchers.IO) {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis
        val oldNotes = noteDao.getOldTrashedNotes(cutoffTime)
        for (note in oldNotes) {
            deleteNote(note.noteId, note.filePath)
        }
    }

    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    override suspend fun insertOrUpdateTag(tagId: String, name: String, colorHex: String) =
        withContext(Dispatchers.IO) {
            tagDao.insertOrUpdateTag(
                TagEntity(
                    tagId = tagId,
                    name = name,
                    colorHex = colorHex,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

    override suspend fun deleteTag(tagId: String) =
        withContext(Dispatchers.IO) { tagDao.deleteTag(tagId) }

    override suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity> {
        return noteDao.getNotesModifiedSince(timestamp)
    }
}
