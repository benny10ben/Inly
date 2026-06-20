package com.ben.inly.presentation.shared.editor.blockViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.presentation.shared.editor.DefaultBlockShape
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioBlockView(
    block: VoiceBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRemoveVoice: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: (Boolean) -> Unit,
    onPlayAudio: (String, () -> Unit) -> Unit,
    onStopAudio: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalSteps = (block.durationSeconds * 10).coerceAtLeast(10)
            for (i in 0..totalSteps) {
                playProgress = i.toFloat() / totalSteps
                delay(100)
                if (!isPlaying) break
            }
            isPlaying = false
            playProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(DefaultBlockShape)
            .background(
                if (isRecording) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = onToggleSelection
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (block.localFilePath == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                if (!inSelectionMode) {
                                    if (isRecording) {
                                        isRecording = false
                                        onStopRecording(false)
                                    } else {
                                        isRecording = true
                                        onStartRecording()
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isRecording) {
                        val mins = recordingDuration / 60
                        val secs = recordingDuration % 60
                        Text(
                            text = "Recording... ${mins}:${secs.toString().padStart(2, '0')}",
                            fontFamily = PoppinsFont,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "Tap mic to record audio",
                            fontFamily = PoppinsFont,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                if (!inSelectionMode) {
                                    if (isPlaying) {
                                        isPlaying = false
                                        onStopAudio()
                                    } else {
                                        isPlaying = true
                                        block.localFilePath?.let { path ->
                                            onPlayAudio(path) {
                                                isPlaying = false
                                                playProgress = 0f
                                            }
                                        }
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val barActiveColor = MaterialTheme.colorScheme.onSurface
                    val barInactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 3.dp.toPx()
                            val gap = 2.dp.toPx()
                            val totalBars = (size.width / (barWidth + gap)).toInt()

                            for (i in 0 until totalBars) {
                                val barProgress = i.toFloat() / totalBars
                                val barColor = if (barProgress <= playProgress) barActiveColor else barInactiveColor

                                val randomHeight = ((block.id.hashCode() + i) % 100) / 100f
                                val barHeight = (size.height * 0.3f) + (size.height * 0.7f * randomHeight)

                                drawLine(
                                    color = barColor,
                                    start = Offset(i * (barWidth + gap), size.height / 2f - barHeight / 2f),
                                    end = Offset(i * (barWidth + gap), size.height / 2f + barHeight / 2f),
                                    strokeWidth = barWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }

                    val mins = block.durationSeconds / 60
                    val secs = block.durationSeconds % 60
                    Text(
                        text = "${mins}:${secs.toString().padStart(2, '0')}",
                        fontFamily = PoppinsFont,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(18.dp)
                        .clickable { if (!inSelectionMode) onRemoveVoice() }
                )
            }
        }
    }
}