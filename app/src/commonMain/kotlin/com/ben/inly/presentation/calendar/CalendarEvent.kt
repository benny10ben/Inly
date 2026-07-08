package com.ben.inly.presentation.calendar

import com.ben.inly.data.local.room.CalendarTaskEntity
import com.ben.inly.data.local.room.TaskSource

data class CalendarEvent(
    val blockId: String,
    val noteId: String,
    val text: String,
    val isChecked: Boolean,
    val dateString: String,
    val reminderTimestamp: Long,
    val categoryId: String?,
    val durationMinutes: Int,
    val sourceType: TaskSource
)

fun CalendarTaskEntity.toCalendarEvent(): CalendarEvent? {
    val timestamp = reminderTimestamp ?: return null
    val date = targetDate ?: return null
    return CalendarEvent(
        blockId = blockId,
        noteId = noteId,
        text = text,
        isChecked = isChecked,
        dateString = date,
        reminderTimestamp = timestamp,
        categoryId = categoryId,
        durationMinutes = durationMinutes,
        sourceType = sourceType
    )
}
