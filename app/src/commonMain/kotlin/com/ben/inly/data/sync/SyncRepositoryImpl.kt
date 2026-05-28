package com.ben.inly.data.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.sync.SyncType
import com.ben.inly.sync.NoteMergeHelper
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SyncRepositoryImpl(
    private val repository: NoteRepository,
    private val fileStorageManager: FileStorageManager,
    private val settingsManager: SettingsManager,
    private val encryptionManager: SyncEncryptionManager
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }


    override suspend fun applyRemoteChanges(changes: List<SyncEnvelope>) {
        val syncKey = settingsManager.getSyncEncryptionKey()

        changes.forEach { envelope ->
            try {
                val decryptedMetaJson = encryptionManager.decryptPayload(envelope.metadataJson, syncKey)
                val decryptedContentJson = if (envelope.contentJson.isNotEmpty()) {
                    encryptionManager.decryptPayload(envelope.contentJson, syncKey)
                } else null

                when (envelope.entityType) {
                    SyncType.STANDALONE_NOTE -> {
                        val remoteMeta = json.decodeFromString<NoteMetadataEntity>(decryptedMetaJson)
                        val remoteContent = if (decryptedContentJson != null && decryptedContentJson.isNotEmpty()) {
                            json.decodeFromString<NoteContent>(decryptedContentJson)
                        } else {
                            NoteContent(blocks = emptyList())
                        }

                        val localMeta = repository.getNoteById(envelope.entityId)

                        if (localMeta == null) {
                            if (!envelope.isDeleted) repository.saveStandaloneNote(remoteMeta, remoteContent)
                        } else if (envelope.isDeleted && envelope.updatedAt > localMeta.updatedAt) {
                            repository.saveStandaloneNote(
                                remoteMeta.copy(trashedAt = System.currentTimeMillis()),
                                remoteContent
                            )
                        } else if (!envelope.isDeleted) {
                            val localContent = repository.getNoteContent(envelope.entityId)
                            val mergedContent = NoteMergeHelper.mergeNoteContent(
                                localContent = localContent,
                                localUpdatedAt = localMeta.updatedAt,
                                remoteContent = remoteContent,
                                remoteUpdatedAt = envelope.updatedAt
                            )
                            if (mergedContent != localContent) {
                                val resolvedUpdatedAt = maxOf(localMeta.updatedAt, envelope.updatedAt)
                                repository.saveStandaloneNote(
                                    remoteMeta.copy(updatedAt = resolvedUpdatedAt),
                                    mergedContent
                                )
                            }
                        }
                    }

                    SyncType.DAILY_NOTE -> {
                        val remoteMeta = json.decodeFromString<NoteMetadataEntity>(decryptedMetaJson)
                        val remoteContent = if (decryptedContentJson != null && decryptedContentJson.isNotEmpty()) {
                            json.decodeFromString<NoteContent>(decryptedContentJson)
                        } else {
                            NoteContent(blocks = emptyList())
                        }

                        val dateString = envelope.entityId
                        val localMeta = repository.getDailyNoteMetadata(dateString)

                        if (localMeta == null) {
                            repository.saveDailyNote(dateString, remoteContent, envelope.updatedAt, remoteMeta)

                            com.ben.inly.domain.util.SyncEventBus.emitSyncCompleted(dateString)
                        } else {
                            val localContent = repository.getDailyNote(dateString)
                            val mergedContent = NoteMergeHelper.mergeNoteContent(
                                localContent = localContent,
                                localUpdatedAt = localMeta.updatedAt,
                                remoteContent = remoteContent,
                                remoteUpdatedAt = envelope.updatedAt
                            )

                            val contentChanged = mergedContent != localContent
                            val metadataChanged = localMeta.isFavorite != remoteMeta.isFavorite || localMeta.coverImagePath != remoteMeta.coverImagePath

                            if (contentChanged || metadataChanged) {
                                val resolvedUpdatedAt = maxOf(localMeta.updatedAt, envelope.updatedAt)

                                val mergedMeta = localMeta.copy(
                                    isFavorite = localMeta.isFavorite || remoteMeta.isFavorite,
                                    coverImagePath = remoteMeta.coverImagePath ?: localMeta.coverImagePath
                                )

                                repository.saveDailyNote(dateString, mergedContent, resolvedUpdatedAt, mergedMeta)

                                com.ben.inly.domain.util.SyncEventBus.emitSyncCompleted(dateString)
                            }
                        }
                    }

                    SyncType.TAG -> {
                        val remoteTag = json.decodeFromString<TagEntity>(decryptedMetaJson)
                        repository.insertOrUpdateTag(remoteTag.tagId, remoteTag.name, remoteTag.colorHex)
                    }

                    SyncType.FOLDER -> {
                        val remoteFolder = json.decodeFromString<FolderEntity>(decryptedMetaJson)
                        repository.insertFolder(remoteFolder)
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                println("Failed to apply remote change for ${envelope.entityId}: ${e.message}")
            }
        }
    }

    override suspend fun collectLocalChanges(): List<SyncEnvelope> {
        val lastSyncTime = settingsManager.getLastSyncTimestamp()
        val syncKey = settingsManager.getSyncEncryptionKey()
        val changes = mutableListOf<SyncEnvelope>()
        val modifiedNotes = repository.getNotesModifiedSince(lastSyncTime)

        modifiedNotes.forEach { meta ->
            val content = if (meta.isDaily && meta.dateString != null) {
                repository.getDailyNote(meta.dateString)
            } else {
                repository.getNoteContent(meta.noteId)
            } ?: NoteContent(blocks = emptyList())

            val encryptedMeta = encryptionManager.encryptPayload(json.encodeToString(meta), syncKey)
            val encryptedContent = encryptionManager.encryptPayload(json.encodeToString(content), syncKey)

            val type = if (meta.isDaily) SyncType.DAILY_NOTE else SyncType.STANDALONE_NOTE
            val eId = if (meta.isDaily && meta.dateString != null) meta.dateString else meta.noteId

            changes.add(
                SyncEnvelope(
                    entityId = eId,
                    entityType = type,
                    metadataJson = encryptedMeta,
                    contentJson = encryptedContent,
                    updatedAt = meta.updatedAt,
                    isDeleted = meta.trashedAt != null
                )
            )
        }

        val allTags = repository.getAllTags().first()
        val modifiedTags = allTags.filter { it.createdAt > lastSyncTime }

        modifiedTags.forEach { tag ->
            val encryptedTag = encryptionManager.encryptPayload(json.encodeToString(tag), syncKey)

            changes.add(
                SyncEnvelope(
                    entityId = tag.tagId,
                    entityType = SyncType.TAG,
                    metadataJson = encryptedTag,
                    contentJson = "",
                    updatedAt = tag.createdAt,
                    isDeleted = false
                )
            )
        }

        val allFolders = repository.getAllFolders().first()
        val modifiedFolders = allFolders.filter { it.createdAt > lastSyncTime }

        modifiedFolders.forEach { folder ->
            val encryptedFolder = encryptionManager.encryptPayload(json.encodeToString(folder), syncKey)

            changes.add(
                SyncEnvelope(
                    entityId = folder.folderId,
                    entityType = SyncType.FOLDER,
                    metadataJson = encryptedFolder,
                    contentJson = "",
                    updatedAt = folder.createdAt,
                    isDeleted = folder.isDeleted
                )
            )
        }
        return changes
    }
}