package com.ben.inly.data.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.sync.SyncType
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
                val decryptedContentJson = envelope.contentJson?.let {
                    encryptionManager.decryptPayload(it, syncKey)
                }

                when (envelope.entityType) {
                    SyncType.STANDALONE_NOTE -> {
                        val remoteMeta = json.decodeFromString<NoteMetadataEntity>(decryptedMetaJson)
                        val remoteContent = if (decryptedContentJson != null) {
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
                                    repository.saveStandaloneNote(remoteMeta, remoteContent)
                                }
                            } else {
                                println("Rejected incoming sync for Note ${envelope.entityId} (Local is newer)")
                            }
                        }
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

        return changes
    }
}