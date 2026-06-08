package com.ben.inly.data.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.sync.SyncType
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.sync.NoteMergeHelper
import com.ben.inly.sync.SyncClient
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SyncRepositoryImpl(
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val settingsManager: SettingsManager,
    private val encryptionManager: SyncEncryptionManager,
    private val syncClient: SyncClient
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private fun extractMediaFileNames(content: NoteContent): List<String> {
        return content.blocks.mapNotNull { block ->
            when (block) {
                is ImageBlock -> block.localFilePath
                is DocumentBlock -> block.localFilePath
                is VoiceBlock -> block.localFilePath
                else -> null
            }
        }.filter { it.isNotBlank() }
    }

    private suspend fun downloadMissingMedia(content: NoteContent) {
        extractMediaFileNames(content).forEach { fileName ->
            val file = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))
            if (!file.exists()) {
                syncClient.downloadMedia(fileName, file)
            }
        }
    }

    private suspend fun uploadLocalMedia(content: NoteContent) {
        extractMediaFileNames(content).forEach { fileName ->
            val file = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))
            if (file.exists()) {
                syncClient.uploadMedia(fileName, file)
            }
        }
    }

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
                            if (!envelope.isDeleted) {
                                downloadMissingMedia(remoteContent)

                                if (remoteMeta.coverImagePath != null) {
                                    val file = File(mediaStorageHelper.getAbsoluteMediaPath(remoteMeta.coverImagePath))
                                    if (!file.exists()) syncClient.downloadMedia(remoteMeta.coverImagePath, file)
                                }

                                repository.saveStandaloneNote(remoteMeta, remoteContent)
                                com.ben.inly.domain.util.SyncEventBus.emitSyncCompleted(envelope.entityId)
                            }
                        } else if (envelope.isDeleted && envelope.updatedAt > localMeta.updatedAt) {
                            repository.saveStandaloneNote(
                                remoteMeta.copy(trashedAt = System.currentTimeMillis()),
                                remoteContent
                            )
                            com.ben.inly.domain.util.SyncEventBus.emitSyncCompleted(envelope.entityId)
                        } else if (!envelope.isDeleted) {
                            val localContent = repository.getNoteContent(envelope.entityId)
                            val mergedContent = NoteMergeHelper.mergeNoteContent(
                                localContent = localContent,
                                localUpdatedAt = localMeta.updatedAt,
                                remoteContent = remoteContent,
                                remoteUpdatedAt = envelope.updatedAt
                            )

                            downloadMissingMedia(mergedContent)

                            if (remoteMeta.coverImagePath != null) {
                                val file = File(mediaStorageHelper.getAbsoluteMediaPath(remoteMeta.coverImagePath))
                                if (!file.exists()) syncClient.downloadMedia(remoteMeta.coverImagePath, file)
                            }

                            val contentChanged = mergedContent != localContent
                            val metadataChanged = localMeta.icon != remoteMeta.icon ||
                                    localMeta.coverImagePath != remoteMeta.coverImagePath ||
                                    localMeta.title != remoteMeta.title ||
                                    localMeta.isFavorite != remoteMeta.isFavorite

                            if (contentChanged || metadataChanged) {
                                val resolvedUpdatedAt = maxOf(localMeta.updatedAt, envelope.updatedAt)

                                val winningMeta = if (envelope.updatedAt >= localMeta.updatedAt) {
                                    remoteMeta.copy(updatedAt = resolvedUpdatedAt)
                                } else {
                                    localMeta.copy(updatedAt = resolvedUpdatedAt)
                                }

                                repository.saveStandaloneNote(
                                    winningMeta,
                                    mergedContent
                                )

                                com.ben.inly.domain.util.SyncEventBus.emitSyncCompleted(envelope.entityId)
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
                            downloadMissingMedia(remoteContent)

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

                            downloadMissingMedia(mergedContent)

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

            uploadLocalMedia(content)

            if (!meta.isDaily && meta.coverImagePath != null) {
                val file = File(mediaStorageHelper.getAbsoluteMediaPath(meta.coverImagePath))
                if (file.exists()) {
                    syncClient.uploadMedia(meta.coverImagePath, file)
                }
            }

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