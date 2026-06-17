package com.ben.inly.domain.repository

import com.ben.inly.data.local.room.BlockDao
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteBlockEntity
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.sync.AutoSyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val blockDao: BlockDao,
    private val noteIndexer: NoteIndexer
) : NoteRepository {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getDailyNote(dateString: String): NoteContent? =
        withContext(Dispatchers.IO) {
            val metadata = noteDao.getDailyNoteMetadata(dateString) ?: return@withContext null

            val entities = blockDao.getAllBlocksForNoteIncludingDeleted(metadata.noteId)

            if (entities.isEmpty()) return@withContext null

            val blocks = entities.mapNotNull { entity ->
                try {
                    jsonFormat.decodeFromString<NoteBlock>(entity.blockDataJson)
                } catch (e: Exception) {
                    null
                }
            }
            NoteContent(blocks = blocks)
        }

    override suspend fun getDailyNoteMetadata(dateString: String): NoteMetadataEntity? =
        withContext(Dispatchers.IO) {
            noteDao.getDailyNoteMetadata(dateString)
        }

    override suspend fun saveDailyNote(dateString: String, content: NoteContent, updatedAt: Long?, remoteMeta: NoteMetadataEntity?) =
        withContext(Dispatchers.IO) {
            val existing = noteDao.getDailyNoteMetadata(dateString)
            val noteId = existing?.noteId ?: remoteMeta?.noteId ?: UUID.randomUUID().toString()

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

            val baseMeta = remoteMeta ?: existing

            val metadata = NoteMetadataEntity(
                noteId = noteId,
                title = "Daily: $dateString",
                folderId = baseMeta?.folderId,
                isDaily = true,
                dateString = dateString,
                createdAt = baseMeta?.createdAt ?: System.currentTimeMillis(),
                updatedAt = updatedAt ?: System.currentTimeMillis(),
                filePath = "",
                snippet = previewText,
                isFavorite = baseMeta?.isFavorite ?: false,
                coverImagePath = baseMeta?.coverImagePath,
                trashedAt = baseMeta?.trashedAt
            )
            noteDao.insertOrUpdateMetadata(metadata)

            val currentEntities = blockDao.getAllBlocksForNoteIncludingDeleted(noteId).associateBy { it.blockId }
            val entitiesToUpsert = mutableListOf<NoteBlockEntity>()

            content.blocks.forEachIndexed { index, block ->
                val existingBlock = currentEntities[block.id]
                if (existingBlock == null || existingBlock.updatedAt != block.updatedAt || existingBlock.displayOrder != index || existingBlock.isDeleted != block.isDeleted) {
                    entitiesToUpsert.add(
                        NoteBlockEntity(
                            blockId = block.id,
                            noteId = noteId,
                            displayOrder = index,
                            blockDataJson = jsonFormat.encodeToString(NoteBlock.serializer(), block),
                            updatedAt = block.updatedAt,
                            isDeleted = block.isDeleted
                        )
                    )
                }
            }

            if (entitiesToUpsert.isNotEmpty()) {
                blockDao.insertOrUpdateBlocks(entitiesToUpsert)
            }

            AutoSyncTrigger.requestSync()
        }

    override fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>> = noteDao.searchDailyNotes(query)

    override fun getAllStandaloneNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getAllStandaloneNotes()

    override fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>> = noteDao.getNotesInFolder(folderId)

    override fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getFavoriteNotes()

    override fun getTrashedNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getTrashedNotes()

    override suspend fun getNoteContent(noteId: String): NoteContent? =
        withContext(Dispatchers.IO) {

            val entities = blockDao.getAllBlocksForNoteIncludingDeleted(noteId)

            if (entities.isEmpty()) return@withContext null

            val blocks = entities.mapNotNull { entity ->
                try {
                    jsonFormat.decodeFromString<NoteBlock>(entity.blockDataJson)
                } catch (e: Exception) {
                    null
                }
            }
            NoteContent(blocks = blocks)
        }

    override suspend fun saveStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent) =
        withContext(Dispatchers.IO) {
            noteDao.insertOrUpdateMetadata(metadata.copy(filePath = ""))

            val currentEntities = blockDao.getAllBlocksForNoteIncludingDeleted(metadata.noteId).associateBy { it.blockId }
            val entitiesToUpsert = mutableListOf<NoteBlockEntity>()

            content.blocks.forEachIndexed { index, block ->
                val existingBlock = currentEntities[block.id]
                if (existingBlock == null || existingBlock.updatedAt != block.updatedAt || existingBlock.displayOrder != index || existingBlock.isDeleted != block.isDeleted) {
                    entitiesToUpsert.add(
                        NoteBlockEntity(
                            blockId = block.id,
                            noteId = metadata.noteId,
                            displayOrder = index,
                            blockDataJson = jsonFormat.encodeToString(NoteBlock.serializer(), block),
                            updatedAt = block.updatedAt,
                            isDeleted = block.isDeleted
                        )
                    )
                }
            }

            if (entitiesToUpsert.isNotEmpty()) {
                blockDao.insertOrUpdateBlocks(entitiesToUpsert)
            }

            AutoSyncTrigger.requestSync()
        }

    override suspend fun deleteNote(noteId: String, filePath: String) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteMetadata(noteId)
            blockDao.deleteAllBlocksForNote(noteId)

            noteIndexer.deleteNoteFromIndex(noteId)

            AutoSyncTrigger.requestSync()
        }
    }

    override suspend fun getNoteById(noteId: String): NoteMetadataEntity? = noteDao.getNoteById(noteId)

    override fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    override suspend fun insertFolder(folder: FolderEntity) =
        withContext(Dispatchers.IO) {
            folderDao.insertFolder(folder)
            AutoSyncTrigger.requestSync()
        }

    override suspend fun deleteFolder(folderId: String) =
        withContext(Dispatchers.IO) {
            folderDao.deleteFolder(folderId)
            AutoSyncTrigger.requestSync()
        }

    override suspend fun restoreNote(noteId: String) =
        withContext(Dispatchers.IO) {
            noteDao.restoreNote(noteId)
            AutoSyncTrigger.requestSync()
        }

    override suspend fun cleanupOldTrashedNotes() = withContext(Dispatchers.IO) {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis
        val oldNotes = noteDao.getOldTrashedNotes(cutoffTime)
        var deletedAny = false
        for (note in oldNotes) {
            deleteNote(note.noteId, note.filePath)
            deletedAny = true
        }

        if (deletedAny) {
            AutoSyncTrigger.requestSync()
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
            AutoSyncTrigger.requestSync()
        }

    override suspend fun deleteTag(tagId: String) =
        withContext(Dispatchers.IO) {
            tagDao.deleteTag(tagId)
            AutoSyncTrigger.requestSync()
        }

    override suspend fun getNotesModifiedSince(timestamp: Long): List<NoteMetadataEntity> {
        return noteDao.getNotesModifiedSince(timestamp)
    }

    override suspend fun indexStandaloneNote(metadata: NoteMetadataEntity, content: NoteContent) =
        withContext(Dispatchers.IO) {
            noteIndexer.indexNote(metadata, content)
        }

    override suspend fun indexDailyNote(dateString: String, content: NoteContent, metadata: NoteMetadataEntity) =
        withContext(Dispatchers.IO) {
            noteIndexer.indexNote(metadata, content)
        }
}