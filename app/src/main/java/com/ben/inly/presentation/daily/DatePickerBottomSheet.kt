package com.ben.inly.presentation.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.ui.theme.BricolageFont
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    selectedDate: LocalDate,
    onDateConfirmed: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var pendingDate by remember { mutableStateOf(selectedDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Fix #4: explicit scrim so it never vanishes on partial drag
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        // Fix #1: no gaps — surface fills edge to edge, shape only on top corners
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        // Fix #1: remove default window insets so sheet fills bottom edge cleanly
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
        }
    ) {
        // Fix #2: verticalScroll on the column so the calendar itself is scrollable
        // and doesn't fight the sheet's drag gesture
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Sheet label ──────────────────────────────────────────────
            Text(
                text = "Jump to date",
                fontFamily = BricolageFont,
                fontSize = 11.sp,  // SIZE_XXS — muted label
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // ── Month navigation header ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthNavButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    description = "Previous month",
                    onClick = { displayedMonth = displayedMonth.minusMonths(1) }
                )

                // Fix #3: tapping month/year toggles the picker panel
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showMonthYearPicker = !showMonthYearPicker }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayedMonth.month.getDisplayName(
                                java.time.format.TextStyle.FULL,
                                java.util.Locale.getDefault()
                            ),
                            fontFamily = BricolageFont,
                            fontSize = 20.sp,  // SIZE_XL
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = displayedMonth.year.toString(),
                            fontFamily = BricolageFont,
                            fontSize = 11.sp,  // SIZE_XXS
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Icon(
                        imageVector = if (showMonthYearPicker)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle month/year picker",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                MonthNavButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    description = "Next month",
                    onClick = { displayedMonth = displayedMonth.plusMonths(1) }
                )
            }

            // Fix #3: Month + Year picker panel
            AnimatedVisibility(
                visible = showMonthYearPicker,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = DampingRatioMediumBouncy,
                        stiffness = StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = DampingRatioMediumBouncy,
                        stiffness = StiffnessMediumLow
                    )
                ) + fadeOut()
            ) {
                MonthYearPickerPanel(
                    displayedMonth = displayedMonth,
                    onMonthYearSelected = { ym ->
                        displayedMonth = ym
                        showMonthYearPicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Day-of-week labels ───────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontFamily = BricolageFont,
                        fontSize = 13.sp,  // SIZE_SM
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Calendar grid ────────────────────────────────────────────
            val firstDayOfMonth = displayedMonth.atDay(1)
            val startOffset = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 0
            val daysInMonth = displayedMonth.lengthOfMonth()
            val rows = (startOffset + daysInMonth + 6) / 7

            val accentColor = MaterialTheme.colorScheme.onSurface
            val todayRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

            // Fix #2: Column of rows instead of nested scroll — plays nicely with
            // the outer verticalScroll and the sheet's drag gesture
            Column(modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        for (col in 0 until 7) {
                            val dayNumber = row * 7 + col - startOffset + 1

                            if (dayNumber < 1 || dayNumber > daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val date = displayedMonth.atDay(dayNumber)
                                val isSelected = date == pendingDate
                                val isToday = date == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) accentColor else MaterialTheme.colorScheme.surface
                                        )
                                        .then(
                                            if (isToday && !isSelected)
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = todayRingColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            else Modifier
                                        )
                                        .clickable { pendingDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontFamily = BricolageFont,
                                        fontSize = 15.sp,  // SIZE_MD
                                        fontWeight = if (isSelected || isToday) FontWeight.Medium else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.surface
                                            isToday -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick-jump chips ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickJumpChip(
                    label = "Today",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        pendingDate = today
                        displayedMonth = YearMonth.from(today)
                    }
                )
                QuickJumpChip(
                    label = "Yesterday",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val yesterday = today.minusDays(1)
                        pendingDate = yesterday
                        displayedMonth = YearMonth.from(yesterday)
                    }
                )
                QuickJumpChip(
                    label = "This week",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val monday = today.with(DayOfWeek.MONDAY)
                        pendingDate = monday
                        displayedMonth = YearMonth.from(monday)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Confirm button ───────────────────────────────────────────
            Button(
                onClick = { onDateConfirmed(pendingDate) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Go to ${formatConfirmLabel(pendingDate, today)}",
                    fontFamily = BricolageFont,
                    fontSize = 15.sp,  // SIZE_MD
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── MonthYearPickerPanel ─────────────────────────────────────────────────────

@Composable
private fun MonthYearPickerPanel(
    displayedMonth: YearMonth,
    onMonthYearSelected: (YearMonth) -> Unit
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    var selectedYear by remember { mutableStateOf(displayedMonth.year) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = DampingRatioMediumBouncy,
                    stiffness = StiffnessMediumLow
                )
            )
            .padding(top = 12.dp)
    ) {
        // Year row with prev/next arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthNavButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                description = "Previous year",
                onClick = { selectedYear-- }
            )
            Text(
                text = selectedYear.toString(),
                fontFamily = BricolageFont,
                fontSize = 17.sp,  // SIZE_LG
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            MonthNavButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                description = "Next year",
                onClick = { selectedYear++ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month grid — 4 columns × 3 rows
        val monthChunks = months.chunked(4)
        monthChunks.forEachIndexed { rowIdx, rowMonths ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowMonths.forEachIndexed { colIdx, monthLabel ->
                    val monthNumber = rowIdx * 4 + colIdx + 1
                    val isCurrentMonth = monthNumber == displayedMonth.monthValue &&
                            selectedYear == displayedMonth.year
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrentMonth)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .clickable {
                                onMonthYearSelected(YearMonth.of(selectedYear, monthNumber))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthLabel,
                            fontFamily = BricolageFont,
                            fontSize = 13.sp,  // SIZE_SM
                            fontWeight = if (isCurrentMonth) FontWeight.Medium else FontWeight.Normal,
                            color = if (isCurrentMonth)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            thickness = 1.dp
        )
    }
}

// ── MonthNavButton ───────────────────────────────────────────────────────────

@Composable
private fun MonthNavButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── QuickJumpChip ────────────────────────────────────────────────────────────

@Composable
private fun QuickJumpChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = BricolageFont,
            fontSize = 13.sp,  // SIZE_SM
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatConfirmLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
}