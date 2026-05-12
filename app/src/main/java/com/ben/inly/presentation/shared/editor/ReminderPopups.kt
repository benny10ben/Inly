package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.BricolageFont
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val PopupButtonShape = RoundedCornerShape(8.dp)

@Composable
fun ReminderPresetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onCustomSelected: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Date"
    ) { closeAnd ->
        PresetSheetItem(
            icon = Icons.Default.Today,
            text = "Later today",
            onClick = {
                closeAnd {
                    onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.Event,
            text = "Tomorrow",
            onClick = {
                closeAnd {
                    onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.Weekend,
            text = "This weekend",
            onClick = {
                closeAnd {
                    onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.NextWeek,
            text = "Next week",
            onClick = {
                closeAnd {
                    onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK))
                }
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        PresetSheetItem(
            icon = Icons.Default.CalendarMonth,
            text = "Custom date...",
            onClick = {
                closeAnd {
                    onCustomSelected()
                }
            }
        )

        if (onRemove != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            PresetSheetItem(
                icon = Icons.Default.NotificationsOff,
                text = "Remove reminder",
                isDestructive = true,
                onClick = {
                    closeAnd {
                        onRemove()
                    }
                }
            )
        }

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(48.dp),
            shape = PopupButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Close",
                fontFamily = BricolageFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun TimePresetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onCustomSelected: () -> Unit
) {
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Time"
    ) { closeAnd ->
        PresetSheetItem(
            icon = Icons.Default.Timer,
            text = "In 15 mins",
            onClick = {
                closeAnd {
                    onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.Schedule,
            text = "In 1 hour",
            onClick = {
                closeAnd {
                    onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.AccessTime,
            text = "In 3 hours",
            onClick = {
                closeAnd {
                    onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS))
                }
            }
        )

        PresetSheetItem(
            icon = Icons.Default.NightsStay,
            text = "This evening",
            onClick = {
                closeAnd {
                    onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING))
                }
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        PresetSheetItem(
            icon = Icons.Default.AccessTime,
            text = "Custom time...",
            onClick = {
                closeAnd {
                    onCustomSelected()
                }
            }
        )

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(48.dp),
            shape = PopupButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Close",
                fontFamily = BricolageFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun PresetSheetItem(
    icon: ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontFamily = BricolageFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalDatePickerDialog(
    expanded: Boolean = true,
    initialTimestamp: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val seedMillis = initialTimestamp ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = seedMillis)

    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Select Date"
    ) { closeAnd ->
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density * 0.85f,
                fontScale = currentDensity.fontScale
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    todayContentColor = MaterialTheme.colorScheme.onSurface,
                    todayDateBorderColor = MaterialTheme.colorScheme.outline,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = {
                    closeAnd {
                        datePickerState.selectedDateMillis?.let { onConfirm(it) }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Save",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun MinimalTimePickerDialog(
    expanded: Boolean = true,
    initialTimestamp: Long?,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        if (initialTimestamp != null) timeInMillis = initialTimestamp
    }
    val initialHour24 = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)

    var isAm by remember { mutableStateOf(initialHour24 < 12) }
    var hour by remember { mutableStateOf(if (initialHour24 % 12 == 0) 12 else initialHour24 % 12) }
    var minute by remember { mutableStateOf(initialMinute) }

    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Select Time"
    ) { closeAnd ->
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = listOf("AM", "PM"),
                    selectedIndex = if (isAm) 0 else 1,
                    onItemSelected = { isAm = (it == 0) },
                    selectedSize = 22f,
                    unselectedSize = 16f
                )

                WheelPicker(
                    items = (1..12).map { it.toString().padStart(2, '0') },
                    selectedIndex = hour - 1,
                    onItemSelected = { hour = it + 1 }
                )

                Text(
                    text = ":",
                    fontFamily = BricolageFont,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                WheelPicker(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onItemSelected = { minute = it }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = {
                    val finalHour = when {
                        isAm && hour == 12 -> 0
                        !isAm && hour < 12 -> hour + 12
                        else -> hour
                    }
                    closeAnd { onConfirm(finalHour, minute) }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Save",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    selectedSize: Float = 22f,
    unselectedSize: Float = 16f
) {
    val itemHeight = 44.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf -1

            val viewportCenter = layoutInfo.viewportEndOffset / 2
            val closestItem = visibleItemsInfo.minByOrNull {
                abs((it.offset + (it.size / 2)) - viewportCenter)
            }
            (closestItem?.index ?: 1) - 1
        }
    }

    LaunchedEffect(centerIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centerIndex in items.indices) {
            onItemSelected(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = Modifier
            .height(itemHeight * 3)
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(itemHeight)) }
        items(items.size) { index ->
            val isSelected = centerIndex == index

            val animatedFontSize by animateFloatAsState(
                targetValue = if (isSelected) selectedSize else unselectedSize,
                animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                label = "fontSize"
            )

            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(150),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontFamily = BricolageFont,
                    fontSize = animatedFontSize.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = animatedColor
                )
            }
        }
        item { Spacer(Modifier.height(itemHeight)) }
    }
}

private enum class DatePresetType { LATER_TODAY, TOMORROW, THIS_WEEKEND, NEXT_WEEK }
private enum class TimePresetType { IN_15_MINS, IN_1_HOUR, IN_3_HOURS, THIS_EVENING }

private fun getDatePresetTime(type: DatePresetType): Long {
    val cal = Calendar.getInstance()
    when (type) {
        DatePresetType.LATER_TODAY -> {
            cal.add(Calendar.HOUR_OF_DAY, 4)
        }
        DatePresetType.TOMORROW -> {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        DatePresetType.THIS_WEEKEND -> {
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                cal.add(Calendar.DATE, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        DatePresetType.NEXT_WEEK -> {
            do {
                cal.add(Calendar.DATE, 1)
            } while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
    }
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun getTimePreset(type: TimePresetType): Long {
    val cal = Calendar.getInstance()
    when (type) {
        TimePresetType.IN_15_MINS -> {
            cal.add(Calendar.MINUTE, 15)
        }
        TimePresetType.IN_1_HOUR -> {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        TimePresetType.IN_3_HOURS -> {
            cal.add(Calendar.HOUR_OF_DAY, 3)
        }
        TimePresetType.THIS_EVENING -> {
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 0)
        }
    }
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}