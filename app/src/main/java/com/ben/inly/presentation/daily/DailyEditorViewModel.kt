package com.ben.inly.presentation.daily

import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.*
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.VoiceTaskEventBus // NEW IMPORT
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.shared.editor.BaseEditorViewModel
import com.ben.inly.presentation.shared.editor.FocusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.util.UUID
import javax.inject.Inject

/**
 * Handles the logic for the Daily Notes screen.
 * Manages autosaving, searching across dates, and automatically moving unfinished checkbox tasks to the next day when midnight hits.
 */
@HiltViewModel
class DailyEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    mediaStorageHelper: MediaStorageHelper,
    reminderScheduler: ReminderScheduler
) : BaseEditorViewModel(mediaStorageHelper, reminderScheduler) {

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
                                is BulletedListBlock -> block.text.lowercase().contains(q)
                                is NumberedListBlock -> block.text.lowercase().contains(q)
                                is ToggleBlock -> block.text.lowercase().contains(q)
                                is CodeBlock -> block.code.lowercase().contains(q)
                                is BookmarkBlock -> block.url.lowercase().contains(q) || block.title?.lowercase()?.contains(q) == true || block.description?.lowercase()?.contains(q) == true
                                is DocumentBlock -> block.fileName.lowercase().contains(q)
                                is ImageBlock -> block.localFilePath?.lowercase()?.contains(q) == true
                                else -> false
                            }
                        }
                        if (matches) {
                            filteredList.add(note)
                        }
                    }
                }
                filteredList
            }.flowOn(Dispatchers.IO)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    private var currentDateString: String? = null
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _loadedDateString = MutableStateFlow<String?>(null)
    val loadedDateString: StateFlow<String?> = _loadedDateString.asStateFlow()

    init {
        loadDailyNote(LocalDate.now().toString())
        startMidnightTimer()

        viewModelScope.launch {
            VoiceTaskEventBus.taskAddedEvent.collect { event ->
                if (event.dateString == currentDateString) {
                    val currentBlocks = _blocks.value.toMutableList()

                    if (currentBlocks.size == 1 && currentBlocks.first() is TextBlock && (currentBlocks.first() as TextBlock).text.isBlank()) {
                        currentBlocks.clear()
                    }

                    currentBlocks.add(event.block)
                    _blocks.value = recalculateNumberedLists(currentBlocks)
                    scheduleAutosave()
                }
            }
        }
    }

    private fun startMidnightTimer() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delayMillis = Duration.between(now, nextMidnight).toMillis()
                delay(delayMillis + 1000L)
                val newToday = LocalDate.now()
                if (_selectedDate.value == newToday.minusDays(1)) {
                    selectDate(newToday)
                }
            }
        }
    }

    override suspend fun performSave() {
        val dateToSave = currentDateString ?: return
        val snapshot = _blocks.value.toList()
        withContext(Dispatchers.IO) {
            repository.saveDailyNote(dateToSave, NoteContent(blocks = snapshot))
        }
    }

    override fun getNoteTitleForReminder(): String {
        return "Daily: ${currentDateString ?: "Note"}"
    }

    fun loadDailyNote(dateString: String) {
        if (currentDateString == dateString) return
        currentDateString = dateString
        _loadedDateString.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val content = repository.getDailyNote(dateString)
            val existingBlocks = content?.blocks ?: emptyList()

            try {
                val targetDate = LocalDate.parse(dateString)
                if (targetDate == LocalDate.now()) {
                    val yesterdayString = targetDate.minusDays(1).toString()
                    val yesterdayContent = repository.getDailyNote(yesterdayString)
                    val allYesterdayBlocks = yesterdayContent?.blocks ?: emptyList()
                    val unfinishedTasks = allYesterdayBlocks.filterIsInstance<CheckboxBlock>().filter { !it.isChecked }

                    if (unfinishedTasks.isNotEmpty()) {
                        val rolledOverTasks = unfinishedTasks.map { it.copy(id = UUID.randomUUID().toString()) }
                        val cleanExistingBlocks = if (isNoteActuallyEmpty(existingBlocks)) emptyList() else existingBlocks
                        var mergedBlocks = rolledOverTasks + cleanExistingBlocks
                        if (mergedBlocks.lastOrNull() !is TextBlock || (mergedBlocks.lastOrNull() as? TextBlock)?.text?.isNotEmpty() == true) {
                            mergedBlocks = mergedBlocks + listOf(TextBlock(id = UUID.randomUUID().toString(), text = ""))
                        }

                        _blocks.value = recalculateNumberedLists(mergedBlocks)
                        _loadedDateString.value = dateString

                        val updatedYesterdayBlocks = allYesterdayBlocks.filterNot { it in unfinishedTasks }.ifEmpty { listOf(TextBlock(id = UUID.randomUUID().toString(), text = "")) }
                        repository.saveDailyNote(yesterdayString, NoteContent(blocks = updatedYesterdayBlocks))
                        performSave()
                        return@launch
                    }
                }

                _blocks.value = recalculateNumberedLists(if (isNoteActuallyEmpty(existingBlocks)) listOf(TextBlock(id = UUID.randomUUID().toString(), text = "")) else existingBlocks)
                _loadedDateString.value = dateString
            } catch (e: Exception) {
                _blocks.value = recalculateNumberedLists(if (isNoteActuallyEmpty(existingBlocks)) listOf(TextBlock(id = UUID.randomUUID().toString(), text = "")) else existingBlocks)
                _loadedDateString.value = dateString
            }
        }
    }

    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        autosaveJob?.cancel()

        val dateToSave = currentDateString
        val blocksToSave = _blocks.value.toList()

        if (blocksToSave.isNotEmpty() && dateToSave != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveDailyNote(dateToSave, NoteContent(blocks = blocksToSave))
            }
        }

        _selectedDate.value = date
        currentDateString = null
        clearSelection()
        loadDailyNote(date.toString())
    }

    suspend fun fetchBlocksForPreview(dateString: String): List<NoteBlock> = withContext(Dispatchers.IO) {
        return@withContext try {
            val content = repository.getDailyNote(dateString)
            val existing = content?.blocks ?: emptyList()
            if (isNoteActuallyEmpty(existing)) listOf(TextBlock(id = UUID.randomUUID().toString(), text = "")) else recalculateNumberedLists(existing)
        } catch (e: Exception) {
            listOf(TextBlock(id = UUID.randomUUID().toString(), text = ""))
        }
    }
}