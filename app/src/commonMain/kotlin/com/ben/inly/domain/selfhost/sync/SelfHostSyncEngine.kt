package com.ben.inly.domain.selfhost.sync

import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.BlockDao
import com.ben.inly.data.local.room.CategoryDao
import com.ben.inly.data.local.room.CategoryEntity
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteBlockEntity
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.selfhost.media.LocalMediaReader
import com.ben.inly.domain.selfhost.media.MediaReferenceScanner
import com.ben.inly.domain.selfhost.merge.NoteMergeHelper
import com.ben.inly.domain.selfhost.translation.NoteJsonCompiler
import com.ben.inly.domain.selfhost.translation.NoteJsonParser
import com.ben.inly.domain.selfhost.webdav.WebDavConfigurationException
import com.ben.inly.domain.selfhost.webdav.WebDavConflictException
import com.ben.inly.domain.selfhost.webdav.WebDavSyncClient
import com.ben.inly.domain.selfhost.webdav.WebDavSyncPaths
import com.ben.inly.domain.util.MediaStorageHelper
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

sealed class SelfHostSyncResult {
    data class Success(val notesSynced: Int, val conflicts: Int) : SelfHostSyncResult()
    data class Failure(val cause: Throwable) : SelfHostSyncResult()
    data object AlreadyInProgress : SelfHostSyncResult()
    data object NotConfigured : SelfHostSyncResult()
}

class SelfHostSyncEngine(
    private val webDavSyncClient: WebDavSyncClient,
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val categoryDao: CategoryDao,
    private val settingsManager: SettingsManager,
    private val mediaStorageHelper: MediaStorageHelper,
    private val localMediaReader: LocalMediaReader,
    private val noteRepository: NoteRepository
) {

    private enum class ReconcileOutcome { SYNCED, CONFLICT_SKIPPED, UNCHANGED }

    private val mutex = Mutex()
    private val manifestJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val blockJson = Json { ignoreUnknownKeys = true }
    private val collectionJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun hasPendingLocalChanges(): Boolean =
        noteDao.getNotesModifiedSince(settingsManager.getSelfHostLastSyncTimestamp()).isNotEmpty()

    suspend fun runSync(): SelfHostSyncResult {
        SelfHostSyncLog.d("runSync() called")
        if (!mutex.tryLock()) {
            SelfHostSyncLog.d("runSync() skipped, a sync is already in progress")
            return SelfHostSyncResult.AlreadyInProgress
        }
        return try {
            runSyncLocked()
        } finally {
            mutex.unlock()
        }
    }

    suspend fun syncMedia(): SelfHostSyncResult {
        SelfHostSyncLog.d("syncMedia() called")
        if (!mutex.tryLock()) {
            SelfHostSyncLog.d("syncMedia() skipped, a sync is already in progress")
            return SelfHostSyncResult.AlreadyInProgress
        }
        return try {
            syncMediaLocked()
        } finally {
            mutex.unlock()
        }
    }

    suspend fun runBaselineSync(): SelfHostSyncResult {
        SelfHostSyncLog.d("runBaselineSync() called")
        if (!mutex.tryLock()) {
            SelfHostSyncLog.d("runBaselineSync() skipped, a sync is already in progress")
            return SelfHostSyncResult.AlreadyInProgress
        }
        return try {
            val textResult = runSyncLocked()

            if (textResult is SelfHostSyncResult.Success) {
                try {
                    when (val mediaResult = syncMediaLocked()) {
                        is SelfHostSyncResult.Failure -> SelfHostSyncLog.e(
                            "runBaselineSync(): baseline media sync failed, will retry via background worker: ${mediaResult.cause.message}",
                            mediaResult.cause
                        )
                        else -> SelfHostSyncLog.d("runBaselineSync(): baseline media sync finished with $mediaResult")
                    }
                } catch (cause: Exception) {
                    SelfHostSyncLog.e(
                        "runBaselineSync(): baseline media sync threw unexpectedly, will retry via background worker",
                        cause
                    )
                }
            } else {
                SelfHostSyncLog.d("runBaselineSync(): skipping baseline media sync, text sync did not succeed ($textResult)")
            }

            textResult
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun syncMediaLocked(): SelfHostSyncResult {
        return try {
            webDavSyncClient.ensureRemoteLayoutExists()

            val manifest = downloadManifest()
            val remoteMediaFileNames = manifest.entries
                .filter { it.entryType == SelfHostEntryType.MEDIA }
                .map { it.entryId }
                .toSet()

            val referencedFileNames = collectReferencedMediaFileNames()
            val existingLocalFileNames = referencedFileNames.filterTo(mutableSetOf()) { fileExistsLocally(it) }

            SelfHostSyncLog.d(
                "MediaSync: ${referencedFileNames.size} media file(s) referenced locally, " +
                    "${existingLocalFileNames.size} actually present on disk, " +
                    "${remoteMediaFileNames.size} tracked on server"
            )

            val toUpload = existingLocalFileNames - remoteMediaFileNames
            val toDownload = remoteMediaFileNames - existingLocalFileNames
            SelfHostSyncLog.d("MediaSync: ${toUpload.size} to upload, ${toDownload.size} to download")

            var uploadedCount = 0
            var failedCount = 0
            val successfullyUploaded = mutableSetOf<String>()

            for (fileName in toUpload) {
                try {
                    val path = mediaStorageHelper.getAbsoluteMediaPath(fileName)
                    val bytes = localMediaReader.readBytes(path)
                    if (bytes == null) {
                        failedCount++
                        SelfHostSyncLog.e("MediaSync Error: could not read local bytes for $fileName from path=$path")
                        continue
                    }
                    webDavSyncClient.uploadMedia(fileName, bytes)
                    successfullyUploaded.add(fileName)
                    uploadedCount++
                    SelfHostSyncLog.d("MediaSync: uploaded $fileName (${bytes.size} bytes)")
                } catch (cause: Exception) {
                    failedCount++
                    SelfHostSyncLog.e("MediaSync Error: failed to upload $fileName: ${cause.message}", cause)
                }
            }

            var downloadedCount = 0
            for (fileName in toDownload) {
                SelfHostSyncLog.d("MediaSync: Downloading missing local file $fileName")
                try {
                    val bytes = webDavSyncClient.downloadMedia(fileName)
                    if (bytes == null) {
                        SelfHostSyncLog.d("MediaSync: $fileName is listed in the manifest but missing on the server, skipping")
                        continue
                    }
                    val file = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                    downloadedCount++
                    SelfHostSyncLog.d("MediaSync: downloaded $fileName (${bytes.size} bytes)")
                } catch (cause: Exception) {
                    failedCount++
                    SelfHostSyncLog.e("MediaSync Error: failed to download $fileName: ${cause.message}", cause)
                }
            }

            uploadMediaManifestEntries(manifest, remoteMediaFileNames + successfullyUploaded)

            SelfHostSyncLog.d("MediaSync: complete, uploaded=$uploadedCount downloaded=$downloadedCount failed=$failedCount")
            SelfHostSyncResult.Success(notesSynced = uploadedCount + downloadedCount, conflicts = failedCount)
        } catch (cause: WebDavConfigurationException) {
            SelfHostSyncLog.d("MediaSync: not configured (${cause.message})")
            SelfHostSyncResult.NotConfigured
        } catch (cause: Exception) {
            SelfHostSyncLog.e("MediaSync: sync failed with ${cause::class.simpleName}", cause)
            SelfHostSyncResult.Failure(cause)
        }
    }

    private fun fileExistsLocally(fileName: String): Boolean {
        return try {
            File(mediaStorageHelper.getAbsoluteMediaPath(fileName)).exists()
        } catch (cause: Exception) {
            SelfHostSyncLog.e("MediaSync: could not check local existence for $fileName", cause)
            false
        }
    }

    private suspend fun collectReferencedMediaFileNames(): Set<String> {
        val fileNames = mutableSetOf<String>()
        var mediaBlockCount = 0

        for (note in noteDao.getAllNotesForBackup()) {
            note.coverImagePath?.substringAfterLast("/")?.let { fileNames.add(it) }

            val blocks: List<NoteBlock> = blockDao.getAllBlocksForNoteIncludingDeleted(note.noteId)
                .filter { !it.isDeleted }
                .mapNotNull { entity ->
                    try {
                        blockJson.decodeFromString(NoteBlock.serializer(), entity.blockDataJson)
                    } catch (cause: Exception) {
                        SelfHostSyncLog.e("MediaSync: could not decode block ${entity.blockId} for note ${note.noteId}", cause)
                        null
                    }
                }

            mediaBlockCount += blocks.count { it is ImageBlock || it is DocumentBlock || it is VoiceBlock }
            fileNames += MediaReferenceScanner.extractMediaFileNames(blocks)
        }

        SelfHostSyncLog.d(
            "MediaSync: Found $mediaBlockCount local media block(s) to process, " +
                "${fileNames.size} distinct media file(s) referenced"
        )
        return fileNames
    }

    private suspend fun uploadMediaManifestEntries(previousManifest: SelfHostManifest, mediaFileNames: Set<String>) {
        val nonMediaEntries = previousManifest.entries.filter { it.entryType != SelfHostEntryType.MEDIA }
        val mediaEntries = mediaFileNames.map { fileName ->
            SelfHostManifestEntry(
                entryId = fileName,
                entryType = SelfHostEntryType.MEDIA,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
        val newManifest = SelfHostManifest(entries = nonMediaEntries + mediaEntries)

        webDavSyncClient.uploadEncryptedJson(
            WebDavSyncPaths.MANIFEST_FILE,
            manifestJson.encodeToString(SelfHostManifest.serializer(), newManifest)
        )
    }

    private suspend fun runSyncLocked(): SelfHostSyncResult {
        return try {
            webDavSyncClient.ensureRemoteLayoutExists()

            try {
                val dedupedCount = noteRepository.dedupeDuplicateDailyNotes()
                if (dedupedCount > 0) {
                    SelfHostSyncLog.d("TextSync: deduped $dedupedCount duplicate daily note row(s) left over from earlier syncs")
                }
            } catch (cause: Exception) {
                SelfHostSyncLog.e("TextSync: dedupeDuplicateDailyNotes failed, continuing sync anyway", cause)
            }

            val syncStartTimestamp = Clock.System.now().toEpochMilliseconds()
            val lastSyncTimestamp = settingsManager.getSelfHostLastSyncTimestamp()
            val manifest = downloadManifest()
            val isRemoteManifestEmpty = manifest.entries.isEmpty()

            val remoteChangedEntries = manifest.entries
                .filter { it.entryType != SelfHostEntryType.MEDIA && it.updatedAt > lastSyncTimestamp }
                .associateBy { it.entryId }

            val localChangedNotes = if (isRemoteManifestEmpty) {
                noteDao.getAllNotesForBackup().associateBy { it.noteId }
            } else {
                noteDao.getNotesModifiedSince(lastSyncTimestamp).associateBy { it.noteId }
            }
            val candidateIds = remoteChangedEntries.keys + localChangedNotes.keys

            SelfHostSyncLog.d(
                "TextSync: lastSyncTimestamp=$lastSyncTimestamp, manifestEmpty=$isRemoteManifestEmpty, " +
                    "remoteChanged=${remoteChangedEntries.size}, localChanged=${localChangedNotes.size}, " +
                    "candidates=${candidateIds.size}"
            )

            var syncedCount = 0
            var conflictCount = 0

            for (noteId in candidateIds) {
                val outcome = reconcileNote(noteId, remoteEntry = remoteChangedEntries[noteId])
                SelfHostSyncLog.d("TextSync: note=$noteId outcome=$outcome")
                when (outcome) {
                    ReconcileOutcome.SYNCED -> syncedCount++
                    ReconcileOutcome.CONFLICT_SKIPPED -> conflictCount++
                    ReconcileOutcome.UNCHANGED -> Unit
                }
            }

            reconcileFolders()
            reconcileTags()
            reconcileCategories()

            uploadManifest(manifest)
            settingsManager.saveSelfHostLastSyncTimestamp(syncStartTimestamp)

            SelfHostSyncLog.d("TextSync: complete, synced=$syncedCount conflicts=$conflictCount")
            SelfHostSyncResult.Success(notesSynced = syncedCount, conflicts = conflictCount)
        } catch (cause: WebDavConfigurationException) {
            SelfHostSyncLog.d("TextSync: not configured (${cause.message})")
            SelfHostSyncResult.NotConfigured
        } catch (cause: Exception) {
            SelfHostSyncLog.e("TextSync: sync failed with ${cause::class.simpleName}", cause)
            SelfHostSyncResult.Failure(cause)
        }
    }

    // Folders/tags/categories are small, infrequently-changed collections, so unlike notes
    // (per-note files with block-level diffing) each is synced as a single encrypted JSON file
    // holding the full list, merged entity-by-entity via last-write-wins on updatedAt - the same
    // ETag-conditional-PUT conflict handling as pushMergedNote, just at collection granularity.
    private suspend fun reconcileFolders() {
        try {
            val remoteJson = webDavSyncClient.downloadAndDecryptJson(WebDavSyncPaths.FOLDERS_FILE)
            val remoteFolders = remoteJson
                ?.let { collectionJson.decodeFromString(ListSerializer(FolderEntity.serializer()), it) }
                .orEmpty()
            val localFolders = folderDao.getFoldersModifiedSince(0L)

            val merged = LinkedHashMap<String, FolderEntity>()
            localFolders.forEach { merged[it.folderId] = it }
            remoteFolders.forEach { remote ->
                val local = merged[remote.folderId]
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    merged[remote.folderId] = remote
                }
            }
            val mergedList = merged.values.toList()
            mergedList.forEach { folderDao.insertFolder(it) }

            if (mergedList.toSet() != remoteFolders.toSet()) {
                val currentEtag = webDavSyncClient.getResourceInfo(WebDavSyncPaths.FOLDERS_FILE)?.etag
                webDavSyncClient.uploadEncryptedJson(
                    WebDavSyncPaths.FOLDERS_FILE,
                    collectionJson.encodeToString(ListSerializer(FolderEntity.serializer()), mergedList),
                    currentEtag
                )
            }
            SelfHostSyncLog.d("FolderSync: complete, ${mergedList.size} folder(s) reconciled")
        } catch (cause: WebDavConflictException) {
            SelfHostSyncLog.d("FolderSync: remote folders.json changed concurrently, will retry next cycle")
        } catch (cause: Exception) {
            SelfHostSyncLog.e("FolderSync: failed to sync folders: ${cause.message}", cause)
        }
    }

    private suspend fun reconcileTags() {
        try {
            val remoteJson = webDavSyncClient.downloadAndDecryptJson(WebDavSyncPaths.TAGS_FILE)
            val remoteTags = remoteJson
                ?.let { collectionJson.decodeFromString(ListSerializer(TagEntity.serializer()), it) }
                .orEmpty()
            val localTags = tagDao.getTagsModifiedSince(0L)

            val merged = LinkedHashMap<String, TagEntity>()
            localTags.forEach { merged[it.tagId] = it }
            remoteTags.forEach { remote ->
                val local = merged[remote.tagId]
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    merged[remote.tagId] = remote
                }
            }
            val mergedList = merged.values.toList()
            mergedList.forEach { tagDao.insertOrUpdateTag(it) }

            if (mergedList.toSet() != remoteTags.toSet()) {
                val currentEtag = webDavSyncClient.getResourceInfo(WebDavSyncPaths.TAGS_FILE)?.etag
                webDavSyncClient.uploadEncryptedJson(
                    WebDavSyncPaths.TAGS_FILE,
                    collectionJson.encodeToString(ListSerializer(TagEntity.serializer()), mergedList),
                    currentEtag
                )
            }
            SelfHostSyncLog.d("TagSync: complete, ${mergedList.size} tag(s) reconciled")
        } catch (cause: WebDavConflictException) {
            SelfHostSyncLog.d("TagSync: remote tags.json changed concurrently, will retry next cycle")
        } catch (cause: Exception) {
            SelfHostSyncLog.e("TagSync: failed to sync tags: ${cause.message}", cause)
        }
    }

    private suspend fun reconcileCategories() {
        try {
            val remoteJson = webDavSyncClient.downloadAndDecryptJson(WebDavSyncPaths.CATEGORIES_FILE)
            val remoteCategories = remoteJson
                ?.let { collectionJson.decodeFromString(ListSerializer(CategoryEntity.serializer()), it) }
                .orEmpty()
            val localCategories = categoryDao.getCategoriesModifiedSince(0L)

            val merged = LinkedHashMap<String, CategoryEntity>()
            localCategories.forEach { merged[it.categoryId] = it }
            remoteCategories.forEach { remote ->
                val local = merged[remote.categoryId]
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    merged[remote.categoryId] = remote
                }
            }
            val mergedList = merged.values.toList()
            mergedList.forEach { categoryDao.insertOrUpdateCategory(it) }

            if (mergedList.toSet() != remoteCategories.toSet()) {
                val currentEtag = webDavSyncClient.getResourceInfo(WebDavSyncPaths.CATEGORIES_FILE)?.etag
                webDavSyncClient.uploadEncryptedJson(
                    WebDavSyncPaths.CATEGORIES_FILE,
                    collectionJson.encodeToString(ListSerializer(CategoryEntity.serializer()), mergedList),
                    currentEtag
                )
            }
            SelfHostSyncLog.d("CategorySync: complete, ${mergedList.size} categor(y/ies) reconciled")
        } catch (cause: WebDavConflictException) {
            SelfHostSyncLog.d("CategorySync: remote categories.json changed concurrently, will retry next cycle")
        } catch (cause: Exception) {
            SelfHostSyncLog.e("CategorySync: failed to sync categories: ${cause.message}", cause)
        }
    }

    private suspend fun reconcileNote(candidateId: String, remoteEntry: SelfHostManifestEntry?): ReconcileOutcome {
        return try {
            val isDaily = remoteEntry?.entryType == SelfHostEntryType.DAILY
            val remoteDateString = remoteEntry?.dateString

            val localMetadata = if (isDaily && remoteDateString != null) {
                noteDao.getDailyNoteMetadata(remoteDateString) ?: noteDao.getNoteById(candidateId)
            } else {
                noteDao.getNoteById(candidateId)
            }
            val noteId = localMetadata?.noteId ?: candidateId

            val remoteJson = when {
                remoteEntry == null -> null
                isDaily -> {
                    val dateString = remoteDateString ?: localMetadata?.dateString
                    if (dateString != null) webDavSyncClient.downloadDaily(dateString) else null
                }
                else -> webDavSyncClient.downloadNote(noteId)
            }
            val remoteOps = remoteJson?.let { NoteJsonParser.parseJsonToDatabaseOperations(it) }

            val mergedMetadata = pickNewerMetadata(localMetadata, remoteOps?.metadataUpsert)
                ?.copy(noteId = noteId)
                ?: return ReconcileOutcome.UNCHANGED

            val localBlocks = blockDao.getAllBlocksForNoteIncludingDeleted(noteId).filter { block ->
                val belongsToNote = block.noteId == noteId
                if (!belongsToNote) {
                    SelfHostSyncLog.e(
                        "SelfHostSyncEngine: query for note $noteId returned block ${block.blockId} " +
                            "belonging to note ${block.noteId}, discarding it"
                    )
                }
                belongsToNote
            }
            val remoteUpserts = remoteOps?.blockUpserts.orEmpty().map { it.copy(noteId = noteId) }
            val mergedBlocks = NoteMergeHelper.mergeBlocks(
                noteId = noteId,
                localBlocks = localBlocks,
                remoteUpserts = remoteUpserts,
                remoteDeletions = remoteOps?.blockDeletions.orEmpty()
            )

            noteDao.insertOrUpdateMetadata(mergedMetadata.copy(filePath = ""))
            blockDao.insertOrUpdateBlocks(mergedBlocks)

            val refreshedContent = NoteContent(
                blocks = mergedBlocks.mapNotNull { entity ->
                    try {
                        blockJson.decodeFromString(NoteBlock.serializer(), entity.blockDataJson)
                    } catch (cause: Exception) {
                        SelfHostSyncLog.e(
                            "SelfHostSyncEngine: could not decode block ${entity.blockId} while refreshing note cache",
                            cause
                        )
                        null
                    }
                }
            )

            if (mergedMetadata.isDaily) {
                mergedMetadata.dateString?.let { dateString ->
                    noteRepository.refreshDailyNoteCache(dateString, refreshedContent)
                }
            } else {
                noteRepository.refreshNoteContentCache(noteId, refreshedContent)
            }
            noteRepository.refreshProjectionsForNote(mergedMetadata, refreshedContent.blocks)

            pushMergedNote(mergedMetadata, mergedBlocks)
            ReconcileOutcome.SYNCED
        } catch (cause: WebDavConflictException) {
            ReconcileOutcome.CONFLICT_SKIPPED
        }
    }

    private suspend fun pushMergedNote(metadata: NoteMetadataEntity, blocks: List<NoteBlockEntity>) {
        val remotePath = if (metadata.isDaily) {
            WebDavSyncPaths.dailyPath(metadata.dateString ?: metadata.noteId)
        } else {
            WebDavSyncPaths.notePath(metadata.noteId)
        }

        val currentEtag = webDavSyncClient.getResourceInfo(remotePath)?.etag
        val json = NoteJsonCompiler.compileNoteToJson(metadata, blocks)

        if (metadata.isDaily) {
            webDavSyncClient.uploadDaily(metadata.dateString ?: metadata.noteId, json, currentEtag)
        } else {
            webDavSyncClient.uploadNote(metadata.noteId, json, currentEtag)
        }
    }

    private fun pickNewerMetadata(local: NoteMetadataEntity?, remote: NoteMetadataEntity?): NoteMetadataEntity? {
        return when {
            remote == null -> local
            local == null -> remote
            remote.updatedAt >= local.updatedAt -> remote
            else -> local
        }
    }

    private suspend fun downloadManifest(): SelfHostManifest {
        val raw = webDavSyncClient.downloadAndDecryptJson(WebDavSyncPaths.MANIFEST_FILE) ?: return SelfHostManifest()
        return try {
            manifestJson.decodeFromString(SelfHostManifest.serializer(), raw)
        } catch (cause: Exception) {
            SelfHostSyncLog.e("Could not parse downloaded manifest.json, treating as empty", cause)
            SelfHostManifest()
        }
    }

    private suspend fun uploadManifest(previousManifest: SelfHostManifest) {
        val noteEntries = noteDao.getAllNotesForBackup().map { note ->
            SelfHostManifestEntry(
                entryId = note.noteId,
                entryType = if (note.isDaily) SelfHostEntryType.DAILY else SelfHostEntryType.NOTE,
                updatedAt = note.updatedAt,
                dateString = note.dateString.takeIf { note.isDaily }
            )
        }
        val preservedMediaEntries = previousManifest.entries.filter { it.entryType == SelfHostEntryType.MEDIA }
        val newManifest = SelfHostManifest(entries = noteEntries + preservedMediaEntries)

        webDavSyncClient.uploadEncryptedJson(
            WebDavSyncPaths.MANIFEST_FILE,
            manifestJson.encodeToString(SelfHostManifest.serializer(), newManifest)
        )
    }
}
