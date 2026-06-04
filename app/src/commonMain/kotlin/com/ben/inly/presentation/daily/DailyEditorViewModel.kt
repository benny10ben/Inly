package com.ben.inly.presentation.daily

import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.VoiceTaskEventBus
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.BaseEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import java.util.UUID

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
                    if (note.title.lowercase().contains(q) || note.snippet.lowercase().contains(q)) {
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
                                is ImageBlock -> block.localFilePath?.lowercase()?.contains(q) == true
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

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    // Date state
    private var currentDateString: String? = null
    private val _selectedDate = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _loadedDateString = MutableStateFlow<String?>(null)
    val loadedDateString: StateFlow<String?> = _loadedDateString.asStateFlow()

    // Preview cache
    private val _previewCache = MutableStateFlow<Map<String, List<NoteBlock>>>(emptyMap())
    val previewCache: StateFlow<Map<String, List<NoteBlock>>> = _previewCache.asStateFlow()

    fun prefetchDateIfNeeded(dateString: String) {
        if (_previewCache.value.containsKey(dateString)) return

        viewModelScope.launch(Dispatchers.IO) {
            val content = repository.getDailyNote(dateString)
            val blocks = content?.blocks ?: emptyList()
            val resolved = if (isNoteActuallyEmpty(blocks)) {
                listOf(TextBlock(id = "root_$dateString", text = ""))
            } else {
                recalculateNumberedLists(blocks)
            }
            _previewCache.update { it + (dateString to resolved.filter { b -> !b.isDeleted }) }
        }
    }

    fun evictPreviewCache(keepDates: Set<String>) {
        _previewCache.update { current -> current.filterKeys { it in keepDates } }
    }

    override fun scheduleAutosave() {
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
        startMidnightTimer()

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
            com.ben.inly.domain.util.SyncEventBus.syncCompletedEvent.collect { syncedEntityId ->
                if (syncedEntityId == currentDateString) {
                    val content = withContext(Dispatchers.IO) { repository.getDailyNote(syncedEntityId) }
                    val newBlocks = content?.blocks ?: emptyList()

                    val finalBlocks = recalculateNumberedLists(
                        if (isNoteActuallyEmpty(newBlocks)) {
                            listOf(TextBlock(id = "root_$syncedEntityId", text = ""))
                        } else {
                            newBlocks
                        }
                    )

                    _blocks.value = finalBlocks
                    _previewCache.update { it + (syncedEntityId to finalBlocks.filter { b -> !b.isDeleted }) }
                }
            }
        }
    }

    private fun startMidnightTimer() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                val newToday = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (_selectedDate.value == newToday.minus(1, DateTimeUnit.DAY)) {
                    selectDate(newToday)
                }
            }
        }
    }

    // Save
    override suspend fun performSave() {
        val dateToSave = currentDateString ?: return
        val snapshot = _blocks.value.toList()
        withContext(Dispatchers.IO) {
            repository.saveDailyNote(dateToSave, NoteContent(blocks = snapshot))
        }
    }

    override fun getNoteTitleForReminder(): String =
        "Daily: ${currentDateString ?: "Note"}"

    // Loading
    fun loadDailyNote(dateString: String) {
        if (currentDateString == dateString) return
        currentDateString = dateString
        _loadedDateString.value = null

        viewModelScope.launch(Dispatchers.IO) {
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
                        val rolledOverTasks = unfinishedTasks.map {
                            it.copy(id = "rollover_${it.id}_$dateString")
                        }

                        val cleanExistingBlocks =
                            if (isNoteActuallyEmpty(existingBlocks)) emptyList() else existingBlocks
                        var mergedBlocks = rolledOverTasks + cleanExistingBlocks

                        if (mergedBlocks.lastOrNull() !is TextBlock
                            || (mergedBlocks.lastOrNull() as? TextBlock)?.text?.isNotEmpty() == true
                        ) {
                            mergedBlocks = mergedBlocks + listOf(
                                TextBlock(id = "root_$dateString", text = "")
                            )
                        }

                        val finalBlocks = recalculateNumberedLists(mergedBlocks)
                        _blocks.value = finalBlocks
                        _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                        _loadedDateString.value = dateString

                        val updatedYesterdayBlocks = allYesterdayBlocks
                            .filterNot { it in unfinishedTasks }
                            .ifEmpty { listOf(TextBlock(id = "root_$yesterdayString", text = "")) }

                        repository.saveDailyNote(
                            yesterdayString,
                            NoteContent(blocks = updatedYesterdayBlocks)
                        )
                        performSave()
                        return@launch
                    }
                }

                val finalBlocks = recalculateNumberedLists(
                    if (isNoteActuallyEmpty(existingBlocks)) {
                        listOf(TextBlock(id = "root_$dateString", text = ""))
                    } else {
                        existingBlocks
                    }
                )
                _blocks.value = finalBlocks
                _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                _loadedDateString.value = dateString

            } catch (e: Exception) {
                val finalBlocks = recalculateNumberedLists(
                    if (isNoteActuallyEmpty(existingBlocks)) {
                        listOf(TextBlock(id = "root_$dateString", text = ""))
                    } else {
                        existingBlocks
                    }
                )
                _blocks.value = finalBlocks
                _previewCache.update { it + (dateString to finalBlocks.filter { b -> !b.isDeleted }) }
                _loadedDateString.value = dateString
            }
        }
    }

    // Date selection
    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        autosaveJob?.cancel()

        val dateToSave = currentDateString
        val blocksToSave = _blocks.value.toList()

        if (blocksToSave.isNotEmpty() && dateToSave != null && !isNoteActuallyEmpty(blocksToSave)) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveDailyNote(dateToSave, NoteContent(blocks = blocksToSave))
            }
        }

        _selectedDate.value = date
        currentDateString = null
        _blocks.value = emptyList()
        clearSelection()
        loadDailyNote(date.toString())
    }
}