package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import coil3.network.httpHeaders
import coil3.request.crossfade
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.DatabaseRow
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.LocalInlyExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import java.io.File


@Composable
fun Modifier.mouseScrollable(scrollState: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    return this.pointerInput(scrollState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.firstOrNull()
                    val delta = change?.scrollDelta?.y ?: 0f
                    if (delta != 0f) {
                        scope.launch {
                            scrollState.scrollBy(delta * 75f)
                        }
                        change?.consume()
                    }
                }
            }
        }
    }
}

private val DefaultBlockShape = RoundedCornerShape(12.dp)

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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

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

    val startPadding = when {
        isDatabase -> (block.indentationLevel * 28).dp
        block is CheckboxBlock -> (18 + (block.indentationLevel * 28)).dp
        block is BulletedListBlock -> (18 + (block.indentationLevel * 28)).dp
        block is NumberedListBlock -> (18 + (block.indentationLevel * 28)).dp
        block is ToggleBlock -> (18 + (block.indentationLevel * 28)).dp
        else -> (16 + (block.indentationLevel * 28)).dp
    }
    val endPadding = if (isDatabase) 0.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(selectionBg)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
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
                                modifier = Modifier.size(32.dp).rotate(rotation)
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
                                                // Notification permissions bypass for KMP structure
                                                val keyboardWasOpen = isKeyboardOpen
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                scope.launch {
                                                    if (keyboardWasOpen) delay(500L) else delay(50L)
                                                    showPresetMenu = true
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
                                                val keyboardWasOpen = isKeyboardOpen
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                scope.launch {
                                                    if (keyboardWasOpen) delay(500L) else delay(50L)
                                                    showTimePresetMenu = true
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
                                        val instant = Instant.fromEpochMilliseconds(block.reminderTimestamp!!)
                                        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                        val amPm = if (dt.hour >= 12) "PM" else "AM"
                                        val hour12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
                                        val minStr = dt.minute.toString().padStart(2, '0')
                                        "${dt.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${dt.dayOfMonth}, $hour12:$minStr $amPm"
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
                        is ImageBlock -> ImageBlockView(
                            block, inSelectionMode,
                            onToggleSelection = { actions.onToggleSelection(block.id) },
                            onRequestPicker = { actions.onRequestImagePicker(block.id) },
                            onDelete = { actions.onDeleteImageBlock(block.id) }
                        )
                        is DocumentBlock -> DocumentBlockView(
                            block = block,
                            inSelectionMode = inSelectionMode,
                            onToggleSelection = { actions.onToggleSelection(block.id) },
                            onRequestPicker = { actions.onRequestDocumentPicker(block.id) },
                            onOpenFile = { filePath, mimeType -> actions.onOpenFile(filePath, mimeType) }
                        )
                        is DatabaseBlock -> DatabaseBlockView(block, inSelectionMode, globalTags, actions)
                        is VoiceBlock -> VoiceBlockView(
                            block = block,
                            inSelectionMode = inSelectionMode,
                            onToggleSelection = { actions.onToggleSelection(block.id) },
                            onRemoveVoice = { actions.onRemoveVoice(block.id) },
                            onStartRecording = { actions.onStartRecording() },
                            onStopRecording = { cancel -> actions.onStopRecording(block.id, cancel) },
                            onPlayAudio = { path, onComplete -> actions.onPlayAudio(path, onComplete) },
                            onStopAudio = { actions.onStopAudio() }
                        )
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
                        val tz = TimeZone.currentSystemDefault()
                        val currentInstant = block.reminderTimestamp?.let { Instant.fromEpochMilliseconds(it) } ?: Clock.System.now()
                        val currentDt = currentInstant.toLocalDateTime(tz)

                        val newDt = LocalDateTime(
                            currentDt.year, currentDt.monthNumber, currentDt.dayOfMonth,
                            hour, minute, 0, 0
                        )
                        actions.onUpdateReminder(block.id, newDt.toInstant(tz).toEpochMilliseconds())
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
fun BookmarkBlockView(
    block: BookmarkBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onUrlSubmit: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(block.url.isEmpty()) }
    var inputUrl by remember { mutableStateOf(block.url) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (inSelectionMode) onToggleSelection()
                    else if (!isEditing && block.url.isNotEmpty()) {
                        try { uriHandler.openUri(block.url) } catch (_: Exception) {}
                    }
                },
                onLongClick = onToggleSelection
            )
    ) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DefaultBlockShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    textStyle = TextStyle(
                        fontFamily = BricolageFont,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputUrl.isNotBlank()) {
                                onUrlSubmit(inputUrl)
                                isEditing = false
                            }
                        }
                    ),
                    decorationBox = { inner ->
                        if (inputUrl.isEmpty()) {
                            Text(
                                "Paste a link and press Enter...",
                                fontFamily = BricolageFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        inner()
                    }
                )
            }
        } else {
            val commonContainerModifier = Modifier
                .fillMaxWidth()
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), DefaultBlockShape)

            val textContent = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier.padding(14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = block.title ?: block.url,
                        fontFamily = BricolageFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!block.description.isNullOrEmpty()) {
                        Text(
                            text = block.description,
                            fontFamily = BricolageFont,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = remember(block.url) {
                                try { java.net.URI(block.url).host ?: block.url }
                                catch (_: Exception) { block.url }
                            },
                            fontFamily = BricolageFont,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val imageContent = @Composable { modifier: Modifier ->
                if (block.previewImageUrl != null) {
                    coil3.compose.AsyncImage(
                        model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                            .data(block.previewImageUrl)
                            .crossfade(true)
                            .httpHeaders(
                                coil3.network.NetworkHeaders.Builder()
                                    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                    .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                    .build()
                            )
                            .build(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        onState = { state ->
                            if (state is coil3.compose.AsyncImagePainter.State.Error) {
                                println("Coil failed to load bookmark image: ${state.result.throwable.message}")
                            }
                        }
                    )
                }
            }

            if (isDesktopPlatform) {
                Row(
                    modifier = commonContainerModifier.height(120.dp)
                ) {
                    textContent(Modifier.weight(1f).fillMaxHeight())

                    if (block.previewImageUrl != null) {
                        imageContent(Modifier.weight(0.35f).fillMaxHeight())
                    }
                }
            } else {
                Column(
                    modifier = commonContainerModifier
                ) {
                    if (block.previewImageUrl != null) {
                        imageContent(Modifier.fillMaxWidth().height(140.dp))
                    }
                    textContent(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceBlockView(
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
                // Recording State UI
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
                                        // Trigger REAL hardware save
                                        onStopRecording(false)
                                    } else {
                                        isRecording = true
                                        // Trigger REAL hardware mic
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
                            fontFamily = BricolageFont,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "Tap mic to record audio",
                            fontFamily = BricolageFont,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Playback State UI
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
                                        // Stop REAL playback
                                        onStopAudio()
                                    } else {
                                        isPlaying = true
                                        block.localFilePath?.let { path ->
                                            // Start REAL playback, wait for completion callback
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
                        fontFamily = BricolageFont,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlockView(
    block: ImageBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRequestPicker: () -> Unit,
    onDelete: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    // THE FIX 1: Grab the FileStorageManager to resolve paths dynamically
    val fileStorageManager = koinInject<FileStorageManager>()
    var showFullScreen by remember { mutableStateOf(false) }

    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else onRequestPicker()
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
        // THE FIX 2: Resolve the absolute path for the current OS!
        val absolutePath = remember(block.localFilePath) {
            fileStorageManager.getAbsoluteMediaPath(block.localFilePath)
        }
        val imageFile = remember(absolutePath) { File(absolutePath) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 100.dp, max = 260.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else showFullScreen = true
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            coil3.compose.AsyncImage(
                // THE FIX 3: Pass the resolved File object directly to Coil
                model = imageFile,
                contentDescription = "Note Image",
                modifier = Modifier.fillMaxSize(),
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
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val dialogHazeState = remember { HazeState() }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(state = dialogHazeState)
                    ) {
                        coil3.compose.AsyncImage(
                            // THE FIX 4: Apply it to the full-screen dialog as well
                            model = imageFile,
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

                    // ... (Keep the rest of your Full Screen Dialog UI exactly as is)

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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                                    block.localFilePath?.let {
                                        onDownload()
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
    onRequestPicker: () -> Unit,
    onOpenFile: (filePath: String, mimeType: String) -> Unit)
{
    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else onRequestPicker()
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
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) {
                            onToggleSelection()
                        } else {
                            block.localFilePath?.let { path ->
                                onOpenFile(path, block.mimeType ?: "*/*")
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
fun DbOptionRow(
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
    val scope = rememberCoroutineScope()

    var currentSheet by remember { mutableStateOf(DbSheetType.NONE) }
    var activeColId by remember { mutableStateOf<String?>(null) }
    var activeRowId by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var filterOperator by remember { mutableStateOf("contains") }
    var filterPriority by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val visibleColumns = remember(block.columns) {
        block.columns.filter { !it.isDeleted }
    }

    fun closeSheet() {
        currentSheet = DbSheetType.NONE
        activeRowId = null
    }

    fun applyAction(action: () -> Unit) {
        closeSheet()
        scope.launch {
            delay(250)
            action()
        }
    }

    val visibleRows = remember(block.rows, block.activeSorts, block.activeFilters) {
        var result = block.rows.filter { !it.isDeleted }

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
            val colType = visibleColumns.find { it.id == sort.columnId }?.type
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

    // --- REUSABLE SHEET CONTENT ---
    val sheetTitle = when (currentSheet) {
        DbSheetType.CELL_OPTIONS -> "Cell Actions"
        DbSheetType.COLUMN_OPTIONS -> visibleColumns.find { it.id == activeColId }?.name ?: "Column Options"
        DbSheetType.RENAME -> "Rename Column"
        DbSheetType.FORMULA -> "Edit Formula"
        DbSheetType.SORT -> "Sort"
        DbSheetType.FILTER -> "Add Filter"
        DbSheetType.FILE_OPTIONS -> "Attached Files"
        DbSheetType.PRIORITY_SELECTION -> "Set Priority"
        else -> ""
    }

    val sheetContent = @Composable {
        when (currentSheet) {
            DbSheetType.CELL_OPTIONS -> {
                val col = visibleColumns.find { it.id == activeColId }
                val row = block.rows.find { it.id == activeRowId }
                if (col != null && row != null) {
                    val colIndex = visibleColumns.indexOf(col)
                    val rowIndex = block.rows.indexOf(row)

                    DbOptionRow(Icons.Default.ArrowUpward, "Insert Row Above") { applyAction { actions.onAddDbRowAt(block.id, rowIndex) } }
                    DbOptionRow(Icons.Default.ArrowDownward, "Insert Row Below") { applyAction { actions.onAddDbRowAt(block.id, rowIndex + 1) } }
                    DbOptionRow(Icons.Default.ArrowBack, "Insert Column Left") { applyAction { actions.onAddDbColumnAt(block.id, colIndex) } }
                    DbOptionRow(Icons.Default.ArrowForward, "Insert Column Right") { applyAction { actions.onAddDbColumnAt(block.id, colIndex + 1) } }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    DbOptionRow(Icons.Default.Delete, "Delete Row", MaterialTheme.colorScheme.error) { applyAction { actions.onDeleteDbRow(block.id, row.id) } }
                    DbOptionRow(Icons.Default.DeleteSweep, "Delete Column", MaterialTheme.colorScheme.error) { applyAction { actions.onDeleteDbColumn(block.id, col.id) } }
                }
            }

            DbSheetType.COLUMN_OPTIONS -> {
                val col = visibleColumns.find { it.id == activeColId }
                if (col != null) {
                    val colIndex = visibleColumns.indexOf(col)

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
                            applyAction { actions.onReorderDbColumns(block.id, colIndex, colIndex - 1) }
                        }
                    }

                    if (colIndex < visibleColumns.lastIndex) {
                        DbOptionRow(Icons.Default.ArrowForward, "Move Right") {
                            applyAction { actions.onReorderDbColumns(block.id, colIndex, colIndex + 1) }
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
                                onClick = { applyAction { actions.onUpdateDbColumn(block.id, col.id, col.name, type) } },
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
                        onClick = { applyAction { actions.onDeleteDbColumn(block.id, col.id) } }
                    )
                }
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
                        onClick = { closeSheet() },
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
                            val c = visibleColumns.find { it.id == activeColId }
                            if (c != null && textInput.isNotBlank()) {
                                applyAction { actions.onUpdateDbColumn(block.id, c.id, textInput.trim(), c.type) }
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
                    visibleColumns.filter { it.id != activeColId }.forEach { c ->
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
                        onClick = { closeSheet() },
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
                        onClick = { applyAction { actions.onUpdateDbFormula(block.id, activeColId!!, textInput.trim()) } },
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
                        val idx = visibleColumns.indexOfFirst { it.id == activeColId }
                        activeColId = visibleColumns[(idx + 1) % visibleColumns.size].id
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = visibleColumns.find { it.id == activeColId }?.name ?: "",
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

                DbOptionRow(Icons.Default.ArrowUpward, "Ascending A → Z") {
                    applyAction { actions.onUpdateDbSort(block.id, activeColId!!, true) }
                }

                DbOptionRow(Icons.Default.ArrowDownward, "Descending Z → A") {
                    applyAction { actions.onUpdateDbSort(block.id, activeColId!!, false) }
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
                        applyAction { actions.onUpdateDbSort(block.id, block.activeSorts.first().columnId, null) }
                    }
                }
            }

            DbSheetType.FILTER -> {
                val activeCol = visibleColumns.find { it.id == activeColId }
                val isCheckbox = activeCol?.type == ColumnType.CHECKBOX
                val isNumber = activeCol?.type == ColumnType.NUMBER

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
                        val idx = visibleColumns.indexOfFirst { it.id == activeColId }
                        activeColId = visibleColumns[(idx + 1) % visibleColumns.size].id
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
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
                )

                val operatorOptions: List<Pair<String, String>> = when {
                    isCheckbox -> listOf("checked" to "Is checked", "unchecked" to "Is unchecked")
                    isNumber -> listOf("equals" to "Equals", "gt" to "Greater than", "lt" to "Less than", "not_empty" to "Is not empty", "empty" to "Is empty")
                    else -> listOf("contains" to "Contains", "equals" to "Equals", "not_empty" to "Is not empty", "empty" to "Is empty", "priority" to "Priority is")
                }
                if (operatorOptions.none { it.first == filterOperator }) filterOperator = operatorOptions.first().first

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                ) {
                    operatorOptions.forEach { (op, label) ->
                        val isSelected = filterOperator == op
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterOperator = op; textInput = "" },
                            label = { Text(label, fontFamily = BricolageFont, fontSize = 13.sp) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                        placeholder = { Text(text = if (isNumber) "Enter number…" else "Enter text…", fontSize = 14.sp) },
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
                                onClick = { filterPriority = p; textInput = p },
                                label = { Text(p, fontFamily = BricolageFont, fontSize = 13.sp) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { closeSheet() },
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
                            if (canApply) applyAction {
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
                val row = block.rows.find { it.id == activeRowId }
                if (row != null) {
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

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {

                        if (tagSearchQuery.isNotBlank() && !exactMatchExists) {
                            // Remove 'item { }' wrapper
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

                        // CHANGE 'items(filteredTags) { tag ->' TO A FOREACH LOOP:
                        filteredTags.forEach { tag ->
                            val isSelected = currentTagIds.contains(tag.tagId)
                            val tagColor = try {
                                Color(tag.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
                            } catch (e: Exception) { MaterialTheme.colorScheme.primary }

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

                        // Remove 'item { }' wrapper
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            DbSheetType.FILE_OPTIONS -> {
                val row = block.rows.find { it.id == activeRowId }
                if (row != null) {
                    val currentFiles = row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {

                        // CHANGE 'items(currentFiles) { fileUri ->' TO A FOREACH LOOP:
                        currentFiles.forEach { fileUri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { try { uriHandler.openUri(fileUri) } catch (e: Exception) {} }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = fileUri.split("/").lastOrNull() ?: "Unknown File",
                                        fontFamily = BricolageFont,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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

                        // Remove 'item { }' wrapper
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newFile = "mock_file_${Clock.System.now().toEpochMilliseconds()}.pdf"
                                    val combined = (currentFiles + newFile).joinToString(",")
                                    actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, combined)
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Attach a new file", fontFamily = BricolageFont, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Remove 'item { }' wrapper
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            DbSheetType.PRIORITY_SELECTION -> {
                val options = listOf(
                    "Low"    to MaterialTheme.colorScheme.outline,
                    "Medium" to MaterialTheme.colorScheme.primary,
                    "High"   to MaterialTheme.colorScheme.tertiary,
                    "Urgent" to MaterialTheme.colorScheme.error
                )
                val row = block.rows.find { it.id == activeRowId }
                if (row != null) {
                    val current = row.cells[activeColId] ?: ""

                    options.forEach { (label, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyAction { actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, label) }
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
                            applyAction { actions.onUpdateDbCell(block.id, activeRowId!!, activeColId!!, "") }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
            else -> {}
        }
    }

    val DesktopDbDropdown = @Composable { visible: Boolean ->
        if (isDesktopPlatform && visible) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { closeSheet() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).padding(vertical = 4.dp)) {
                    if (sheetTitle.isNotBlank()) {
                        Text(
                            text = sheetTitle,
                            fontFamily = BricolageFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp).padding(horizontal = 20.dp)
                        )
                    }
                    sheetContent()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = { actions.onToggleSelection(block.id) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
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
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                enabled = !inSelectionMode
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    val hasSort = block.activeSorts.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasSort) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = block.activeSorts.firstOrNull()?.columnId ?: visibleColumns.first().id
                                currentSheet = DbSheetType.SORT
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            val sortIcon = if (block.activeSorts.firstOrNull()?.isAscending == false) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (hasSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.SORT)
                }

                Box {
                    val hasFilter = block.activeFilters.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = visibleColumns.first().id
                                textInput = ""
                                filterOperator = "contains"
                                currentSheet = DbSheetType.FILTER
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (hasFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.FILTER)
                }
            }
        }

        if (block.activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.activeFilters.forEach { filter ->
                    val colName = visibleColumns.find { it.id == filter.columnId }?.name ?: "?"
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
                                modifier = Modifier.size(13.dp).clickable(enabled = !inSelectionMode) { actions.onRemoveDbFilter(block.id, filter) },
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
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
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
                            visibleColumns.forEachIndexed { index, col ->
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
                                Box {
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
                                            .clickable(enabled = !inSelectionMode) {
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
                                                    imageVector = if (activeSort.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    DesktopDbDropdown(activeColId == col.id && currentSheet in listOf(DbSheetType.COLUMN_OPTIONS, DbSheetType.RENAME, DbSheetType.FORMULA))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .drawBehind { drawLine(color = borderColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 0.5.dp.toPx()) }
                                    .clickable(enabled = !inSelectionMode) { actions.onAddDbColumn(block.id) }
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
                                visibleColumns.forEach { col ->
                                    val cellValue = row.cells[col.id] ?: ""
                                    val isHighlighted = currentSheet == DbSheetType.CELL_OPTIONS && activeRowId == row.id && activeColId == col.id

                                    Box {
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
                                                            if (!inSelectionMode) {
                                                                focusManager.clearFocus()
                                                                activeRowId = row.id
                                                                activeColId = col.id
                                                                currentSheet = DbSheetType.CELL_OPTIONS
                                                            }
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
                                                inSelectionMode = inSelectionMode,
                                                onValueChange = { actions.onUpdateDbCell(block.id, row.id, col.id, it) },
                                                onDateClick = {
                                                    if (!inSelectionMode) {
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        showDatePicker = true
                                                    }
                                                },
                                                onTagClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.TAG_SELECTION
                                                    }
                                                },
                                                onFileClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.FILE_OPTIONS
                                                    }
                                                },
                                                onPriorityClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.PRIORITY_SELECTION
                                                    }
                                                },
                                                onLongPress = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.CELL_OPTIONS
                                                    }
                                                }
                                            )
                                        }
                                        DesktopDbDropdown(activeRowId == row.id && activeColId == col.id && currentSheet in listOf(DbSheetType.CELL_OPTIONS, DbSheetType.TAG_SELECTION, DbSheetType.FILE_OPTIONS, DbSheetType.PRIORITY_SELECTION))
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
                        .clickable(enabled = !inSelectionMode) { actions.onAddDbRow(block.id) }
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

    if (!isDesktopPlatform && currentSheet != DbSheetType.NONE) {
        InlyBottomSheet(
            expanded = true,
            onDismiss = { closeSheet() },
            title = sheetTitle
        ) { _ ->
            sheetContent()
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
    inSelectionMode: Boolean,
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
            val textScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (columnType == ColumnType.TEXT) {
                            Modifier
                                .horizontalScroll(textScrollState)
                                .mouseScrollable(textScrollState)
                        } else Modifier
                    )
            ) {
                val isLinkType = columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !inSelectionMode,
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
                        ColumnType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                        else -> KeyboardOptions.Default
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = columnType != ColumnType.TEXT,
                    modifier = Modifier
                        .defaultMinSize(minWidth = cellWidth - 24.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                )

                if (!isFocused && !inSelectionMode) {
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
                        onCheckedChange = { if (!inSelectionMode) onValueChange(it.toString()) },
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
                modifier = Modifier.fillMaxWidth().clickable(enabled = !inSelectionMode) { onDateClick() }
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
                modifier = Modifier
                    .horizontalScroll(formulaScrollState)
                    .mouseScrollable(formulaScrollState)
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
                        onClick = { if (!inSelectionMode) onTagClick() },
                        onLongClick = { if (!inSelectionMode) onLongPress() }
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
                            val tagColor = try {
                                Color(tag.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
                            } catch (e: Exception) { MaterialTheme.colorScheme.primary }
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
                        onClick = { if (!inSelectionMode) onFileClick() },
                        onLongClick = { if (!inSelectionMode) onLongPress() }
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
                            // KMP-Safe URI segment parsing
                            val fileName = fileUri.split("/").lastOrNull() ?: "File"

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
                        onClick = { if (!inSelectionMode) onPriorityClick() },
                        onLongClick = { if (!inSelectionMode) onLongPress() }
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