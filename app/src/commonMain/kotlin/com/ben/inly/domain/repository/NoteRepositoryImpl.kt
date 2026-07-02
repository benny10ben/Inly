package com.ben.inly.domain.repository

import com.ben.inly.data.local.room.BlockDao
import com.ben.inly.data.local.room.BookmarkBlockDao
import com.ben.inly.data.local.room.BookmarkBlockEntity
import com.ben.inly.data.local.room.CalendarTaskEntity
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteBlockEntity
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.data.local.room.CalendarTaskDao
import com.ben.inly.data.local.room.DocumentBlockDao
import com.ben.inly.data.local.room.DocumentBlockEntity
import com.ben.inly.data.local.room.ImageBlockDao
import com.ben.inly.data.local.room.ImageBlockEntity
import com.ben.inly.data.local.room.TaskSource
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.NoteSearchResult
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.RowContainerBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.sync.AutoSyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// NoteRepositoryImpl is the single point through which all note data flows.
// Every ViewModel reads from and writes to this class — nothing talks to Room directly.
//
// ARCHITECTURE: Why a cache exists here
//
// The editor holds its own in-memory MutableStateFlow<List<NoteBlock>> for performance.
// Writing to Room on every keystroke would trigger Room Flow emissions that fight the
// editor mid-typing, causing cursor jumps and UI flicker. So the editor writes to its
// own in-memory state instantly, then flushes to Room on a 1-second debounce.
//
// This creates a problem: other screens (e.g. RemindersScreen) write directly to Room
// via saveNote/saveDailyNote, but the editor's in-memory state doesn't know about it.
// On navigation back, the editor was showing stale data.
//
// The fix: this repository maintains two MutableStateFlow caches — one for regular notes,
// one for daily notes. Every write updates the cache synchronously before touching Room.
// Every read checks the cache first. ViewModels that need to stay in sync (DailyEditorViewModel,
// NoteEditorViewModel) observe these caches via observeDailyNote / observeNoteContent.
// When any writer calls saveNote or saveDailyNote, the relevant observer fires automatically —
// on mobile, on desktop, regardless of how screens were navigated or dismissed.
//
// The editor guards against its own writes bouncing back by checking autosaveJob?.isActive.
// If the editor itself triggered the write, the cache emission is ignored. If another
// ViewModel (RemindersViewModel, SyncRepositoryImpl, etc.) triggered it, the emission
// goes through and updates the editor's blocks.
//
// CRASH SAFETY
//
// The cache lives in memory and is gone on crash. That's fine — it's purely a mirror of
// Room. On next launch, getDailyNote and getNoteContent read from Room and repopulate
// the cache on first access. Data loss on crash is at most 1 second of typing (the
// autosave debounce window). BaseEditorViewModel.onCleared() fires a final save on
// normal process death, so real-world data loss is essentially zero.

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val blockDao: BlockDao,
    private val noteIndexer: NoteIndexer,
    private val calendarTaskDao: CalendarTaskDao,
    private val imageBlockDao: ImageBlockDao,
    private val documentBlockDao: DocumentBlockDao,
    private val bookmarkBlockDao: BookmarkBlockDao
) : NoteRepository {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // In-memory cache for regular notes keyed by noteId.
    // Updated on every saveNote call, checked before every getNoteContent DB read.
    private val noteContentCache = MutableStateFlow<Map<String, NoteContent>>(emptyMap())

    // In-memory cache for daily notes keyed by dateString (e.g. "2025-06-01").
    // global_pinned is intentionally excluded from this cache to avoid pinned block
    // emissions triggering unnecessary editor refreshes.
    private val dailyNoteCache = MutableStateFlow<Map<String, NoteContent>>(emptyMap())

    // Exposes a Flow that emits whenever the cache entry for this noteId changes.
    // NoteEditorViewModel subscribes to this in its init block to stay in sync
    // with external writes (e.g. RemindersViewModel toggling a checkbox).
    override fun observeNoteContent(noteId: String): Flow<NoteContent?> =
        noteContentCache.map { it[noteId] }

    // Exposes a Flow that emits whenever the cache entry for this dateString changes.
    // DailyEditorViewModel subscribes to this in its init block for the same reason.
    override fun observeDailyNote(dateString: String): Flow<NoteContent?> =
        dailyNoteCache.map { it[dateString] }

    override suspend fun getDailyNote(dateString: String): NoteContent? =
        withContext(Dispatchers.IO) {
            // Return from cache if available — avoids a DB round-trip on repeat reads
            // and ensures callers always see the most recently written content.
            dailyNoteCache.value[dateString]?.let { return@withContext it }

            val metadata = noteDao.getDailyNoteMetadata(dateString) ?: return@withContext null
            val entities = blockDao.getAllBlocksForNoteIncludingDeleted(metadata.noteId)
            if (entities.isEmpty()) return@withContext null

            val blocks = entities.mapNotNull { entity ->
                try { jsonFormat.decodeFromString<NoteBlock>(entity.blockDataJson) }
                catch (e: Exception) { null }
            }
            val content = NoteContent(blocks = blocks)

            // Populate the cache so subsequent reads and observers get this value.
            dailyNoteCache.update { it + (dateString to content) }
            content
        }

    override suspend fun getDailyNoteMetadata(dateString: String): NoteMetadataEntity? =
        withContext(Dispatchers.IO) {
            noteDao.getDailyNoteMetadata(dateString)
        }

    override suspend fun saveDailyNote(dateString: String, content: NoteContent, updatedAt: Long?, remoteMeta: NoteMetadataEntity?) =
        withContext(Dispatchers.IO) {

            // Update the cache synchronously before the DB write.
            // This means any observer (DailyEditorViewModel) sees the new content
            // immediately, without waiting for Room to finish writing.
            // global_pinned is excluded because pinned blocks are merged into daily
            // content by DailyEditorViewModel and don't need their own cache entry.
            if (dateString != "global_pinned") {
                dailyNoteCache.update { it + (dateString to content) }
            }

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

            // Only upsert blocks that have actually changed — avoids unnecessary
            // DB writes on every autosave when most blocks haven't been touched.
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

            // Sync projection tables — these are flat Room tables that allow
            // RemindersScreen, ImagesScreen, DocumentsScreen, and BookmarksScreen
            // to query their content without scanning every note's block list.
            if (dateString != "global_pinned") {
                syncCalendarTasks(
                    noteId = dateString,
                    blocks = content.blocks,
                    sourceType = TaskSource.DAILY,
                    dailyDateString = dateString
                )
                syncImageBlocks(
                    noteId        = noteId,
                    blocks        = content.blocks,
                    sourceType    = TaskSource.DAILY,
                    noteCreatedAt = metadata.createdAt
                )
                syncDocumentBlocks(
                    noteId        = noteId,
                    blocks        = content.blocks,
                    sourceType    = TaskSource.DAILY,
                    noteCreatedAt = metadata.createdAt
                )
                syncBookmarkBlocks(
                    noteId        = noteId,
                    blocks        = content.blocks,
                    sourceType    = TaskSource.DAILY,
                    noteUpdatedAt = updatedAt ?: System.currentTimeMillis()
                )
            }
        }

    override fun searchDailyNotes(query: String): Flow<List<NoteMetadataEntity>> = noteDao.searchDailyNotes(query)

    // Cross-note search. Runs the two DAO queries added for this feature:
    // 1) a title/snippet LIKE match (cheap, covers most everyday searches), and
    // 2) a content LIKE match over the raw block JSON, which only tells us *which* notes
    //    matched - so for those we still have to decode blocks to find the actual matching
    //    text to show/highlight. That decode only happens for notes not already found by (1),
    //    keeping the expensive part proportional to result size, not corpus size.
    override suspend fun searchNotes(query: String): List<NoteSearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

            val titleOrSnippetMatches = noteDao.searchNotesByTitleOrSnippet(query).first()
            val matchedIds = titleOrSnippetMatches.mapTo(mutableSetOf()) { it.noteId }

            val contentMatchIds = blockDao.findNoteIdsMatchingContent(query)
                .filterNot { it in matchedIds }

            val contentMatches = if (contentMatchIds.isEmpty()) {
                emptyList()
            } else {
                noteDao.getNotesByIds(contentMatchIds).mapNotNull { metadata ->
                    val matchedText = findMatchingBlockText(metadata.noteId, query) ?: return@mapNotNull null
                    NoteSearchResult(note = metadata, matchedText = matchedText)
                }
            }

            val metadataResults = titleOrSnippetMatches.map { metadata ->
                NoteSearchResult(
                    note = metadata,
                    matchedText = metadata.snippet.ifBlank { metadata.title }
                )
            }

            (metadataResults + contentMatches).sortedByDescending { it.note.updatedAt }
        }

    // Decodes a single note's blocks and returns the flattened text of the first
    // non-deleted block whose text contains the query (case-insensitive).
    private suspend fun findMatchingBlockText(noteId: String, query: String): String? {
        val entities = blockDao.getAllBlocksForNoteIncludingDeleted(noteId)
        val lowerQuery = query.lowercase()
        for (entity in entities) {
            val block = try {
                jsonFormat.decodeFromString<NoteBlock>(entity.blockDataJson)
            } catch (e: Exception) {
                null
            } ?: continue
            if (block.isDeleted) continue
            val text = flattenBlockText(block) ?: continue
            if (text.lowercase().contains(lowerQuery)) return text
        }
        return null
    }

    // Reduces any block type down to its searchable plain text, mirroring the
    // per-block-type switch already used for the (unused) filter in HomeViewModel.notes.
    private fun flattenBlockText(block: NoteBlock): String? = when (block) {
        is TextBlock -> block.text
        is HeadingBlock -> block.text
        is CheckboxBlock -> block.text
        is BulletedListBlock -> block.text
        is NumberedListBlock -> block.text
        is ToggleBlock -> block.text
        is CodeBlock -> block.code
        is BookmarkBlock -> block.title?.takeIf { it.isNotBlank() } ?: block.url
        is DocumentBlock -> block.fileName
        else -> null
    }.let { text -> text?.takeIf { it.isNotBlank() } }

    override fun getAllNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getAllNotes()

    override fun getNotesInFolder(folderId: String): Flow<List<NoteMetadataEntity>> = noteDao.getNotesInFolder(folderId)

    override fun getFavoriteNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getFavoriteNotes()

    override fun getTrashedNotes(): Flow<List<NoteMetadataEntity>> = noteDao.getTrashedNotes()

    override suspend fun getNoteContent(noteId: String): NoteContent? =
        withContext(Dispatchers.IO) {
            // Return from cache if available — same reasoning as getDailyNote.
            noteContentCache.value[noteId]?.let { return@withContext it }

            val entities = blockDao.getAllBlocksForNoteIncludingDeleted(noteId)
            if (entities.isEmpty()) return@withContext null

            val blocks = entities.mapNotNull { entity ->
                try { jsonFormat.decodeFromString<NoteBlock>(entity.blockDataJson) }
                catch (e: Exception) { null }
            }
            val content = NoteContent(blocks = blocks)

            // Populate cache on first DB read so future reads and observers are live.
            noteContentCache.update { it + (noteId to content) }
            content
        }

    override suspend fun saveNote(metadata: NoteMetadataEntity, content: NoteContent) =
        withContext(Dispatchers.IO) {

            // Update the cache synchronously before the DB write.
            // Any observer (NoteEditorViewModel) immediately sees the new blocks.
            noteContentCache.update { it + (metadata.noteId to content) }

            noteDao.insertOrUpdateMetadata(metadata.copy(filePath = ""))

            // Same delta-write strategy as saveDailyNote — only changed blocks hit DB.
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
            syncCalendarTasks(
                noteId = metadata.noteId,
                blocks = content.blocks,
                sourceType = TaskSource.NOTE,
                dailyDateString = null
            )
            syncImageBlocks(
                noteId       = metadata.noteId,
                blocks       = content.blocks,
                sourceType   = TaskSource.NOTE,
                noteCreatedAt = metadata.createdAt
            )
            syncDocumentBlocks(
                noteId        = metadata.noteId,
                blocks        = content.blocks,
                sourceType    = TaskSource.NOTE,
                noteCreatedAt = metadata.createdAt
            )
            syncBookmarkBlocks(
                noteId       = metadata.noteId,
                blocks       = content.blocks,
                sourceType   = TaskSource.NOTE,
                noteUpdatedAt = metadata.updatedAt
            )
        }

    override suspend fun deleteNote(noteId: String, filePath: String) {
        withContext(Dispatchers.IO) {
            // Evict from cache so no observer gets a stale emission after deletion,
            // and so a future note created with the same ID starts with a clean slate.
            noteContentCache.update { it - noteId }
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

    override suspend fun indexNote(metadata: NoteMetadataEntity, content: NoteContent) =
        withContext(Dispatchers.IO) {
            try {
                noteIndexer.indexNote(metadata, content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    override suspend fun indexDailyNote(dateString: String, content: NoteContent, metadata: NoteMetadataEntity) =
        withContext(Dispatchers.IO) {
            try {
                noteIndexer.indexNote(metadata, content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    // Walks the block tree recursively to find all CheckboxBlocks including those
    // nested inside RowContainerBlock columns.
    private fun extractActiveCheckboxes(blocks: List<NoteBlock>): List<CheckboxBlock> {
        val result = mutableListOf<CheckboxBlock>()
        for (block in blocks) {
            if (block.isDeleted) continue
            when (block) {
                is CheckboxBlock -> result.add(block)
                is RowContainerBlock -> {
                    block.columns.forEach { column ->
                        result.addAll(extractActiveCheckboxes(column.blocks))
                    }
                }
                else -> {}
            }
        }
        return result
    }

    // Rebuilds the CalendarTaskEntity projection table for a given note on every save.
    // RemindersScreen and the calendar strip both read from this table, so they always
    // reflect the latest checkbox state without scanning raw block JSON.
    private suspend fun syncCalendarTasks(
        noteId: String,
        blocks: List<NoteBlock>,
        sourceType: TaskSource,
        dailyDateString: String? = null
    ) {
        calendarTaskDao.deleteTasksByNoteId(noteId)
        val allCheckboxes = extractActiveCheckboxes(blocks)
        val tasksToInsert = allCheckboxes.map { block ->
            val targetDate = when (sourceType) {
                TaskSource.DAILY -> dailyDateString ?: ""
                TaskSource.NOTE -> {
                    if (block.reminderTimestamp != null) {
                        val instant = Instant.fromEpochMilliseconds(block.reminderTimestamp)
                        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        val monthStr = dt.monthNumber.toString().padStart(2, '0')
                        val dayStr = dt.dayOfMonth.toString().padStart(2, '0')
                        "${dt.year}-${monthStr}-${dayStr}"
                    } else ""
                }
            }

            CalendarTaskEntity(
                blockId = block.id,
                noteId = noteId,
                text = block.text,
                isChecked = block.isChecked,
                targetDate = targetDate,
                reminderTimestamp = block.reminderTimestamp,
                sourceType = sourceType
            )
        }

        if (tasksToInsert.isNotEmpty()) {
            calendarTaskDao.upsertTasks(tasksToInsert)
        }
    }

    override fun getCalendarTasksForMonth(yearMonth: String): Flow<List<CalendarTaskEntity>> {
        return calendarTaskDao.getTasksForMonth(yearMonth)
    }

    override fun getAllTasksFlow(): Flow<List<CalendarTaskEntity>> {
        return calendarTaskDao.getAllTasksFlow()
    }

    override fun getIncompleteTasksCount(): Flow<Int> = noteDao.getIncompleteTasksCount()

    // Rebuilds the ImageBlockEntity projection table for a given note on every save.
    // ImagesScreen reads from this table via getAllImagesFlow() — a Room Flow that
    // emits automatically whenever this table changes.
    private suspend fun syncImageBlocks(
        noteId: String,
        blocks: List<NoteBlock>,
        sourceType: TaskSource,
        noteCreatedAt: Long
    ) {
        imageBlockDao.deleteByNoteId(noteId)

        val images = blocks
            .filterIsInstance<ImageBlock>()
            .filter { !it.isDeleted && it.localFilePath != null }
            .map { block ->
                ImageBlockEntity(
                    blockId = block.id,
                    noteId = noteId,
                    localFilePath = block.localFilePath!!,
                    noteCreatedAt = noteCreatedAt,
                    sourceType = sourceType
                )
            }

        if (images.isNotEmpty()) {
            imageBlockDao.upsertImages(images)
        }
    }

    override fun getAllImagesFlow(): Flow<List<ImageBlockEntity>> =
        imageBlockDao.getAllImagesFlow()

    // Rebuilds the DocumentBlockEntity projection table for a given note on every save.
    // DocumentsScreen reads from this via getAllDocumentsFlow().
    private suspend fun syncDocumentBlocks(
        noteId: String,
        blocks: List<NoteBlock>,
        sourceType: TaskSource,
        noteCreatedAt: Long
    ) {
        documentBlockDao.deleteByNoteId(noteId)

        val documents = blocks
            .filterIsInstance<DocumentBlock>()
            .filter { !it.isDeleted && it.localFilePath != null }
            .map { block ->
                DocumentBlockEntity(
                    blockId = block.id,
                    noteId = noteId,
                    localFilePath = block.localFilePath!!,
                    fileName = block.fileName,
                    mimeType = block.mimeType,
                    fileSizeString = block.fileSizeString,
                    noteCreatedAt = noteCreatedAt,
                    sourceType = sourceType
                )
            }

        if (documents.isNotEmpty()) {
            documentBlockDao.upsertDocuments(documents)
        }
    }

    override fun getAllDocumentsFlow(): Flow<List<DocumentBlockEntity>> =
        documentBlockDao.getAllDocumentsFlow()

    // Rebuilds the BookmarkBlockEntity projection table for a given note on every save.
    // BookmarksScreen reads from this via getAllBookmarksFlow().
    private suspend fun syncBookmarkBlocks(
        noteId: String,
        blocks: List<NoteBlock>,
        sourceType: TaskSource,
        noteUpdatedAt: Long
    ) {
        bookmarkBlockDao.deleteByNoteId(noteId)

        val bookmarks = blocks
            .filterIsInstance<BookmarkBlock>()
            .filter { !it.isDeleted && it.url.isNotBlank() }
            .map { block ->
                BookmarkBlockEntity(
                    blockId = block.id,
                    noteId = noteId,
                    url = block.url,
                    title = block.title,
                    description = block.description,
                    previewImageUrl = block.previewImageUrl,
                    noteUpdatedAt = noteUpdatedAt,
                    sourceType = sourceType
                )
            }

        if (bookmarks.isNotEmpty()) {
            bookmarkBlockDao.upsertBookmarks(bookmarks)
        }
    }

    override fun getAllBookmarksFlow(): Flow<List<BookmarkBlockEntity>> =
        bookmarkBlockDao.getAllBookmarksFlow()

    override fun getImagesCount(): Flow<Int> = imageBlockDao.getImagesCount()
    override fun getDocumentsCount(): Flow<Int> = documentBlockDao.getDocumentsCount()
    override fun getBookmarksCount(): Flow<Int> = bookmarkBlockDao.getBookmarksCount()

    override fun getAllLinkableNotes(): Flow<List<NoteMetadataEntity>> {
        return noteDao.getAllLinkableNotes()
    }

    override suspend fun updateNoteSortOrder(noteId: String, order: Int) =
        withContext(Dispatchers.IO) {
            noteDao.updateNoteSortOrder(noteId, order)
        }

    override suspend fun updateFolderSortOrder(folderId: String, order: Int) =
        withContext(Dispatchers.IO) {
            folderDao.updateFolderSortOrder(folderId, order)
        }

    // clear cache after import
    override fun clearCaches() {
        noteContentCache.value = emptyMap()
        dailyNoteCache.value = emptyMap()
    }
}