package com.ben.inly.presentation.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyButtonSecondary
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import com.ben.inly.presentation.shared.components.InlyTextField
import com.ben.inly.presentation.shared.components.MinimalDatePickerDialog
import com.ben.inly.presentation.shared.components.MinimalTimePickerDialog
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val EventChipTextColor = Color(0xFF1A1A1A)

@Composable
fun EventChip(
    text: String,
    color: Color,
    hasCategory: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = if (hasCategory) EventChipTextColor else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = modifier
            .height(height)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.TopStart) {
            Text(
                text = text.ifBlank { "Untitled event" },
                fontFamily = PoppinsFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

data class EventEditorState(
    val original: CalendarEvent?,
    val name: String,
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val categoryId: String?,
    val durationMinutes: Int = 30
)

fun EventEditorState.toEpochMillis(): Long {
    val localDateTime = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute)
    return localDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
fun EventEditorState.toEndEpochMillis(): Long = toEpochMillis() + durationMinutes * 60_000L

fun EventEditorState.endHour(): Int {
    val startTotal = hour * 60 + minute
    return ((startTotal + durationMinutes) / 60) % 24
}

fun EventEditorState.endMinute(): Int = (minute + durationMinutes) % 60

fun formatTimeOfDay(hour: Int, minute: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val period = if (hour < 12) "AM" else "PM"
    return "$displayHour:${minute.toString().padStart(2, '0')} $period"
}

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "${mins}min"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}min"
    }
}

@Composable
fun EventEditorSheet(
    state: EventEditorState?,
    categories: List<CalendarCategory>,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onDurationChange: (minutes: Int) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    desktopMenuOffset: DpOffset = DpOffset.Zero
) {
    if (isDesktopPlatform) {
        InlyDesktopMenu(
            expanded = state != null,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(340.dp),
            offset = desktopMenuOffset
        ) {
            if (state != null) {
                Text(
                    text = if (state.original == null) "Add Event" else "Edit Event",
                    fontFamily = PoppinsFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                EventEditorFields(
                    state = state,
                    categories = categories,
                    onNameChange = onNameChange,
                    onDateChange = onDateChange,
                    onTimeChange = onTimeChange,
                    onDurationChange = onDurationChange,
                    onCategoryChange = onCategoryChange,
                    onCancel = onDismiss,
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
        }
    } else {
        InlyBottomSheet(
            expanded = state != null,
            onDismiss = onDismiss,
            title = if (state?.original == null) "Add Event" else "Edit Event"
        ) { closeAnd ->
            if (state != null) {
                EventEditorFields(
                    state = state,
                    categories = categories,
                    onNameChange = onNameChange,
                    onDateChange = onDateChange,
                    onTimeChange = onTimeChange,
                    onDurationChange = onDurationChange,
                    onCategoryChange = onCategoryChange,
                    onCancel = { closeAnd(onDismiss) },
                    onSave = { closeAnd(onSave) },
                    onDelete = onDelete?.let { delete -> { closeAnd(delete) } }
                )
            }
        }
    }
}

@Composable
private fun EventEditorFields(
    state: EventEditorState,
    categories: List<CalendarCategory>,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onDurationChange: (minutes: Int) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp)
    ) {
                InlyTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = "Event name",
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    EventFieldRow(
                        icon = Icons.Default.CalendarMonth,
                        label = formatFullDate(state.date),
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showDatePicker) {
                        MinimalDatePickerDialog(
                            initialTimestamp = state.toEpochMillis(),
                            onDismiss = { showDatePicker = false },
                            onConfirm = { millis ->
                                val instant = Instant.fromEpochMilliseconds(millis)
                                onDateChange(instant.toLocalDateTime(TimeZone.UTC).date)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EventFieldRow(
                            icon = Icons.Default.Schedule,
                            label = formatTimeOfDay(state.hour, state.minute),
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showStartTimePicker) {
                            MinimalTimePickerDialog(
                                initialTimestamp = state.toEpochMillis(),
                                onDismiss = { showStartTimePicker = false },
                                onConfirm = { hour, minute -> onTimeChange(hour, minute) }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EventFieldRow(
                            icon = Icons.Default.Schedule,
                            label = formatTimeOfDay(state.endHour(), state.endMinute()),
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showEndTimePicker) {
                            MinimalTimePickerDialog(
                                initialTimestamp = state.toEndEpochMillis(),
                                onDismiss = { showEndTimePicker = false },
                                onConfirm = { hour, minute ->
                                    val startTotal = state.hour * 60 + state.minute
                                    var endTotal = hour * 60 + minute
                                    if (endTotal <= startTotal) endTotal += 24 * 60
                                    onDurationChange((endTotal - startTotal).coerceAtLeast(5))
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Duration: ${formatDuration(state.durationMinutes)}",
                    fontFamily = PoppinsFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Category",
                    fontFamily = PoppinsFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "None",
                        color = MaterialTheme.colorScheme.surface,
                        hasCategory = false,
                        isSelected = state.categoryId == null,
                        onClick = { onCategoryChange(null) }
                    )
                    categories.forEach { category ->
                        CategoryChip(
                            label = category.name,
                            color = category.colorHex.toCategoryColor(),
                            hasCategory = true,
                            isSelected = state.categoryId == category.id,
                            onClick = { onCategoryChange(category.id) }
                        )
                    }
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Delete event",
                            fontFamily = PoppinsFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (onDelete != null) 4.dp else 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InlyButtonSecondary(
                        text = "Cancel",
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    )
                    InlyButtonPrimary(
                        text = if (state.original == null) "Add" else "Save",
                        onClick = onSave,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
}

@Composable
private fun EventFieldRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontFamily = PoppinsFont,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    color: Color,
    hasCategory: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (hasCategory) EventChipTextColor else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color,
        modifier = Modifier
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontFamily = PoppinsFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// An event plus where it lands in the day column's horizontal layout: `columnIndex` of
// `columnCount` equal-width slots, so overlapping events sit side by side instead of stacking.
data class PositionedEvent(
    val event: CalendarEvent,
    val columnIndex: Int,
    val columnCount: Int
)

private fun CalendarEvent.endTimestamp(): Long = reminderTimestamp + durationMinutes * 60_000L
fun layoutEventsForColumn(events: List<CalendarEvent>): List<PositionedEvent> {
    if (events.isEmpty()) return emptyList()
    val sorted = events.sortedBy { it.reminderTimestamp }

    val result = mutableListOf<PositionedEvent>()
    var clusterColumns = mutableListOf<MutableList<CalendarEvent>>()
    var clusterEnd = Long.MIN_VALUE

    fun flushCluster() {
        val columnCount = clusterColumns.size
        if (columnCount == 0) return
        clusterColumns.forEachIndexed { columnIndex, column ->
            column.forEach { event -> result.add(PositionedEvent(event, columnIndex, columnCount)) }
        }
        clusterColumns = mutableListOf()
        clusterEnd = Long.MIN_VALUE
    }

    for (event in sorted) {
        if (clusterColumns.isNotEmpty() && event.reminderTimestamp >= clusterEnd) {
            flushCluster()
        }
        val targetColumn = clusterColumns.firstOrNull { column -> column.last().endTimestamp() <= event.reminderTimestamp }
        if (targetColumn != null) {
            targetColumn.add(event)
        } else {
            clusterColumns.add(mutableListOf(event))
        }
        clusterEnd = maxOf(clusterEnd, event.endTimestamp())
    }
    flushCluster()

    return result
}
