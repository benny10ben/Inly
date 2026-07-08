package com.ben.inly.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class TaskSource {
    DAILY,
    NOTE
}

@Serializable
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
    val sourceType: TaskSource,
    val categoryId: String? = null,
    @ColumnInfo(defaultValue = "30") val durationMinutes: Int = 30
)