package com.ben.inly.presentation.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.ui.theme.BricolageFont
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

// Change this value to adjust the roundness of the individual date cards
private val DateCardShape = RoundedCornerShape(6.dp)

/**
 * The horizontally scrolling calendar strip at the top of the daily screen.
 * It automatically snaps to the selected date and highlights the current day.
 */
@Composable
fun CalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var anchorDate by remember { mutableStateOf(selectedDate) }
    val today = remember { LocalDate.now() }

    LaunchedEffect(selectedDate) {
        val daysBetween = ChronoUnit.DAYS.between(anchorDate, selectedDate).absoluteValue
        if (daysBetween > 7) {
            anchorDate = selectedDate
        }
    }

    val dates = remember(anchorDate) {
        (-15..15).map { anchorDate.plusDays(it.toLong()) }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedDate, anchorDate) {
        val targetIndex = dates.indexOf(selectedDate)
        if (targetIndex != -1 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(maxOf(0, targetIndex - 4))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dates, key = { it.toEpochDay() }) { date ->
                val isSelected = date == selectedDate
                val isToday = date == today

                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                val mutedTextColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(42.dp)
                        .clip(DateCardShape)
                        .background(bgColor)
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                        fontSize = 10.sp,
                        fontFamily = BricolageFont,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = mutedTextColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontFamily = BricolageFont,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday && !isSelected) MaterialTheme.colorScheme.outline
                                else if (isToday && isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}