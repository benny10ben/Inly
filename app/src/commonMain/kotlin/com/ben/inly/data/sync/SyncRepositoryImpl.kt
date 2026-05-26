package com.ben.inly.data.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.prefs.SettingsManager
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

                // THE FIX: Only try to decrypt if the contentJson actually has encrypted data
                val decryptedContentJson = if (envelope.contentJson.isNotEmpty()) {
                    encryptionManager.decryptPayload(envelope.contentJson, syncKey)
                } else {
                    null
                }

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
                                repository.saveStandaloneNote(remoteMeta, remoteContent)
                            }
                        } else {
                            if (envelope.updatedAt > localMeta.updatedAt) {
                                if (envelope.isDeleted) {
                                    repository.saveStandaloneNote(
                                        remoteMeta.copy(trashedAt = System.currentTimeMillis()),
                                        remoteContent
                                    )
                                } else {
                                    // THE FIX: Fetch the local content and merge it before saving!
                                    val localContent = repository.getNoteContent(envelope.entityId)
                                    val smartMergedContent = NoteMergeHelper.mergeNoteContent(localContent, remoteContent)

                                    repository.saveStandaloneNote(remoteMeta, smartMergedContent)
                                }
                            } else {
                                println("Rejected incoming sync for Note ${envelope.entityId} (Local is newer)")
                            }
                        }
                    }
                    SyncType.TAG -> {
                        // NEW CODE: Handle incoming tags
                        val remoteTag = json.decodeFromString<TagEntity>(decryptedMetaJson)
                        repository.insertOrUpdateTag(remoteTag.tagId, remoteTag.name, remoteTag.colorHex)
                    }
                    SyncType.DAILY_NOTE -> {
                        // Daily note implementation
                    }
                    else -> {
                        // Folder implementation
                    }
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

        // 1. Collect Notes (Your existing code)
        val allNotes = repository.getAllStandaloneNotes().first()
        val modifiedNotes = allNotes.filter { it.updatedAt > lastSyncTime }

        modifiedNotes.forEach { meta ->
            val content = repository.getNoteContent(meta.noteId) ?: NoteContent(blocks = emptyList())
            val encryptedMeta = encryptionManager.encryptPayload(json.encodeToString(meta), syncKey)
            val encryptedContent = encryptionManager.encryptPayload(json.encodeToString(content), syncKey)

            changes.add(
                SyncEnvelope(
                    entityId = meta.noteId,
                    entityType = SyncType.STANDALONE_NOTE,
                    metadataJson = encryptedMeta,
                    contentJson = encryptedContent,
                    updatedAt = meta.updatedAt,
                    isDeleted = meta.trashedAt != null
                )
            )
        }

        // 2. Collect Tags (NEW CODE)
        val allTags = repository.getAllTags().first() // Make sure you have this function in your repository
        val modifiedTags = allTags.filter { it.createdAt > lastSyncTime } // Assuming createdAt acts as updatedAt for tags

        modifiedTags.forEach { tag ->
            val encryptedTag = encryptionManager.encryptPayload(json.encodeToString(tag), syncKey)

            changes.add(
                SyncEnvelope(
                    entityId = tag.tagId,
                    entityType = SyncType.TAG,
                    metadataJson = encryptedTag, // We put the tag data here
                    contentJson = "", // Tags don't have separate content files
                    updatedAt = tag.createdAt,
                    isDeleted = false // Tags usually aren't soft-deleted in this architecture
                )
            )
        }

        return changes
    }
}