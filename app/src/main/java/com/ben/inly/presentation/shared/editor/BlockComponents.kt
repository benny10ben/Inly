package com.ben.inly.presentation.shared.editor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ben.inly.R
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.*
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.LocalInlyExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items

// Adjust this value to change the corner roundness for block containers
private val DefaultBlockShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteBlockItem(
    block: NoteBlock,
    globalTags: List<TagEntity>,
    actions: EditorActions,
    focusRequest: FocusRequest?,
    focusRequester: FocusRequester,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    isActiveBlock: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isHandlingEnter by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(density)

    val text = when (block) {
        is CodeBlock -> block.code
        is TextBlock -> block.text
        is HeadingBlock -> block.text
        is CheckboxBlock -> block.text
        is BulletedListBlock -> block.text
        is NumberedListBlock -> block.text
        is ToggleBlock -> block.text
        is BookmarkBlock, is ImageBlock, is DocumentBlock, is DatabaseBlock, is VoiceBlock -> ""
    }

    var textFieldValue by remember(block.id) { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }

    var showPresetMenu by remember { mutableStateOf(false) }
    var showTimePresetMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDateMillis by remember { mutableStateOf<Long?>(null) }

    var targetDbRowId by remember { mutableStateOf<String?>(null) }
    var targetDbColId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun checkNotificationPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else onGranted()
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            val bufferPx = with(density) { 100.dp.toPx() }
            val componentHeight = textLayoutResult?.size?.height?.toFloat() ?: 50f

            val targetRect = androidx.compose.ui.geometry.Rect(
                left = 0f,
                top = 0f,
                right = 1f,
                bottom = componentHeight + bufferPx
            )

            bringIntoViewRequester.bringIntoView(targetRect)
        }
    }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            if (!isFocused || kotlin.math.abs(textFieldValue.text.length - text.length) > 1) {
                val safeStart = textFieldValue.selection.start.coerceAtMost(text.length)
                val safeEnd = textFieldValue.selection.end.coerceAtMost(text.length)
                textFieldValue = textFieldValue.copy(
                    text = text,
                    selection = TextRange(safeStart, safeEnd)
                )
            }
        }
    }

    LaunchedEffect(focusRequest) {
        if (focusRequest != null && focusRequest.id == block.id && focusRequest.placeCursorAtEnd) {
            textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
        }
    }

    val baseStyle = when (block) {
        is HeadingBlock -> TextStyle(
            fontFamily = BricolageFont,
            fontSize = if (block.level == 1) 26.sp else 20.sp,
            lineHeight = if (block.level == 1) 32.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        is CodeBlock -> TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> TextStyle(
            fontFamily = BricolageFont,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    val isCheckboxChecked = block is CheckboxBlock && block.isChecked
    val applyStrikeThrough = block.isStrikeThrough || isCheckboxChecked

    val textStyle = baseStyle.copy(
        fontWeight = if (block.isBold) FontWeight.Bold else baseStyle.fontWeight,
        fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            applyStrikeThrough && block.isUnderlined -> TextDecoration.LineThrough + TextDecoration.Underline
            applyStrikeThrough -> TextDecoration.LineThrough
            block.isUnderlined -> TextDecoration.Underline
            else -> TextDecoration.None
        },
        color = if (isCheckboxChecked) MaterialTheme.colorScheme.outline else baseStyle.color
    )

    val internalVerticalPadding = when (block) {
        is HeadingBlock -> 8.dp
        is CodeBlock -> 4.dp
        else -> 4.dp
    }

    val selectionBg = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    )

    val isTextBased = block !is BookmarkBlock && block !is ImageBlock && block !is DocumentBlock && block !is DatabaseBlock && block !is VoiceBlock
    val isDatabase = block is DatabaseBlock

    val startPadding = if (isDatabase) (block.indentationLevel * 28).dp else (20 + (block.indentationLevel * 28)).dp
    val endPadding = if (isDatabase) 0.dp else 24.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(selectionBg)
            .combinedClickable(
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = { actions.onToggleSelection(block.id) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .padding(vertical = internalVerticalPadding),
            verticalAlignment = Alignment.Top
        ) {

            val iconOffset = when (block) {
                is HeadingBlock -> if (block.level == 1) 4.dp else 2.dp
                is CodeBlock -> 12.dp
                else -> (-2).dp
            }

            if (block is CheckboxBlock || block is BulletedListBlock || block is NumberedListBlock || block is ToggleBlock) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .offset(y = iconOffset)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (block) {
                        is CheckboxBlock -> CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides 0.dp
                        ) {
                            Checkbox(
                                checked = block.isChecked,
                                onCheckedChange = { actions.onToggleCheckbox(block.id, it) },
                                modifier = Modifier
                                    .scale(0.9f)
                                    .size(16.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.surface,
                                    checkmarkColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        is BulletedListBlock -> Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape)
                                .background(textStyle.color)
                        )

                        is NumberedListBlock -> Text(
                            "${block.number}.",
                            style = textStyle.copy(fontSize = 17.sp)
                        )

                        is ToggleBlock -> {
                            val rotation by animateFloatAsState(
                                if (block.isExpanded) 90f else 0f,
                                label = "toggleRotation"
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).rotate(rotation)
                                    .clickable { actions.onToggleExpand(block.id) })
                        }

                        else -> {}
                    }
                }
            }

            val textFieldWrapperModifier = if (block is CodeBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, DefaultBlockShape)
                    .padding(12.dp)
            } else if (isDatabase) {
                Modifier.weight(1f)
            } else {
                Modifier.weight(1f).padding(horizontal = 4.dp)
            }

            Column(modifier = textFieldWrapperModifier) {
                if (isTextBased) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                            BasicTextField(
                                value = textFieldValue,
                                onTextLayout = { textLayoutResult = it },
                                onValueChange = { newValue ->
                                    val newText = newValue.text

                                    if (block !is CodeBlock && newText.contains('\n')) {
                                        if (!isHandlingEnter) {
                                            isHandlingEnter = true
                                            val splitIndex = newText.indexOf('\n')
                                            val textBefore = newText.substring(0, splitIndex)
                                            val textAfter = newText.substring(splitIndex + 1)
                                            textFieldValue = TextFieldValue(textBefore, TextRange(textBefore.length))
                                            actions.onEnterPressed(block.id, textBefore, textAfter)
                                            scope.launch {
                                                delay(50)
                                                isHandlingEnter = false
                                            }
                                        }
                                    } else {
                                        textFieldValue = newValue
                                        actions.onUpdateText(block.id, newText)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .onFocusChanged {
                                        isFocused = it.isFocused
                                        if (it.isFocused) {
                                            onFocus()
                                        }
                                    }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && textFieldValue.text.isEmpty()) {
                                            actions.onBackspaceOnEmpty(block.id)
                                            true
                                        } else false
                                    },
                                textStyle = textStyle,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                enabled = !inSelectionMode
                            )
                        }

                        if (!isFocused && !inSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(block.id) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                val position = textLayoutResult
                                                    ?.getOffsetForPosition(offset)
                                                    ?: textFieldValue.text.length
                                                textFieldValue = textFieldValue.copy(
                                                    selection = TextRange(position)
                                                )
                                                focusRequester.requestFocus()
                                            },
                                            onLongPress = { actions.onToggleSelection(block.id) }
                                        )
                                    }
                            )
                        }
                    }

                    if (block is CheckboxBlock) {
                        val hasReminder = block.reminderTimestamp != null

                        AnimatedVisibility(
                            visible = isActiveBlock || hasReminder,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                checkNotificationPermission {
                                                    val keyboardWasOpen = isKeyboardOpen
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    scope.launch {
                                                        if (keyboardWasOpen) delay(500L) else delay(50L)
                                                        tempDateMillis = null
                                                        showPresetMenu = true
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            "Date",
                                            modifier = Modifier.size(15.dp),
                                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    ReminderPresetMenu(
                                        expanded = showPresetMenu,
                                        onDismiss = { showPresetMenu = false },
                                        onPresetSelected = { actions.onUpdateReminder(block.id, it) },
                                        onCustomSelected = { showDatePicker = true },
                                        onRemove = if (hasReminder) {
                                            { actions.onUpdateReminder(block.id, null) }
                                        } else null
                                    )
                                }
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                checkNotificationPermission {
                                                    val keyboardWasOpen = isKeyboardOpen
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    scope.launch {
                                                        if (keyboardWasOpen) delay(500L) else delay(50L)
                                                        showTimePresetMenu = true
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            "Time",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TimePresetMenu(
                                        expanded = showTimePresetMenu,
                                        onDismiss = { showTimePresetMenu = false },
                                        onPresetSelected = { actions.onUpdateReminder(block.id, it) },
                                        onCustomSelected = { showTimePicker = true }
                                    )
                                }
                                if (hasReminder) {
                                    val timeText = remember(block.reminderTimestamp) {
                                        SimpleDateFormat(
                                            "MMM d, h:mm a",
                                            Locale.getDefault()
                                        ).format(Date(block.reminderTimestamp!!))
                                    }
                                    Text(
                                        text = timeText,
                                        fontFamily = BricolageFont,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (block) {
                        is BookmarkBlock -> BookmarkBlockView(block, inSelectionMode, { actions.onToggleSelection(block.id) }, { actions.onUrlSubmit(block.id, it) })
                        is ImageBlock -> ImageBlockView(block, inSelectionMode, { actions.onToggleSelection(block.id) }, { actions.onImagePicked(block.id, it) }, { actions.onDeleteImageBlock(block.id) })
                        is DocumentBlock -> DocumentBlockView(block, inSelectionMode, { actions.onToggleSelection(block.id) }, { actions.onDocumentPicked(block.id, it) })
                        is DatabaseBlock -> DatabaseBlockView(block, inSelectionMode, globalTags, actions)
                        is VoiceBlock -> VoiceBlockView(block, inSelectionMode, { actions.onToggleSelection(block.id) }, { p, d -> actions.onVoiceRecorded(block.id, p, d) }, { actions.onRemoveVoice(block.id) })
                        else -> {}
                    }
                }
            }
        }

        if (block is CheckboxBlock) {
            if (showDatePicker) {
                MinimalDatePickerDialog(
                    initialTimestamp = block.reminderTimestamp,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { timestamp ->
                        actions.onUpdateReminder(block.id, timestamp)
                        showDatePicker = false
                    }
                )
            }

            if (showTimePicker) {
                MinimalTimePickerDialog(
                    initialTimestamp = block.reminderTimestamp,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = block.reminderTimestamp ?: System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        actions.onUpdateReminder(block.id, cal.timeInMillis)
                        showTimePicker = false
                    }
                )
            }
        }

        if (inSelectionMode) Box(Modifier.matchParentSize().clickable(onClick = { actions.onToggleSelection(block.id) }))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceBlockView(
    block: VoiceBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRecorded: (String, Int) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val audioHelper = remember { com.ben.inly.domain.util.AudioHelper(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    if (block.localFilePath != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = { if (inSelectionMode) onToggleSelection() },
                    onLongClick = onToggleSelection
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !inSelectionMode) {
                            if (isPlaying) {
                                audioHelper.stopPlaying()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                audioHelper.play(block.localFilePath) { isPlaying = false }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = String.format("%02d:%02d", block.durationSeconds / 60, block.durationSeconds % 60),
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (!inSelectionMode) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp).clickable { onRemove() }
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(DefaultBlockShape)
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = {
                    if (inSelectionMode) {
                        onToggleSelection()
                    } else if (!isRecording) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isRecording = true
                            audioHelper.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        isRecording = false
                        val result = audioHelper.stopRecording(cancel = false)
                        if (result != null) onRecorded(result.first, result.second)
                    }
                },
                onLongClick = onToggleSelection
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (isRecording) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = String.format("Recording %02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Cancel",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .clickable(enabled = !inSelectionMode) {
                            isRecording = false
                            audioHelper.stopRecording(cancel = true)
                        }
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Record a voice note",
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkBlockView(
    block: BookmarkBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(block.url, block.title) {
        if (block.title == "Loading preview...") {
            onSubmit(block.url)
        }
    }

    if (block.url.isBlank() || block.title == null) {
        var urlInput by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = { if (inSelectionMode) onToggleSelection() },
                    onLongClick = onToggleSelection
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (urlInput.isNotBlank()) onSubmit(urlInput) }),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                            if (urlInput.isEmpty()) {
                                Text("Paste a link…", fontFamily = BricolageFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            inner()
                        }
                    }
                }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) {
                            onToggleSelection()
                        } else {
                            try { uriHandler.openUri(block.url) }
                            catch (e: Exception) { e.printStackTrace() }
                        }
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            if (block.previewImageUrl != null) {
                AsyncImage(
                    model = block.previewImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 48.dp,
                        top = if (block.previewImageUrl != null) 148.dp else 14.dp,
                        bottom = 14.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = block.title ?: "Link",
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!block.description.isNullOrBlank()) {
                    Text(
                        text = block.description,
                        fontFamily = BricolageFont,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = block.url,
                    fontFamily = BricolageFont,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            val faviconUrl = remember(block.url) {
                try {
                    val host = Uri.parse(block.url).host ?: return@remember null
                    "https://www.google.com/s2/favicons?sz=64&domain=$host"
                } catch (e: Exception) { null }
            }
            if (faviconUrl != null) {
                AsyncImage(
                    model = faviconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(22.dp)
                        .clip(DefaultBlockShape)
                )
            }
        }
    }
}

fun downloadImageToGallery(context: android.content.Context, localFilePath: String) {
    try {
        val file = File(context.filesDir, localFilePath)
        if (!file.exists()) {
            Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show()
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Inly_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Inly")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlockView(
    block: ImageBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onDelete: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    val context = LocalContext.current
    var showFullScreen by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onImagePicked(it) }
    }

    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onLongClick = onToggleSelection
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("Add image", fontFamily = BricolageFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(max = 260.dp)
                .clip(DefaultBlockShape)
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else showFullScreen = true
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            AsyncImage(
                model = File(context.filesDir, block.localFilePath),
                contentDescription = "Note Image",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
        if (showFullScreen) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val isDark = LocalAppIsDark.current
            val pillColor = LocalInlyExtendedColors.current.variant1.copy(alpha = 0.45f)
            val tint = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

            Dialog(
                onDismissRequest = { showFullScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                val dialogHazeState = remember { HazeState() }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(state = dialogHazeState)
                    ) {
                        AsyncImage(
                            model = File(context.filesDir, block.localFilePath),
                            contentDescription = "Full Screen Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else scale = 2.5f
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val maxX = (size.width * (scale - 1)) / 2
                                            val maxY = (size.height * (scale - 1)) / 2
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        } else offset = Offset.Zero
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 18.dp, start = 18.dp, end = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                                .clip(CircleShape)
                                .clickable { showFullScreen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.chevron_left),
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        shape = DefaultBlockShape,
                        color = pillColor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                            .clip(DefaultBlockShape)
                            .hazeChild(state = dialogHazeState)
                    ) {
                        val divider = @Composable { Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f))) }

                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            val iconSize = 18.dp

                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(iconSize).clickable {
                                    block.localFilePath?.let { path ->
                                        downloadImageToGallery(context, path)
                                    }
                                },
                                tint = tint
                            )
                            divider()
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(iconSize).clickable { onDelete(); showFullScreen = false }, tint = tint)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentBlockView(
    block: DocumentBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onFilePicked: (Uri) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onFilePicked(it) }
    }

    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else launcher.launch("*/*")
                    },
                    onLongClick = onToggleSelection
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("Attach a file", fontFamily = BricolageFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = {
                        if (inSelectionMode) {
                            onToggleSelection()
                        } else {
                            try {
                                val file = File(context.filesDir, block.localFilePath)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, block.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open Document"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open document. Ensure FileProvider is setup.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 48.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = block.fileName,
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = block.fileSizeString,
                    fontFamily = BricolageFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(18.dp)
            )
        }
    }
}

// --- DATABASE COMPONENTS ---

enum class DbSheetType { NONE, COLUMN_OPTIONS, RENAME, FORMULA, FILTER, SORT, CELL_OPTIONS, TAG_SELECTION, FILE_OPTIONS, PRIORITY_SELECTION }

@Composable
private fun DbOptionRow(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = BricolageFont, fontSize = 15.sp, color = color)
    }
}

@Composable
private fun DbOptionRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = BricolageFont, fontSize = 15.sp, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseBlockView(
    block: DatabaseBlock,
    inSelectionMode: Boolean,
    globalTags: List<TagEntity>,
    actions: EditorActions
) {
    val hazeState = remember { HazeState() }
    val scrollState = rememberScrollState()
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val focusManager = LocalFocusManager.current

    var currentSheet by remember { mutableStateOf(DbSheetType.NONE) }

    var activeColId by remember { mutableStateOf<String?>(null) }
    var activeRowId by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var filterOperator by remember { mutableStateOf("contains") }
    var filterPriority by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty() && activeRowId != null && activeColId != null) {
            uris.forEach {
                try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(e:Exception){}
            }

            val row = block.rows.find { it.id == activeRowId }
            val currentFiles = row?.cells?.get(activeColId)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            val combined = (currentFiles + uris.map { it.toString() }).joinToString(",")
            actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, combined)
        }
    }

    fun closeSheet(action: () -> Unit = {}) {
        currentSheet = DbSheetType.NONE
        activeRowId = null
        action()
    }

    val visibleRows = remember(block.rows, block.activeSorts, block.activeFilters) {
        var result = block.rows

        block.activeFilters.forEach { filter ->
            result = result.filter { row ->
                val cellVal = row.cells[filter.columnId] ?: ""
                when (filter.operator) {
                    "contains"  -> cellVal.contains(filter.value, ignoreCase = true)
                    "equals"    -> cellVal.equals(filter.value, ignoreCase = true)
                    "not_empty" -> cellVal.isNotEmpty()
                    "empty"     -> cellVal.isEmpty()
                    "checked"   -> cellVal == "true"
                    "unchecked" -> cellVal != "true"
                    "gt"        -> (cellVal.toDoubleOrNull() ?: 0.0) > (filter.value.toDoubleOrNull() ?: 0.0)
                    "lt"        -> (cellVal.toDoubleOrNull() ?: 0.0) < (filter.value.toDoubleOrNull() ?: 0.0)
                    "priority"  -> cellVal.equals(filter.value, ignoreCase = true)
                    else        -> true
                }
            }
        }

        block.activeSorts.firstOrNull()?.let { sort ->
            val colType = block.columns.find { it.id == sort.columnId }?.type
            result = if (colType == ColumnType.NUMBER) {
                if (sort.isAscending) result.sortedBy<DatabaseRow, Double> { it.cells[sort.columnId]?.toDoubleOrNull() ?: Double.MAX_VALUE }
                else result.sortedByDescending<DatabaseRow, Double> { it.cells[sort.columnId]?.toDoubleOrNull() ?: Double.MIN_VALUE }
            } else {
                if (sort.isAscending) result.sortedBy<DatabaseRow, String> { it.cells[sort.columnId]?.lowercase() ?: "" }
                else result.sortedByDescending<DatabaseRow, String> { it.cells[sort.columnId]?.lowercase() ?: "" }
            }
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .combinedClickable(
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = { actions.onToggleSelection(block.id) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = block.title,
                onValueChange = { actions.onUpdateDbTitle(block.id, it) },
                textStyle = TextStyle(
                    fontFamily = BricolageFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { inner ->
                    if (block.title.isEmpty()) {
                        Text(
                            text = "Untitled Database",
                            fontFamily = BricolageFont,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    inner()
                },
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasSort = block.activeSorts.isNotEmpty()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasSort) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier.clickable {
                        if (block.columns.isNotEmpty()) {
                            activeColId = block.activeSorts.firstOrNull()?.columnId ?: block.columns.first().id
                            currentSheet = DbSheetType.SORT
                        }
                    }
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        val sortIcon = if (block.activeSorts.firstOrNull()?.isAscending == false) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                        Icon(
                            painter = painterResource(R.drawable.arrow_down_up),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hasSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val hasFilter = block.activeFilters.isNotEmpty()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier.clickable {
                        if (block.columns.isNotEmpty()) {
                            activeColId = block.columns.first().id
                            textInput = ""
                            filterOperator = "contains"
                            currentSheet = DbSheetType.FILTER
                        }
                    }
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.sliders_horizontal),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hasFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (block.activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.activeFilters.forEach { filter ->
                    val colName = block.columns.find { it.id == filter.columnId }?.name ?: "?"
                    val label = when (filter.operator) {
                        "not_empty" -> "$colName is not empty"
                        "empty"     -> "$colName is empty"
                        "checked"   -> "$colName is checked"
                        "unchecked" -> "$colName is unchecked"
                        "priority"  -> "$colName = ${filter.value}"
                        "gt"        -> "$colName > ${filter.value}"
                        "lt"        -> "$colName < ${filter.value}"
                        else        -> "$colName ${filter.operator} \"${filter.value}\""
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontFamily = BricolageFont,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp).clickable { actions.onRemoveDbFilter(block.id, filter) },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .haze(state = hazeState)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, borderColor),
                    color = Color.Transparent
                ) {
                    Column {
                        Row(modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .height(IntrinsicSize.Max)
                            .defaultMinSize(minHeight = 44.dp)
                        ) {
                            block.columns.forEachIndexed { index, col ->
                                val activeSort = block.activeSorts.find { it.columnId == col.id }
                                val typeIcon = when (col.type) {
                                    ColumnType.TEXT -> Icons.Default.Subject
                                    ColumnType.NUMBER -> Icons.Default.Numbers
                                    ColumnType.CHECKBOX -> Icons.Default.CheckBox
                                    ColumnType.DATE -> Icons.Default.CalendarToday
                                    ColumnType.FORMULA -> Icons.Default.Functions
                                    ColumnType.PHONE -> Icons.Default.Phone
                                    ColumnType.EMAIL -> Icons.Default.Email
                                    ColumnType.TAGS -> Icons.Default.LocalOffer
                                    ColumnType.URL -> Icons.Default.Link
                                    ColumnType.FILES -> Icons.Default.AttachFile
                                    ColumnType.PRIORITY -> Icons.Default.Flag
                                }
                                Box(
                                    modifier = Modifier
                                        .width(col.width.dp)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .drawBehind {
                                            val stroke = 0.5.dp.toPx()
                                            drawLine(color = borderColor, start = Offset(size.width, 0f), end = Offset(size.width, size.height), strokeWidth = stroke)
                                            drawLine(color = borderColor, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = stroke)
                                        }
                                        .clickable {
                                            activeColId = col.id
                                            currentSheet = DbSheetType.COLUMN_OPTIONS
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = typeIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            text = col.name,
                                            fontFamily = BricolageFont,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (activeSort != null) {
                                            Icon(
                                                painter = if (activeSort.isAscending) painterResource(R.drawable.move_up) else painterResource(R.drawable.move_down),
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .drawBehind { drawLine(color = borderColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 0.5.dp.toPx()) }
                                    .clickable { actions.onAddDbColumn(block.id) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }

                        visibleRows.forEach { row ->
                            Row(modifier = Modifier
                                .height(IntrinsicSize.Max)
                                .defaultMinSize(minHeight = 44.dp)
                            ) {
                                block.columns.forEach { col ->
                                    val cellValue = row.cells[col.id] ?: ""
                                    val isHighlighted = currentSheet == DbSheetType.CELL_OPTIONS && activeRowId == row.id && activeColId == col.id

                                    Box(
                                        modifier = Modifier
                                            .width(col.width.dp)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 44.dp)
                                            .drawBehind {
                                                val stroke = 0.5.dp.toPx()
                                                drawLine(color = borderColor, start = Offset(size.width, 0f), end = Offset(size.width, size.height), strokeWidth = stroke)
                                                drawLine(color = borderColor, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = stroke)
                                            }
                                            .then(
                                                if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                                else Modifier
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.CELL_OPTIONS
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        TableCell(
                                            value = cellValue,
                                            columnType = col.type,
                                            cellWidth = col.width.dp,
                                            globalTags = globalTags,
                                            onValueChange = { actions.onUpdateDbCell(block.id, row.id, col.id, it) },
                                            onDateClick = {
                                                activeRowId = row.id
                                                activeColId = col.id
                                                showDatePicker = true
                                            },
                                            onTagClick = {
                                                focusManager.clearFocus()
                                                activeRowId = row.id
                                                activeColId = col.id
                                                currentSheet = DbSheetType.TAG_SELECTION
                                            },
                                            onFileClick = {
                                                focusManager.clearFocus()
                                                activeRowId = row.id
                                                activeColId = col.id
                                                currentSheet = DbSheetType.FILE_OPTIONS
                                            },
                                            onPriorityClick = {
                                                focusManager.clearFocus()
                                                activeRowId = row.id
                                                activeColId = col.id
                                                currentSheet = DbSheetType.PRIORITY_SELECTION
                                            },
                                            onLongPress = {
                                                focusManager.clearFocus()
                                                activeRowId = row.id
                                                activeColId = col.id
                                                currentSheet = DbSheetType.CELL_OPTIONS
                                            }
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .drawBehind {
                                            val stroke = 0.5.dp.toPx()
                                            drawLine(color = borderColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = stroke)
                                            drawLine(color = borderColor, start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = stroke)
                                        }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { actions.onAddDbRow(block.id) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "New Row",
                        fontFamily = BricolageFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDatePicker && activeRowId != null && activeColId != null) {
        MinimalDatePickerDialog(
            initialTimestamp = null,
            onDismiss = { showDatePicker = false },
            onConfirm = { timestamp ->
                val dateString = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, dateString)
                showDatePicker = false
                activeRowId = null
                activeColId = null
            }
        )
    }

    val sheetTitle = when (currentSheet) {
        DbSheetType.CELL_OPTIONS -> "Cell Actions"
        DbSheetType.COLUMN_OPTIONS -> block.columns.find { it.id == activeColId }?.name ?: "Column Options"
        DbSheetType.RENAME -> "Rename Column"
        DbSheetType.FORMULA -> "Edit Formula"
        DbSheetType.SORT -> "Sort"
        DbSheetType.FILTER -> "Add Filter"
        DbSheetType.NONE -> ""
        DbSheetType.FILE_OPTIONS -> "Attached Files"
        DbSheetType.PRIORITY_SELECTION -> "Set Priority"
        else -> ""
    }

    InlyBottomSheet(
        expanded = currentSheet != DbSheetType.NONE,
        onDismiss = { closeSheet() },
        title = sheetTitle
    ) { closeAnd ->
        when (currentSheet) {
            DbSheetType.CELL_OPTIONS -> {
                val col = block.columns.find { it.id == activeColId }
                val row = block.rows.find { it.id == activeRowId }
                if (col == null || row == null) return@InlyBottomSheet

                val colIndex = block.columns.indexOf(col)
                val rowIndex = block.rows.indexOf(row)

                DbOptionRow(Icons.Default.ArrowUpward, "Insert Row Above") { closeAnd { actions.onAddDbRowAt(block.id, rowIndex) } }
                DbOptionRow(Icons.Default.ArrowDownward, "Insert Row Below") { closeAnd { actions.onAddDbRowAt(block.id, rowIndex + 1) } }
                DbOptionRow(Icons.Default.ArrowBack, "Insert Column Left") { closeAnd { actions.onAddDbColumnAt(block.id, colIndex) } }
                DbOptionRow(Icons.Default.ArrowForward, "Insert Column Right") { closeAnd { actions.onAddDbColumnAt(block.id, colIndex + 1) } }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                DbOptionRow(Icons.Default.Delete, "Delete Row", MaterialTheme.colorScheme.error) { closeAnd { actions.onDeleteDbRow(block.id, row.id) } }
                DbOptionRow(Icons.Default.DeleteSweep, "Delete Column", MaterialTheme.colorScheme.error) { closeAnd { actions.onDeleteDbColumn(block.id, col.id) } }
            }

            DbSheetType.COLUMN_OPTIONS -> {
                val col = block.columns.find { it.id == activeColId } ?: return@InlyBottomSheet
                val colIndex = block.columns.indexOf(col)

                DbOptionRow(Icons.Default.Edit, "Rename Column") {
                    textInput = col.name
                    currentSheet = DbSheetType.RENAME
                }

                if (col.type == ColumnType.FORMULA) {
                    DbOptionRow(
                        icon = Icons.Default.Functions,
                        text = "Edit Formula",
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        textInput = col.formulaExpression ?: ""
                        currentSheet = DbSheetType.FORMULA
                    }
                }

                if (colIndex > 0) {
                    DbOptionRow(Icons.Default.ArrowBack, "Move Left") {
                        closeAnd { actions.onReorderDbColumns(block.id, colIndex, colIndex - 1) }
                    }
                }

                if (colIndex < block.columns.lastIndex) {
                    DbOptionRow(Icons.Default.ArrowForward, "Move Right") {
                        closeAnd { actions.onReorderDbColumns(block.id, colIndex, colIndex + 1) }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Text(
                    text = "Column Width",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { actions.onUpdateDbColumnWidth(block.id, col.id, col.width - 20) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${col.width} px",
                        fontFamily = BricolageFont,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(min = 50.dp),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { actions.onUpdateDbColumnWidth(block.id, col.id, col.width + 20) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Text(
                    text = "Property Type",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 12.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    ColumnType.entries.forEach { type ->
                        val isSelected = col.type == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { closeAnd { actions.onUpdateDbColumn(block.id, col.id, col.name, type) } },
                            label = {
                                Text(
                                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontFamily = BricolageFont,
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                DbOptionRow(
                    icon = Icons.Default.Delete,
                    text = "Delete Column",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { closeAnd { actions.onDeleteDbColumn(block.id, col.id) } }
                )
            }

            DbSheetType.RENAME -> {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 15.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { closeAnd(::closeSheet) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Cancel", fontFamily = BricolageFont, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val c = block.columns.find { it.id == activeColId }
                            if (c != null && textInput.isNotBlank()) {
                                closeAnd { actions.onUpdateDbColumn(block.id, c.id, textInput.trim(), c.type) }
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Save", fontFamily = BricolageFont, fontSize = 14.sp)
                    }
                }
            }

            DbSheetType.FORMULA -> {
                Text(
                    text = "Properties",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                ) {
                    block.columns.filter { it.id != activeColId }.forEach { c ->
                        SuggestionChip(
                            onClick = { textInput += "prop(\"${c.name}\") " },
                            label = { Text(c.name, fontFamily = BricolageFont, fontSize = 13.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(0.dp, Color.Transparent)
                        )
                    }
                }

                Text(
                    text = "Operators",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                ) {
                    listOf("+", "-", "*", "/", "(", ")").forEach { op ->
                        SuggestionChip(
                            onClick = { textInput += "$op " },
                            label = { Text(op, fontFamily = BricolageFont, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(0.dp, Color.Transparent)
                        )
                    }
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g. prop(\"Price\") * 2", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 15.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { closeAnd(::closeSheet) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Cancel", fontFamily = BricolageFont, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { closeAnd { actions.onUpdateDbFormula(block.id, activeColId!!, textInput.trim()) } },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Save", fontFamily = BricolageFont, fontSize = 14.sp)
                    }
                }
            }

            DbSheetType.SORT -> {
                Text(
                    text = "Column",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable {
                        val idx = block.columns.indexOfFirst { it.id == activeColId }
                        activeColId = block.columns[(idx + 1) % block.columns.size].id
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = block.columns.find { it.id == activeColId }?.name ?: "",
                            fontFamily = BricolageFont,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                DbOptionRow(R.drawable.move_up, "Ascending A → Z") {
                    closeAnd { actions.onUpdateDbSort(block.id, activeColId!!, true) }
                }

                DbOptionRow(R.drawable.move_down, "Descending Z → A") {
                    closeAnd { actions.onUpdateDbSort(block.id, activeColId!!, false) }
                }

                if (block.activeSorts.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    DbOptionRow(
                        icon = Icons.Default.Close,
                        text = "Remove Sort",
                        color = MaterialTheme.colorScheme.error
                    ) {
                        closeAnd { actions.onUpdateDbSort(block.id, block.activeSorts.first().columnId, null) }
                    }
                }
            }

            DbSheetType.FILTER -> {
                val activeCol = block.columns.find { it.id == activeColId }
                val isCheckbox = activeCol?.type == ColumnType.CHECKBOX
                val isNumber = activeCol?.type == ColumnType.NUMBER

                Text(
                    text = "Column",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable {
                        val idx = block.columns.indexOfFirst { it.id == activeColId }
                        activeColId = block.columns[(idx + 1) % block.columns.size].id
                        filterOperator = "contains"
                        textInput = ""
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeCol?.name ?: "",
                            fontFamily = BricolageFont,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Condition",
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 14.dp,
                        bottom = 8.dp
                    )
                )

                val operatorOptions: List<Pair<String, String>> = when {
                    isCheckbox -> listOf("checked" to "Is checked", "unchecked" to "Is unchecked")
                    isNumber -> listOf(
                        "equals" to "Equals",
                        "gt" to "Greater than",
                        "lt" to "Less than",
                        "not_empty" to "Is not empty",
                        "empty" to "Is empty"
                    )

                    else -> listOf(
                        "contains" to "Contains",
                        "equals" to "Equals",
                        "not_empty" to "Is not empty",
                        "empty" to "Is empty",
                        "priority" to "Priority is"
                    )
                }
                if (operatorOptions.none { it.first == filterOperator }) filterOperator =
                    operatorOptions.first().first

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                ) {
                    operatorOptions.forEach { (op, label) ->
                        val isSelected = filterOperator == op
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                filterOperator = op
                                textInput = ""
                            },
                            label = { Text(label, fontFamily = BricolageFont, fontSize = 13.sp) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.5f
                                )
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                val needsTextInput = filterOperator in listOf("contains", "equals", "gt", "lt")
                val needsPriorityPicker = filterOperator == "priority"

                if (needsTextInput) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isNumber) "Enter number…" else "Enter text…",
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 15.sp),
                        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (needsPriorityPicker) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Priority level",
                        fontFamily = BricolageFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                            val isSelected = filterPriority == p
                            val chipColor = when (p) {
                                "Urgent" -> MaterialTheme.colorScheme.error
                                "High" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    filterPriority = p
                                    textInput = p
                                },
                                label = { Text(p, fontFamily = BricolageFont, fontSize = 13.sp) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { closeAnd(::closeSheet) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Cancel", fontFamily = BricolageFont, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val canApply = when {
                                isCheckbox -> true
                                filterOperator in listOf("not_empty", "empty") -> true
                                filterOperator == "priority" -> filterPriority.isNotBlank()
                                else -> textInput.isNotBlank()
                            }
                            if (canApply) closeAnd {
                                actions.onAddDbFilter(
                                    block.id,
                                    activeColId!!,
                                    filterOperator,
                                    if (filterOperator == "priority") filterPriority.trim() else textInput.trim()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Apply", fontFamily = BricolageFont, fontSize = 14.sp)
                    }
                }
            }

            DbSheetType.TAG_SELECTION -> {
                var tagSearchQuery by remember { mutableStateOf("") }
                val row = block.rows.find { it.id == activeRowId } ?: return@InlyBottomSheet
                val currentTagIds = row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()

                OutlinedTextField(
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    placeholder = { Text("Search or create a tag...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 15.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(12.dp))

                val filteredTags = globalTags.filter { it.name.contains(tagSearchQuery, ignoreCase = true) }
                val exactMatchExists = globalTags.any { it.name.equals(tagSearchQuery.trim(), ignoreCase = true) }

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {

                    if (tagSearchQuery.isNotBlank() && !exactMatchExists) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val colors = listOf("#E03E3E", "#D9730D", "#DFAB01", "#0F7B6C", "#0B6E99", "#6940A5", "#9065B0")
                                        val newTagId = actions.onCreateGlobalTag(tagSearchQuery.trim(), colors.random())

                                        currentTagIds.add(newTagId)
                                        actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, currentTagIds.joinToString(","))
                                        tagSearchQuery = ""
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Create \"${tagSearchQuery.trim()}\"", fontFamily = BricolageFont, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    items(filteredTags) { tag ->
                        val isSelected = currentTagIds.contains(tag.tagId)
                        val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) currentTagIds.remove(tag.tagId) else currentTagIds.add(tag.tagId)
                                    actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, currentTagIds.joinToString(","))
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tagColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = tag.name,
                                    fontSize = 14.sp,
                                    fontFamily = BricolageFont,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            DbSheetType.FILE_OPTIONS -> {
                val row = block.rows.find { it.id == activeRowId } ?: return@InlyBottomSheet
                val currentFiles = row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {

                    items(currentFiles) { fileUri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { try { uriHandler.openUri(fileUri) } catch (e: Exception) {} } // Click to view file
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = android.net.Uri.parse(fileUri).lastPathSegment ?: "Unknown File",
                                    fontFamily = BricolageFont,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        currentFiles.remove(fileUri)
                                        actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, currentFiles.joinToString(","))
                                    }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fileLauncher.launch(arrayOf("*/*")) } // Open Android Picker
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Attach a new file", fontFamily = BricolageFont, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            DbSheetType.PRIORITY_SELECTION -> {
                val options = listOf(
                    "Low"    to MaterialTheme.colorScheme.outline,
                    "Medium" to MaterialTheme.colorScheme.primary,
                    "High"   to MaterialTheme.colorScheme.tertiary,
                    "Urgent" to MaterialTheme.colorScheme.error
                )
                val row = block.rows.find { it.id == activeRowId } ?: return@InlyBottomSheet
                val current = row.cells[activeColId] ?: ""

                options.forEach { (label, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                closeAnd {
                                    actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, label)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontFamily = BricolageFont,
                                color = color,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (current == label) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (current.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    DbOptionRow(
                        icon = Icons.Default.Close,
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error
                    ) {
                        closeAnd { actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, "") }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TableCell(
    value: String,
    columnType: ColumnType,
    cellWidth: Dp,
    globalTags: List<TagEntity>,
    onValueChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onTagClick: () -> Unit,
    onFileClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    when (columnType) {
        ColumnType.TEXT, ColumnType.NUMBER, ColumnType.PHONE, ColumnType.EMAIL, ColumnType.URL -> {
            var isFocused by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (columnType == ColumnType.TEXT) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                // ADDED URL HERE
                val isLinkType = columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = BricolageFont,
                        fontSize = 15.sp,
                        color = if (isLinkType && value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isLinkType && value.isNotBlank()) TextDecoration.Underline else TextDecoration.None
                    ),
                    keyboardOptions = when (columnType) {
                        ColumnType.NUMBER -> KeyboardOptions(keyboardType = KeyboardType.Number)
                        ColumnType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                        ColumnType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email)
                        ColumnType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri) // <--- ADDED URL
                        else -> KeyboardOptions.Default
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = columnType != ColumnType.TEXT,
                    modifier = Modifier
                        .defaultMinSize(minWidth = cellWidth - 24.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                )

                if (!isFocused) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (columnType == ColumnType.EMAIL && value.isNotBlank()) {
                                        try { uriHandler.openUri("mailto:$value") } catch(e:Exception){}
                                    } else if (columnType == ColumnType.PHONE && value.isNotBlank()) {
                                        try { uriHandler.openUri("tel:$value") } catch(e:Exception){}
                                    } else if (columnType == ColumnType.URL && value.isNotBlank()) {
                                        // Handle URLs (auto-append https if missing)
                                        try {
                                            val url = if (!value.startsWith("http://") && !value.startsWith("https://")) "https://$value" else value
                                            uriHandler.openUri(url)
                                        } catch(e:Exception){}
                                    } else {
                                        focusRequester.requestFocus()
                                    }
                                },
                                onDoubleClick = { focusRequester.requestFocus() },
                                onLongClick = { onLongPress() }
                            )
                    )
                }
            }
        }
        ColumnType.CHECKBOX -> {
            val isChecked = value == "true"
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onValueChange(it.toString()) },
                        modifier = Modifier.scale(0.9f).size(18.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.surface,
                            checkmarkColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
        ColumnType.DATE -> {
            Text(
                text = value.ifEmpty { "—" },
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clickable { onDateClick() }
            )
        }
        ColumnType.FORMULA -> {
            val formulaScrollState = rememberScrollState()
            Text(
                text = value,
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(formulaScrollState)
            )
        }
        ColumnType.TAGS -> {
            val activeTagIds = value.split(",").filter { it.isNotBlank() }
            val activeTags = activeTagIds.mapNotNull { id -> globalTags.find { it.tagId == id } }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp)
                    .combinedClickable(
                        onClick = { onTagClick() },
                        onLongClick = { onLongPress() }
                    )
            ) {
                if (activeTags.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeTags.forEach { tag ->
                            val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tagColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = tag.name,
                                    fontSize = 12.sp,
                                    fontFamily = BricolageFont,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        ColumnType.FILES -> {
            val files = value.split(",").filter { it.isNotBlank() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp)
                    .combinedClickable(
                        onClick = { onFileClick() },
                        onLongClick = { onLongPress() }
                    )
            ) {
                if (files.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        files.forEach { fileUri ->
                            // Attempt to extract the file name from the URI
                            val fileName = android.net.Uri.parse(fileUri).lastPathSegment ?: "File"

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = fileName,
                                        fontSize = 12.sp,
                                        fontFamily = BricolageFont,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ColumnType.PRIORITY -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp)
                    .combinedClickable(
                        onClick = { onPriorityClick() },
                        onLongClick = { onLongPress() }
                    )
            ) {
                if (value.isBlank()) {
                    Text("—", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), fontSize = 14.sp)
                } else {
                    val chipColor = when (value) {
                        "Low"    -> MaterialTheme.colorScheme.outline
                        "Medium" -> MaterialTheme.colorScheme.primary
                        "High"   -> MaterialTheme.colorScheme.tertiary
                        "Urgent" -> MaterialTheme.colorScheme.error
                        else     -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = chipColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = value,
                            fontSize = 13.sp,
                            fontFamily = BricolageFont,
                            color = chipColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}