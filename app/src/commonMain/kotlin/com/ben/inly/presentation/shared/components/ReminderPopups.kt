package com.ben.inly.presentation.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val PopupButtonShape = RoundedCornerShape(12.dp)
private val DesktopMenuWidth = 240.dp

@Composable
fun ReminderPresetMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onCustomSelected: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    if (isDesktopPlatform) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(DesktopMenuWidth)
        ) {
            DropdownMenuItem(
                text = { Text("Later today", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.Today, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("Tomorrow", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.Event, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("This weekend", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.Weekend, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("Next week", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.NextWeek, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK)); onDismiss() }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom date...", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                onClick = { onCustomSelected(); onDismiss() }
            )
            if (onRemove != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
                DropdownMenuItem(
                    text = { Text("Remove reminder", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onRemove(); onDismiss() }
                )
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Date") {
            PresetSheetItem(Icons.Default.Today, "Later today") { onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY)); onDismiss() }
            PresetSheetItem(Icons.Default.Event, "Tomorrow") { onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW)); onDismiss() }
            PresetSheetItem(Icons.Default.Weekend, "This weekend") { onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND)); onDismiss() }
            PresetSheetItem(Icons.Default.NextWeek, "Next week") { onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK)); onDismiss() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            PresetSheetItem(Icons.Default.CalendarMonth, "Custom date...") { onCustomSelected(); onDismiss() }
            if (onRemove != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                PresetSheetItem(Icons.Default.NotificationsOff, "Remove reminder", isDestructive = true) { onRemove(); onDismiss() }
            }
            Button(
                onClick = { onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = PopupButtonShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Close", fontFamily = PoppinsFont, fontSize = 14.sp) }
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
    if (isDesktopPlatform) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(DesktopMenuWidth)
        ) {
            DropdownMenuItem(
                text = { Text("In 15 mins", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("In 1 hour", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("In 3 hours", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("This evening", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.NightsStay, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING)); onDismiss() }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom time...", fontFamily = PoppinsFont, fontWeight = FontWeight.Normal) },
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                onClick = { onCustomSelected(); onDismiss() }
            )
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Time") {
            PresetSheetItem(Icons.Default.Timer, "In 15 mins") { onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS)); onDismiss() }
            PresetSheetItem(Icons.Default.Schedule, "In 1 hour") { onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR)); onDismiss() }
            PresetSheetItem(Icons.Default.AccessTime, "In 3 hours") { onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS)); onDismiss() }
            PresetSheetItem(Icons.Default.NightsStay, "This evening") { onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING)); onDismiss() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            PresetSheetItem(Icons.Default.AccessTime, "Custom time...") { onCustomSelected(); onDismiss() }
            Button(
                onClick = { onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = PopupButtonShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Close", fontFamily = PoppinsFont, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun PresetSheetItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontFamily = PoppinsFont, fontSize = 14.sp, fontWeight = FontWeight.Normal, color = textColor)
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

    val customDatePickerColors = DatePickerDefaults.colors(
        containerColor = Color.Transparent,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dayContentColor = MaterialTheme.colorScheme.onSurface,
        weekdayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    )

    if (isDesktopPlatform) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.width(340.dp).height(460.dp)) {

                // Calendar Grid
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density = currentDensity.density * 0.85f, fontScale = currentDensity.fontScale)) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = customDatePickerColors,
                        modifier = Modifier.weight(1f).padding(top = 8.dp)
                    )
                }

                // Subtle separator
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                // Polished Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel", fontFamily = PoppinsFont, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { onConfirm(it) }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Save", fontFamily = PoppinsFont, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Select Date") {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = currentDensity.density * 0.85f, fontScale = currentDensity.fontScale)) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null,
                    colors = customDatePickerColors
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = PopupButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text("Cancel", fontFamily = PoppinsFont, fontSize = 14.sp) }

                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onConfirm(it) }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = PopupButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("Save", fontFamily = PoppinsFont, fontSize = 14.sp) }
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
    val cal = Calendar.getInstance().apply { if (initialTimestamp != null) timeInMillis = initialTimestamp }
    val initialHour24 = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)
    var isAm by remember { mutableStateOf(initialHour24 < 12) }
    var hour by remember { mutableStateOf(if (initialHour24 % 12 == 0) 12 else initialHour24 % 12) }
    var minute by remember { mutableStateOf(initialMinute) }

    val pickerItemHeight = if (isDesktopPlatform) 36.dp else 44.dp

    val content = @Composable {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(items = listOf("AM", "PM"), selectedIndex = if (isAm) 0 else 1, onItemSelected = { isAm = (it == 0) }, itemHeight = pickerItemHeight)
                Spacer(Modifier.width(8.dp))
                WheelPicker(items = (1..12).map { it.toString().padStart(2, '0') }, selectedIndex = hour - 1, onItemSelected = { hour = it + 1 }, itemHeight = pickerItemHeight)

                Text(
                    text = ":",
                    fontFamily = PoppinsFont,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 4.dp).offset(y = (-4).dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                WheelPicker(items = (0..59).map { it.toString().padStart(2, '0') }, selectedIndex = minute, onItemSelected = { minute = it }, itemHeight = pickerItemHeight)
            }
        }
    }

    if (isDesktopPlatform) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            content()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Medium) }
                TextButton(onClick = {
                    val finalHour = when { isAm && hour == 12 -> 0; !isAm && hour < 12 -> hour + 12; else -> hour }
                    onConfirm(finalHour, minute)
                    onDismiss()
                }) { Text("Save", fontWeight = FontWeight.Medium) }
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Select Time") {
            content()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = PopupButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text("Cancel", fontFamily = PoppinsFont, fontSize = 14.sp) }

                Button(
                    onClick = {
                        val finalHour = when { isAm && hour == 12 -> 0; !isAm && hour < 12 -> hour + 12; else -> hour }
                        onConfirm(finalHour, minute)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = PopupButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("Save", fontFamily = PoppinsFont, fontSize = 14.sp) }
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
    unselectedSize: Float = 16f,
    itemHeight: Dp = 44.dp
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf -1
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            val closestItem = visibleItemsInfo.minByOrNull { abs((it.offset + (it.size / 2)) - viewportCenter) }
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
            .width(if (isDesktopPlatform) 48.dp else 64.dp),
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
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = tween(150),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontFamily = PoppinsFont,
                    fontSize = animatedFontSize.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
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
        DatePresetType.LATER_TODAY -> cal.add(Calendar.HOUR_OF_DAY, 4)
        DatePresetType.TOMORROW -> { cal.add(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
        DatePresetType.THIS_WEEKEND -> { while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) { cal.add(Calendar.DATE, 1) }; cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
        DatePresetType.NEXT_WEEK -> { do { cal.add(Calendar.DATE, 1) } while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY); cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0) }
    }
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun getTimePreset(type: TimePresetType): Long {
    val cal = Calendar.getInstance()
    when (type) {
        TimePresetType.IN_15_MINS -> cal.add(Calendar.MINUTE, 15)
        TimePresetType.IN_1_HOUR -> cal.add(Calendar.HOUR_OF_DAY, 1)
        TimePresetType.IN_3_HOURS -> cal.add(Calendar.HOUR_OF_DAY, 3)
        TimePresetType.THIS_EVENING -> { cal.set(Calendar.HOUR_OF_DAY, 18); cal.set(Calendar.MINUTE, 0) }
    }
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}