package com.ben.inly.presentation.tabs.daily

import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.CalendarTaskEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.room.TaskSource
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AiEventBus
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.SyncEventBus
import com.ben.inly.domain.util.VoiceTaskEventBus
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.BaseEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class DailyEditorViewModel constructor(
    repository: NoteRepository,
    mediaStorageHelper: MediaStorageHelper,
    reminderScheduler: ReminderScheduler,
    audioRecorder: AudioRecorder
) : BaseEditorViewModel(repository, mediaStorageHelper, reminderScheduler, audioRecorder) {

    // Search
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailySearchResults = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.searchDailyNotes("").map { allNotes ->
                val q = query.lowercase()
                val filteredList = mutableListOf<NoteMetadataEntity>()

                for (note in allNotes) {
                    if (note.title.lowercase().contains(q) || note.snippet.lowercase()
                            .contains(q)
                    ) {
                        filteredList.add(note)
                        continue
                    }

                    val date = note.dateString ?: continue
                    val content = repository.getDailyNote(date)

                    if (content != null) {
                        val matches = content.blocks.any { block ->
                            when (block) {
                                is TextBlock -> block.text.lowercase().contains(q)
                                is HeadingBlock -> block.text.lowercase().contains(q)
                                is CheckboxBlock -> block.text.lowercase().contains(q)
                                is QuoteBlock -> block.text.lowercase().contains(q)
                                is BulletedListBlock -> block.text.lowercase().contains(q)
                                is NumberedListBlock -> block.text.lowercase().contains(q)
                                is ToggleBlock -> block.text.lowercase().contains(q)
                                is CodeBlock -> block.code.lowercase().contains(q)
                                is BookmarkBlock -> block.url.lowercase().contains(q)
                                        || block.title?.lowercase()?.contains(q) == true
                                        || block.description?.lowercase()?.contains(q) == true

                                is DocumentBlock -> block.fileName.lowercase().contains(q)
                                is ImageBlock -> block.localFilePath?.lowercase()
                                    ?.contains(q) == true

                                else -> false
                            }
                        }
                        if (matches) filteredList.add(note)
                    }
                }
                filteredList
            }.flowOn(Dispatchers.IO)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Date state
    private val _currentDateString = MutableStateFlow<String?>(null)
    private var currentDateString: String?
        get() = _currentDateString.value
        set(value) { _currentDateString.value = value }
    private val _selectedDate =
        MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _loadedDateString = MutableStateFlow<String?>(null)
    val loadedDateString: StateFlow<String?> = _loadedDateString.asStateFlow()

    // Preview cache
    private val _previewCache = MutableStateFlow<Map<String, List<NoteBlock>>>(emptyMap())
    val previewCache: StateFlow<Map<String, List<NoteBlock>>> = _previewCache.asStateFlow()

    private fun ensureTrailingEmptyBlock(blocks: List<NoteBlock>, dateString: String): List<NoteBlock> {
        if (blocks.isEmpty() || blocks.lastOrNull() !is TextBlock || (blocks.lastOrNull() as? TextBlock)?.text?.isNotEmpty() == true) {
            val cached = _previewCache.value[dateString]
            val cachedTail = cached?.lastOrNull() as? TextBlock
            val tailId = if (cachedTail != null && cachedTail.text.isEmpty()) {
                cachedTail.id // Reuse the ID
            } else {
                java.util.UUID.randomUUID().toString()
            }
            return blocks + listOf(TextBlock(id = tailId, text = ""))
        }
        return blocks
    }

    fun prefetchDateIfNeeded(dateString: String) {
        if (_previewCache.value.containsKey(dateString)) return

        viewModelScope.launch(Dispatchers.IO) {
            val pinnedContent = repository.getDailyNote("global_pinned")
            val pinnedBlocks = pinnedContent?.blocks?.filter { !it.isDeleted } ?: emptyList()

            val content = repository.getDailyNote(dateString)
            val blocks = content?.blocks ?: emptyList()

            var merged = pinnedBlocks + (if (isNoteActuallyEmpty(blocks)) emptyList() else blocks)
            merged = ensureTrailingEmptyBlock(merged, dateString)

            val resolved = recalculateNumberedLists(merged)
            _previewCache.update { it + (dateString to resolved.filter { b -> !b.isDeleted }) }
        }
    }

    fun evictPreviewCache(keepDates: Set<String>) {
        _previewCache.update { current -> current.filterKeys { it in keepDates } }
    }

    override fun scheduleAutosave() {
        isAiIndexDirty = true
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            currentDateString?.let { date ->
                _previewCache.update { it + (date to _blocks.value.filter { b -> !b.isDeleted }) }
            }
            delay(1000L)
            performSave()
        }
    }

    // Init
    init {
        loadDailyNote(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
        viewModelScope.launch {
            VoiceTaskEventBus.taskAddedEvent.collect { event ->
                if (event.dateString == currentDateString) {
                    val currentBlocks = _blocks.value.toMutableList()

                    if (currentBlocks.size == 1
                        && currentBlocks.first() is TextBlock
                        && (currentBlocks.first() as TextBlock).text.isBlank()
                    ) {
                        currentBlocks.clear()
                    }

                    currentBlocks.add(event.block)
                    _blocks.value = recalculateNumberedLists(currentBlocks)
                    scheduleAutosave()
                }
            }
        }
        viewModelScope.launch {
            SyncEventBus.syncCompletedEvent.collect { syncedEntityId ->

                if (syncedEntityId == currentDateString || syncedEntityId == "global_pinned" || syncedEntityId == "import_complete") {
                    if (syncedEntityId == "import_complete") {
                        autosaveJob?.cancel()
                    } else if (autosaveJob?.isActive == true) {
                        return@collect
                    }

                    currentDateString?.let {
                        val pinnedContent =
                            withContext(Dispatchers.IO) { repository.getDailyNote("global_pinned") }
                        val pinnedBlocks =
                            pinnedContent?.blocks?.filter { !it.isDeleted } ?: emptyList()

                        val content = withContext(Dispatchers.IO) { repository.getDailyNote(it) }
                        val newBlocks = content?.blocks ?: emptyList()

                        var merged = pinnedBlocks + (if (isNoteActuallyEmpty(newBlocks)) emptyList() else newBlocks)
                        merged = ensureTrailingEmptyBlock(merged, it)

                        val finalBlocks = recalculateNumberedLists(merged)
                        if (finalBlocks != _blocks.value) {
                            _blocks.value = finalBlocks
                            _previewCache.update { cache -> cache + (it to finalBlocks.filter { b -> !b.isDeleted }) }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            _currentDateString
                .filterNotNull()
                .flatMapLatest { date -> repository.observeDailyNote(date) }
                .filterNotNull()
                .collect { freshContent ->
                    val date = currentDateString ?: return@collect
                    if (_loadedDateString.value != date) return@collect
                    if (autosaveJob?.isActive == true) return@collect

                    val pinnedContent = repository.getDailyNote("global_pinned")
                    val pinnedBlocks = pinnedContent?.blocks?.filter { !it.isDeleted } ?: emptyList()

                    var merged = pinnedBlocks + (if (isNoteActuallyEmpty(freshContent.blocks)) emptyList() else freshContent.blocks)
                    merged = ensureTrailingEmptyBlock(merged, date)
                    val final = recalculateNumberedLists(merged)

                    if (final != _blocks.value) {
                        _blocks.value = final
                        _previewCache.update { it + (date to final.filter { b -> !b.isDeleted }) }
                    }
                }
        }
    }

    override suspend fun performSave() {
        if (_loadedDateString.value == null || _loadedDateString.value != currentDateString) return

        val dateToSave = currentDateString ?: return
        val snapshot = _blocks.value.toList()

        val pinnedBlocks = snapshot.filter { it.isPinned }
        val dailyBlocks = snapshot.filter { !it.isPinned }

        withContext(Dispatchers.IO) {
            repository.saveDailyNote("global_pinned", NoteContent(blocks = pinnedBlocks))
            repository.saveDailyNote(dateToSave, NoteContent(blocks = dailyBlocks))
        }
    }

    override fun getNoteTitleForReminder(): String =
        "Daily: ${currentDateString ?: "Note"}"

    // Loading
    fun loadDailyNote(dateString: String) {
        if (currentDateString == dateString) return
        currentDateString = dateString
        _loadedDateString.value = null

        AiEventBus.activeNoteId = dateString

        viewModelScope.launch(Dispatchers.IO) {
            val pinnedContent = repository.getDailyNote("global_pinned")
            val pinnedBlocks = pinnedContent?.blocks?.filter { !it.isDeleted } ?: emptyList()

            val content = repository.getDailyNote(dateString)
            val existingBlocks = content?.blocks ?: emptyList()

            try {
                val targetDate = LocalDate.parse(dateString)
                if (targetDate == Clock.System.todayIn(TimeZone.currentSystemDefault())) {
                    val yesterdayString = targetDate.minus(1, DateTimeUnit.DAY).toString()
                    val yesterdayContent = repository.getDailyNote(yesterdayString)
                    val allYesterdayBlocks = yesterdayContent?.blocks ?: emptyList()
                    val unfinishedTasks = allYesterdayBlocks
                        .filterIsInstance<CheckboxBlock>()
                        .filter { !it.isChecked && !it.isDeleted }

                    if (unfinishedTasks.isNotEmpty()) {
                        val cleanExistingBlocks =
                            if (isNoteActuallyEmpty(existingBlocks)) emptyList() else existingBlocks
                        val existingIds = cleanExistingBlocks.mapTo(HashSet()) { it.id }
                        val rolledOverTasks = unfinishedTasks
                            .map { it.copy(id = "rollover_${it.id}_$dateString") }
                            .filter { it.id !in existingIds }

                        var mergedBlocks = pinnedBlocks + rolledOverTasks + cleanExistingBlocks
                        mergedBlocks = ensureTrailingEmptyBlock(mergedBlocks, dateString)

                        val finalBlocks = recalculateNumberedLists(mergedBlocks)
                        if (currentDateString != dateString) return@launch
                        _blocks.value = finalBlocks
                        _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                        _loadedDateString.value = dateString
                        lastIndexedContentHash = 0

                        val rolledIds = unfinishedTasks.mapTo(HashSet()) { it.id }
                        val updatedYesterdayBlocks = allYesterdayBlocks
                            .map { if (it.id in rolledIds) it.markDeleted() else it }

                        repository.saveDailyNote(
                            yesterdayString,
                            NoteContent(blocks = updatedYesterdayBlocks)
                        )

                        val yesterdayMeta = repository.getDailyNoteMetadata(yesterdayString)
                        if (yesterdayMeta != null) {
                            repository.indexDailyNote(yesterdayString, NoteContent(blocks = updatedYesterdayBlocks), yesterdayMeta)
                        }

                        performSave()
                        return@launch
                    }
                }

                val cleanExistingBlocks = if (isNoteActuallyEmpty(existingBlocks)) emptyList() else existingBlocks
                var mergedBlocks = pinnedBlocks + cleanExistingBlocks

                mergedBlocks = ensureTrailingEmptyBlock(mergedBlocks, dateString)

                val finalBlocks = recalculateNumberedLists(mergedBlocks)
                if (currentDateString != dateString) return@launch
                _blocks.value = finalBlocks
                _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                _loadedDateString.value = dateString
                lastIndexedContentHash = 0

            } catch (e: Exception) {
                val cleanExistingBlocks = if (isNoteActuallyEmpty(existingBlocks)) emptyList() else existingBlocks
                var mergedBlocks = pinnedBlocks + cleanExistingBlocks

                mergedBlocks = ensureTrailingEmptyBlock(mergedBlocks, dateString)

                val finalBlocks = recalculateNumberedLists(mergedBlocks)
                if (currentDateString != dateString) return@launch
                _blocks.value = finalBlocks
                _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                _loadedDateString.value = dateString
                lastIndexedContentHash = 0
            }
        }
    }

    // Date selection
    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        autosaveJob?.cancel()
        indexingJob?.cancel()

        val dateToSave = currentDateString
        val blocksToSave = _blocks.value.toList()
        val wasLoaded = _loadedDateString.value == dateToSave

        _selectedDate.value = date
        currentDateString = null
        _loadedDateString.value = null
        _blocks.value = emptyList()
        clearSelection()

        viewModelScope.launch {
            if (wasLoaded && dateToSave != null) {
                val pinnedBlocks = blocksToSave.filter { it.isPinned }
                val dailyBlocks = blocksToSave.filter { !it.isPinned }
                withContext(Dispatchers.IO + NonCancellable) {
                    repository.saveDailyNote("global_pinned", NoteContent(blocks = pinnedBlocks))
                    repository.saveDailyNote(dateToSave, NoteContent(blocks = dailyBlocks))
                }
            }
            loadDailyNote(date.toString())
        }
    }

    override suspend fun performIndexing() {
        if (_loadedDateString.value == null || _loadedDateString.value != currentDateString) return
        val dateToSave = currentDateString ?: return
        val snapshot = _blocks.value.toList()

        val dailyBlocks = snapshot.filter { !it.isPinned }

        withContext(Dispatchers.IO) {
            val meta = repository.getDailyNoteMetadata(dateToSave)
            if (meta != null) {
                repository.indexDailyNote(dateToSave, NoteContent(blocks = dailyBlocks), meta)
            }
        }
    }

    private val _visibleCalendarMonth = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val calendarTaskMap: StateFlow<Map<LocalDate, List<CalendarTaskEntity>>> = _visibleCalendarMonth
        .flatMapLatest { date ->
            val monthStr = date.monthNumber.toString().padStart(2, '0')
            val yearMonth = "${date.year}-$monthStr"
            repository.getCalendarTasksForMonth(yearMonth)
        }
        .map { tasks ->
            tasks.groupBy { LocalDate.parse(it.targetDate!!) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun updateVisibleCalendarMonth(date: LocalDate) {
        _visibleCalendarMonth.value = date
    }

    fun toggleCalendarTask(task: CalendarTaskEntity, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (task.sourceType == TaskSource.DAILY && task.noteId == currentDateString) {
                _blocks.update { currentBlocks ->
                    currentBlocks.map { block ->
                        if (block.id == task.blockId && block is CheckboxBlock) {
                            block.copy(isChecked = isChecked)
                        } else block
                    }
                }
                performSave()
                return@launch
            }

            if (task.sourceType == TaskSource.DAILY) {
                val content = repository.getDailyNote(task.noteId) ?: return@launch
                val updatedBlocks = content.blocks.map { block ->
                    if (block.id == task.blockId && block is CheckboxBlock) {
                        block.copy(isChecked = isChecked)
                    } else block
                }
                val meta = repository.getDailyNoteMetadata(task.noteId)
                repository.saveDailyNote(task.noteId, NoteContent(blocks = updatedBlocks), remoteMeta = meta)
            }
            else if (task.sourceType == TaskSource.NOTE) {
                val meta = repository.getNoteById(task.noteId) ?: return@launch
                val content = repository.getNoteContent(task.noteId) ?: return@launch
                val updatedBlocks = content.blocks.map { block ->
                    if (block.id == task.blockId && block is CheckboxBlock) {
                        block.copy(isChecked = isChecked)
                    } else block
                }
                repository.saveNote(meta, NoteContent(blocks = updatedBlocks))
            }
        }
    }
}