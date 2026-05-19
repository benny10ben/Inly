package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.BricolageFont
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val PopupButtonShape = RoundedCornerShape(8.dp)
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
                text = { Text("Later today", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.Today, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("Tomorrow", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.Event, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("This weekend", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.Weekend, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("Next week", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.NextWeek, null) },
                onClick = { onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK)); onDismiss() }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom date...", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                onClick = { onCustomSelected(); onDismiss() }
            )
            if (onRemove != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
                DropdownMenuItem(
                    text = { Text("Remove reminder", fontFamily = BricolageFont, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onRemove(); onDismiss() }
                )
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Date") { closeAnd ->
            PresetSheetItem(Icons.Default.Today, "Later today") { closeAnd { onPresetSelected(getDatePresetTime(DatePresetType.LATER_TODAY)) } }
            PresetSheetItem(Icons.Default.Event, "Tomorrow") { closeAnd { onPresetSelected(getDatePresetTime(DatePresetType.TOMORROW)) } }
            PresetSheetItem(Icons.Default.Weekend, "This weekend") { closeAnd { onPresetSelected(getDatePresetTime(DatePresetType.THIS_WEEKEND)) } }
            PresetSheetItem(Icons.Default.NextWeek, "Next week") { closeAnd { onPresetSelected(getDatePresetTime(DatePresetType.NEXT_WEEK)) } }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            PresetSheetItem(Icons.Default.CalendarMonth, "Custom date...") { closeAnd { onCustomSelected() } }
            if (onRemove != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                PresetSheetItem(Icons.Default.NotificationsOff, "Remove reminder", isDestructive = true) { closeAnd { onRemove() } }
            }
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Close", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
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
                text = { Text("In 15 mins", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("In 1 hour", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("In 3 hours", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS)); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("This evening", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.NightsStay, null) },
                onClick = { onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING)); onDismiss() }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Custom time...", fontFamily = BricolageFont) },
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                onClick = { onCustomSelected(); onDismiss() }
            )
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Time") { closeAnd ->
            PresetSheetItem(Icons.Default.Timer, "In 15 mins") { closeAnd { onPresetSelected(getTimePreset(TimePresetType.IN_15_MINS)) } }
            PresetSheetItem(Icons.Default.Schedule, "In 1 hour") { closeAnd { onPresetSelected(getTimePreset(TimePresetType.IN_1_HOUR)) } }
            PresetSheetItem(Icons.Default.AccessTime, "In 3 hours") { closeAnd { onPresetSelected(getTimePreset(TimePresetType.IN_3_HOURS)) } }
            PresetSheetItem(Icons.Default.NightsStay, "This evening") { closeAnd { onPresetSelected(getTimePreset(TimePresetType.THIS_EVENING)) } }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            PresetSheetItem(Icons.Default.AccessTime, "Custom time...") { closeAnd { onCustomSelected() } }
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = PopupButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Close", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    }
}

@Composable
private fun PresetSheetItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontFamily = BricolageFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
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

    if (isDesktopPlatform) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(360.dp)) {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = currentDensity.density * 0.85f, fontScale = currentDensity.fontScale)) {
                DatePicker(state = datePickerState, showModeToggle = false, title = null, headline = null)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { datePickerState.selectedDateMillis?.let { onConfirm(it) }; onDismiss() }) { Text("Save") }
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Select Date") { closeAnd ->
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = currentDensity.density * 0.85f, fontScale = currentDensity.fontScale)) {
                DatePicker(state = datePickerState, showModeToggle = false, title = null, headline = null)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { closeAnd(onDismiss) }, modifier = Modifier.weight(1f).height(48.dp), shape = PopupButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                Button(onClick = { closeAnd { datePickerState.selectedDateMillis?.let { onConfirm(it) } } }, modifier = Modifier.weight(1f).height(48.dp), shape = PopupButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Save", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
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
                modifier = if (isDesktopPlatform) Modifier else Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(items = listOf("AM", "PM"), selectedIndex = if (isAm) 0 else 1, onItemSelected = { isAm = (it == 0) }, itemHeight = pickerItemHeight)
                Spacer(Modifier.width(8.dp))
                WheelPicker(items = (1..12).map { it.toString().padStart(2, '0') }, selectedIndex = hour - 1, onItemSelected = { hour = it + 1 }, itemHeight = pickerItemHeight)

                Text(
                    text = ":",
                    fontFamily = BricolageFont,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 4.dp).offset(y = (-4).dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                WheelPicker(items = (0..59).map { it.toString().padStart(2, '0') }, selectedIndex = minute, onItemSelected = { minute = it }, itemHeight = pickerItemHeight)
            }
        }
    }

    if (isDesktopPlatform) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            content()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = {
                    val finalHour = when { isAm && hour == 12 -> 0; !isAm && hour < 12 -> hour + 12; else -> hour }
                    onConfirm(finalHour, minute)
                    onDismiss()
                }) { Text("Save") }
            }
        }
    } else {
        InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Select Time") { closeAnd ->
            content()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { closeAnd(onDismiss) }, modifier = Modifier.weight(1f).height(48.dp), shape = PopupButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                Button(onClick = {
                    val finalHour = when { isAm && hour == 12 -> 0; !isAm && hour < 12 -> hour + 12; else -> hour }
                    closeAnd { onConfirm(finalHour, minute) }
                }, modifier = Modifier.weight(1f).height(48.dp), shape = PopupButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Save", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
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
    itemHeight: androidx.compose.ui.unit.Dp = 44.dp
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
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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