package com.ben.inly.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.CategoryEntity
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.presentation.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CalendarViewModel(
    private val repository: NoteRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsManager: SettingsManager
) : ViewModel() {

    val categories: StateFlow<List<CalendarCategory>> = repository.getAllCategories()
        .map { entities -> entities.map { it.toCalendarCategory() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultCalendarCategories())
    val viewMode: StateFlow<CalendarViewMode> = settingsManager.calendarViewModeFlow
        .map { stored -> runCatching { CalendarViewMode.valueOf(stored) }.getOrDefault(CalendarViewMode.DAY) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalendarViewMode.DAY)

    fun setViewMode(mode: CalendarViewMode) {
        settingsManager.saveCalendarViewMode(mode.name)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getAllCategories().first().isEmpty()) {
                val default = defaultCalendarCategories().first()
                repository.insertOrUpdateCategory(default.id, default.name, default.colorHex)
            }
        }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateCategory(UUID.randomUUID().toString(), name, colorHex)
        }
    }

    fun updateCategory(id: String, name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateCategory(id, name, colorHex)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(id)
        }
    }

    fun eventsForDate(dateString: String): Flow<List<CalendarEvent>> =
        repository.getCalendarTasksForDate(dateString).map { tasks -> tasks.mapNotNull { it.toCalendarEvent() } }
    fun eventsForMonth(yearMonth: String): Flow<List<CalendarEvent>> =
        repository.getCalendarTasksForMonth(yearMonth).map { tasks -> tasks.mapNotNull { it.toCalendarEvent() } }

    // Events ARE checkboxes: creating/rescheduling one means writing a CheckboxBlock into that
    // day's note content (which re-derives the CalendarTaskEntity row as a side effect, see
    // NoteRepositoryImpl.syncCalendarTasks) - there is no separate "event" storage to keep in sync.
    fun saveEvent(
        original: CalendarEvent?,
        dateString: String,
        timestamp: Long,
        name: String,
        categoryId: String?,
        durationMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val blockId = original?.blockId ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            var baseBlock = CheckboxBlock(id = blockId, updatedAt = now)
            if (original != null) {
                val oldBlocks = repository.getDailyNote(original.dateString)?.blocks ?: emptyList()
                (oldBlocks.firstOrNull { it.id == blockId } as? CheckboxBlock)?.let { baseBlock = it }
                if (original.dateString != dateString) {
                    repository.saveDailyNote(
                        original.dateString,
                        NoteContent(blocks = oldBlocks.filterNot { it.id == blockId })
                    )
                }
            }

            val updatedBlock = baseBlock.copy(
                text = name,
                reminderTimestamp = timestamp,
                categoryId = categoryId,
                durationMinutes = durationMinutes,
                updatedAt = now
            )

            val targetBlocks = repository.getDailyNote(dateString)?.blocks ?: emptyList()
            val newBlocks = if (targetBlocks.any { it.id == blockId }) {
                targetBlocks.map { if (it.id == blockId) updatedBlock else it }
            } else {
                listOf(updatedBlock) + targetBlocks
            }
            repository.saveDailyNote(dateString, NoteContent(blocks = newBlocks))

            reminderScheduler.schedule(
                blockId = blockId,
                noteTitle = "Daily: $dateString",
                text = name.ifBlank { "Unfinished task" },
                timestamp = timestamp
            )
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            val blocks = repository.getDailyNote(event.dateString)?.blocks ?: return@launch
            repository.saveDailyNote(event.dateString, NoteContent(blocks = blocks.filterNot { it.id == event.blockId }))
            reminderScheduler.cancel(event.blockId)
        }
    }
}

private fun CategoryEntity.toCalendarCategory() = CalendarCategory(
    id = categoryId,
    name = name,
    colorHex = colorHex
)
