package com.ben.inly.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskSource {
    DAILY,
    NOTE
}

@Entity(
    tableName = "calendar_tasks",
    indices = [
        Index("targetDate"),
        Index("noteId")
    ]
)
data class CalendarTaskEntity(
    @PrimaryKey val blockId: String,
    val noteId: String,
    val text: String,
    val isChecked: Boolean,
    val targetDate: String?,
    val reminderTimestamp: Long?,
    val sourceType: TaskSource
)