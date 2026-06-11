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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.layout.onSizeChanged
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
import coil3.network.httpHeaders
import coil3.request.crossfade
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.QuoteBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.PoppinsFont
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
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException

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

@Composable
fun NoteBlockItem(
    block: NoteBlock,
    globalTags: List<TagEntity>,
    actions: EditorActions,
    focusRequest: FocusRequest?,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    isActiveBlock: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val focusRequester = remember { FocusRequester() }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var blockHeight by remember { mutableFloatStateOf(50f) }
    val imeBottom = WindowInsets.ime.getBottom(density)

    val text = when (block) {
        is CodeBlock -> block.code
        is QuoteBlock -> block.text
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

    LaunchedEffect(isFocused, imeBottom, blockHeight) {
        if (isFocused && imeBottom > 0) {
            val bufferPx = with(density) { 100.dp.toPx() }

            val targetRect = androidx.compose.ui.geometry.Rect(
                left = 0f,
                top = 0f,
                right = 1f,
                bottom = blockHeight + bufferPx
            )

            bringIntoViewRequester.bringIntoView(targetRect)
        }
    }

    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            val safeStart = textFieldValue.selection.start.coerceAtMost(text.length)
            val safeEnd = textFieldValue.selection.end.coerceAtMost(text.length)
            textFieldValue = textFieldValue.copy(
                text = text,
                selection = TextRange(safeStart, safeEnd)
            )
        }
    }

    LaunchedEffect(focusRequest?.nonce) {
        if (focusRequest != null && focusRequest.id == block.id) {
            if (focusRequest.placeCursorAtEnd) {
                textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
            }

            delay(50)

            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) { }

            actions.onClearFocusRequest()
        }
    }

    val baseStyle = when (block) {
        is HeadingBlock -> TextStyle(
            fontFamily = PoppinsFont,
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
        is QuoteBlock -> TextStyle(
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> TextStyle(
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
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

            val primaryColor = MaterialTheme.colorScheme.primary
            val textFieldWrapperModifier = (if (block is CodeBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, DefaultBlockShape)
                    .padding(12.dp)
            } else if (block is QuoteBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp)
                    .drawBehind {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            } else if (isDatabase) {
                Modifier.weight(1f)
            } else {
                Modifier.weight(1f).padding(horizontal = 4.dp)
            })
                .bringIntoViewRequester(bringIntoViewRequester)
                .onSizeChanged { blockHeight = it.height.toFloat() }
                .onFocusChanged { focusState ->
                    val currentlyFocused = focusState.isFocused || focusState.hasFocus
                    isFocused = currentlyFocused
                    if (currentlyFocused) {
                        onFocus()
                    }
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
                                        val splitIndex = newText.indexOf('\n')
                                        val textBefore = newText.substring(0, splitIndex)
                                        val textAfter = newText.substring(splitIndex + 1).replace("\n", "")

                                        textFieldValue = newValue.copy(
                                            text = textBefore,
                                            selection = TextRange(textBefore.length)
                                        )

                                        actions.onEnterPressed(block.id, textBefore, textAfter)
                                    } else {
                                        textFieldValue = newValue
                                        actions.onUpdateText(block.id, newText)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { event ->
                                        val isBackspace = event.key == Key.Backspace
                                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter

                                        if (isBackspace && textFieldValue.text.isEmpty()) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Previous)
                                                actions.onBackspaceOnEmpty(block.id)
                                            }
                                            return@onPreviewKeyEvent true
                                        }

                                        if (isEnter && block !is CodeBlock) {
                                            if (event.type == KeyEventType.KeyDown) {
                                                val cursor = textFieldValue.selection.start
                                                val textBefore = textFieldValue.text.substring(0, cursor)
                                                val textAfter = textFieldValue.text.substring(cursor)
                                                actions.onEnterPressed(block.id, textBefore, textAfter)
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        false
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
                                                keyboardController?.show()
                                            },
                                            onDoubleTap = { offset ->
                                                val position = textLayoutResult
                                                    ?.getOffsetForPosition(offset)
                                                    ?: textFieldValue.text.length
                                                textFieldValue = textFieldValue.copy(
                                                    selection = TextRange(position)
                                                )
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
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
                                        fontFamily = PoppinsFont,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                } else {
                    androidx.compose.runtime.key(block.id) {
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

        }

        if (block.isPinned) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 16.dp)
                    .size(15.dp)
            )
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
                        fontFamily = PoppinsFont,
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
                                fontFamily = PoppinsFont,
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
                        fontFamily = PoppinsFont,
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
                            fontFamily = PoppinsFont,
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
                            fontFamily = PoppinsFont,
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
    val mediaStorageHelper = org.koin.compose.koinInject<com.ben.inly.domain.util.MediaStorageHelper>()
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
                Text("Add image", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        val absolutePath = remember(block.localFilePath) {
            mediaStorageHelper.getAbsoluteMediaPath(block.localFilePath)
        }
        val imageFile = remember(absolutePath) { java.io.File(absolutePath) }

        val request = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
            .data(imageFile)
            .memoryCacheKey("$absolutePath-${imageFile.lastModified()}")
            .diskCacheKey("$absolutePath-${imageFile.lastModified()}")
            .build()

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
                model = request,
                contentDescription = "Note Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (showFullScreen) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val pillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            val tint = MaterialTheme.colorScheme.primary

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showFullScreen = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val dialogHazeState = remember { HazeState() }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(state = dialogHazeState)
                    ) {
                        coil3.compose.AsyncImage(
                            model = request,
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
                Text("Attach a file", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
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
                    fontFamily = PoppinsFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = block.fileSizeString,
                    fontFamily = PoppinsFont,
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

enum class DbSheetType { NONE, COLUMN_OPTIONS, RENAME, FORMULA, FILTER, SORT, CELL_OPTIONS, TAG_SELECTION, FILE_OPTIONS, PRIORITY_SELECTION, AGGREGATION, CURRENCY_SELECTION }

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
        Text(text, fontFamily = PoppinsFont, fontSize = 14.sp, color = color)
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
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val borderColor1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()

    var currentSheet by remember { mutableStateOf(DbSheetType.NONE) }
    var activeColId by remember { mutableStateOf<String?>(null) }
    var activeRowId by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var textInputMax by remember { mutableStateOf("") }
    var filterOperator by remember { mutableStateOf("contains") }
    var filterPriority by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var columnAggregations by remember { mutableStateOf(mapOf<String, String>()) }
    var columnCurrencies by remember { mutableStateOf(mapOf<String, String>()) }
    var formulaIsCurrency by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var aggregationExpandedSection by remember { mutableStateOf<String?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var playingFileUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    fun closeSheet() {
        if (isRecording && activeRowId != null && activeColId != null) {
            isRecording = false
            actions.onStopDbAudioRecording(block.id, activeRowId!!, activeColId!!, true)
        }
        if (playingFileUri != null) {
            playingFileUri = null
            actions.onStopAudio()
        }
        currentSheet = DbSheetType.NONE
        activeRowId = null
        aggregationExpandedSection = null
    }

    val visibleColumns = remember(block.columns) {
        block.columns.filter { !it.isDeleted }
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
                    // Text/String Constraints
                    "contains"     -> cellVal.contains(filter.value, ignoreCase = true)
                    "not_contains" -> !cellVal.contains(filter.value, ignoreCase = true)
                    "equals"       -> cellVal.equals(filter.value, ignoreCase = true)
                    "not_equals"   -> !cellVal.equals(filter.value, ignoreCase = true)
                    "starts_with"  -> cellVal.startsWith(filter.value, ignoreCase = true)
                    "ends_with"    -> cellVal.endsWith(filter.value, ignoreCase = true)

                    // Element state rules
                    "empty"        -> cellVal.isBlank() || cellVal == "—"
                    "not_empty"    -> cellVal.isNotBlank() && cellVal != "—"
                    "checked"      -> cellVal == "true"
                    "unchecked"    -> cellVal != "true"

                    // Math / Numeric comparison bounds
                    "gt"           -> (cellVal.toDoubleOrNull() ?: 0.0) > (filter.value.toDoubleOrNull() ?: 0.0)
                    "gte"          -> (cellVal.toDoubleOrNull() ?: 0.0) >= (filter.value.toDoubleOrNull() ?: 0.0)
                    "lt"           -> (cellVal.toDoubleOrNull() ?: 0.0) < (filter.value.toDoubleOrNull() ?: 0.0)
                    "lte"          -> (cellVal.toDoubleOrNull() ?: 0.0) <= (filter.value.toDoubleOrNull() ?: 0.0)

                    // Between range — value stored as "lo|hi"
                    "between"      -> {
                        val parts = filter.value.split("|")
                        if (parts.size == 2 && cellVal.isNotBlank()) {
                            val lo = parts[0].toDoubleOrNull()
                            val hi = parts[1].toDoubleOrNull()
                            val v  = cellVal.toDoubleOrNull()
                            if (lo != null && hi != null && v != null) v in lo..hi
                            else cellVal >= parts[0] && cellVal <= parts[1]
                        } else true
                    }

                    // Custom priority maps & timestamps
                    "priority"     -> cellVal.equals(filter.value, ignoreCase = true)
                    "before"       -> cellVal.isNotBlank() && cellVal < filter.value
                    "after"        -> cellVal.isNotBlank() && cellVal > filter.value
                    else           -> true
                }
            }
        }

        if (block.activeSorts.isNotEmpty()) {
            result = result.sortedWith(Comparator { row1, row2 ->
                var comparisonResult = 0

                for (sortRule in block.activeSorts) {
                    val targetCol = block.columns.find { it.id == sortRule.columnId } ?: continue
                    val rawVal1 = row1.cells[sortRule.columnId] ?: ""
                    val rawVal2 = row2.cells[sortRule.columnId] ?: ""

                    comparisonResult = when (targetCol.type) {
                        ColumnType.NUMBER, ColumnType.MONEY -> {
                            val num1 = rawVal1.toDoubleOrNull() ?: Double.MAX_VALUE
                            val num2 = rawVal2.toDoubleOrNull() ?: Double.MAX_VALUE
                            num1.compareTo(num2)
                        }
                        ColumnType.CHECKBOX -> {
                            val bool1 = rawVal1 == "true"
                            val bool2 = rawVal2 == "true"
                            bool1.compareTo(bool2)
                        }
                        ColumnType.PRIORITY -> {
                            val weight = mapOf("Low" to 1, "Medium" to 2, "High" to 3, "Urgent" to 4)
                            val p1 = weight[rawVal1] ?: 0
                            val p2 = weight[rawVal2] ?: 0
                            p1.compareTo(p2)
                        }
                        else -> rawVal1.lowercase().compareTo(rawVal2.lowercase())
                    }

                    if (!sortRule.isAscending) comparisonResult = -comparisonResult
                    if (comparisonResult != 0) break
                }
                comparisonResult
            })
        }
        result
    }

    val sheetTitle = when (currentSheet) {
        DbSheetType.CELL_OPTIONS    -> "Cell Actions"
        DbSheetType.COLUMN_OPTIONS  -> visibleColumns.find { it.id == activeColId }?.name ?: "Column Options"
        DbSheetType.RENAME          -> "Rename Column"
        DbSheetType.FORMULA         -> "Edit Formula"
        DbSheetType.SORT            -> "Sort"
        DbSheetType.FILTER          -> "Add Filter"
        DbSheetType.FILE_OPTIONS    -> "Attached Files"
        DbSheetType.PRIORITY_SELECTION -> "Set Priority"
        DbSheetType.AGGREGATION     -> "Calculate"
        else                        -> ""
    }

    val sheetContent = @Composable {
        when (currentSheet) {

            // ── CELL OPTIONS ─────────────────────────────────────────────────────────
            DbSheetType.CELL_OPTIONS -> {
                val col = visibleColumns.find { it.id == activeColId }
                val row = block.rows.find { it.id == activeRowId }
                if (col != null && row != null) {
                    val colIndex = visibleColumns.indexOf(col)
                    val rowIndex = block.rows.indexOf(row)

                    DbOptionRow(Icons.Default.ArrowUpward,   "Insert Row Above")    { applyAction { actions.onAddDbRowAt(block.id, rowIndex) } }
                    DbOptionRow(Icons.Default.ArrowDownward, "Insert Row Below")    { applyAction { actions.onAddDbRowAt(block.id, rowIndex + 1) } }
                    DbOptionRow(Icons.Default.ArrowBack,     "Insert Column Left")  { applyAction { actions.onAddDbColumnAt(block.id, colIndex) } }
                    DbOptionRow(Icons.Default.ArrowForward,  "Insert Column Right") { applyAction { actions.onAddDbColumnAt(block.id, colIndex + 1) } }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    DbOptionRow(Icons.Default.Delete,      "Delete Row",    MaterialTheme.colorScheme.error) { applyAction { actions.onDeleteDbRow(block.id, row.id) } }
                    DbOptionRow(Icons.Default.DeleteSweep, "Delete Column", MaterialTheme.colorScheme.error) { applyAction { actions.onDeleteDbColumn(block.id, col.id) } }
                }
            }

            // ── COLUMN OPTIONS ───────────────────────────────────────────────────────
            DbSheetType.COLUMN_OPTIONS -> {
                val col = visibleColumns.find { it.id == activeColId }
                if (col != null) {
                    val colIndex = visibleColumns.indexOf(col)

                    DbOptionRow(Icons.Default.Edit, "Rename Column") {
                        textInput = col.name
                        currentSheet = DbSheetType.RENAME
                    }

                    if (col.type == ColumnType.FORMULA) {
                        DbOptionRow(icon = Icons.Default.Functions, text = "Edit Formula", color = MaterialTheme.colorScheme.primary) {
                            textInput = col.formulaExpression ?: ""
                            currentSheet = DbSheetType.FORMULA
                        }

                        val isCurrency = formulaIsCurrency[col.id] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newMap = formulaIsCurrency.toMutableMap()
                                    newMap[col.id] = !isCurrency
                                    formulaIsCurrency = newMap
                                }
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Format as currency", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            androidx.compose.material3.Switch(checked = isCurrency, onCheckedChange = null, modifier = Modifier.scale(0.8f))
                        }

                        if (isCurrency) {
                            val currentCurrency = columnCurrencies[col.id] ?: "$"
                            DbOptionRow(icon = Icons.Default.MonetizationOn, text = "Currency: $currentCurrency", color = MaterialTheme.colorScheme.primary) {
                                currentSheet = DbSheetType.CURRENCY_SELECTION
                            }
                        }
                    }

                    if (col.type == ColumnType.MONEY) {
                        val currentCurrency = columnCurrencies[col.id] ?: "$"
                        DbOptionRow(icon = Icons.Default.MonetizationOn, text = "Format: $currentCurrency", color = MaterialTheme.colorScheme.primary) {
                            currentSheet = DbSheetType.CURRENCY_SELECTION
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text(text = "Column Width", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { actions.onUpdateDbColumnWidth(block.id, col.id, col.width - 20) }
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Text(
                            text = "${col.width} px",
                            fontFamily = PoppinsFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 50.dp),
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { actions.onUpdateDbColumnWidth(block.id, col.id, col.width + 20) }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text(text = "Property Type", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 12.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                        ColumnType.entries.forEach { type ->
                            val isSelected = col.type == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { applyAction { actions.onUpdateDbColumn(block.id, col.id, col.name, type) } },
                                label = { Text(text = type.name.lowercase().replaceFirstChar { it.uppercase() }, fontFamily = PoppinsFont, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    DbOptionRow(icon = Icons.Default.Delete, text = "Delete Column", color = MaterialTheme.colorScheme.error, onClick = { applyAction { actions.onDeleteDbColumn(block.id, col.id) } })
                }
            }

            // ── RENAME ───────────────────────────────────────────────────────────────
            DbSheetType.RENAME -> {
                OutlinedTextField(value = textInput, onValueChange = { textInput = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 14.sp), shape = RoundedCornerShape(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { closeSheet() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                        Text("Cancel", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            val c = visibleColumns.find { it.id == activeColId }
                            if (c != null && textInput.isNotBlank()) {
                                applyAction { actions.onUpdateDbColumn(block.id, c.id, textInput.trim(), c.type) }
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Save", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                }
            }

            // ── FORMULA ──────────────────────────────────────────────────────────────
            DbSheetType.FORMULA -> {
                Text(text = "Properties", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    visibleColumns.filter { it.id != activeColId }.forEach { c ->
                        SuggestionChip(
                            onClick = { textInput += "prop(\"${c.name}\") " },
                            label = { Text(c.name, fontFamily = PoppinsFont, fontSize = 13.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        )
                    }
                }

                Text(text = "Operators", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    listOf("+", "-", "*", "/", "(", ")").forEach { op ->
                        SuggestionChip(
                            onClick = { textInput += "$op " },
                            label = { Text(op, fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        )
                    }
                }

                OutlinedTextField(value = textInput, onValueChange = { textInput = it }, placeholder = { Text("e.g. prop(\"Price\") * 2", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 18.sp), shape = RoundedCornerShape(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { closeSheet() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                        Text("Cancel", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                    Button(onClick = { val colId = activeColId ?: return@Button; applyAction { actions.onUpdateDbFormula(block.id, colId, textInput.trim()) } }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                        Text("Save", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                }
            }

            // ── SORT (multi-layer, Notion-style) ─────────────────────────────────────
            DbSheetType.SORT -> {
                val sortedColIds = block.activeSorts.map { it.columnId }
                val unsortedColumns = visibleColumns.filter { it.id !in sortedColIds }

                // Active sort layers
                if (block.activeSorts.isNotEmpty()) {
                    Text(
                        text = "Sort order — top layer wins, lower layers break ties",
                        fontFamily = PoppinsFont, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
                    )

                    block.activeSorts.forEachIndexed { index, sortRule ->
                        val col = visibleColumns.find { it.id == sortRule.columnId } ?: return@forEachIndexed
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Layer number badge
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontFamily = PoppinsFont, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                col.name,
                                fontFamily = PoppinsFont, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            // Asc/Desc toggle — stays in sheet so you can keep editing layers
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable {
                                    actions.onUpdateDbSort(block.id, col.id, !sortRule.isAscending)
                                }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (sortRule.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        null, modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (sortRule.isAscending) "Asc" else "Desc",
                                        fontFamily = PoppinsFont, fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Close, "Remove sort layer",
                                modifier = Modifier.size(18.dp).clickable {
                                    actions.onUpdateDbSort(block.id, col.id, null)
                                },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }

                if (unsortedColumns.isNotEmpty()) {
                    Text(
                        text = if (block.activeSorts.isEmpty()) "Sort by" else "Then by",
                        fontFamily = PoppinsFont, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    ) {
                        unsortedColumns.forEach { col ->
                            SuggestionChip(
                                onClick = { actions.onUpdateDbSort(block.id, col.id, true) },
                                label = { Text(col.name, fontFamily = PoppinsFont, fontSize = 13.sp) },
                                icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            )
                        }
                    }
                } else if (block.activeSorts.isNotEmpty()) {
                    Text(
                        text = "Every column is already in the sort.",
                        fontFamily = PoppinsFont, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (block.activeSorts.isNotEmpty()) {
                        Button(
                            onClick = {
                                block.activeSorts.map { it.columnId }.forEach { cid ->
                                    actions.onUpdateDbSort(block.id, cid, null)
                                }
                                closeSheet()
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("Clear all", fontFamily = PoppinsFont, fontSize = 14.sp)
                        }
                    }
                    Button(
                        onClick = { closeSheet() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Done", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                }
            }

            // ── FILTER ───────────────────────────────────────────────────────────────
            DbSheetType.FILTER -> {
                val activeCol = visibleColumns.find { it.id == activeColId }
                val isCheckbox = activeCol?.type == ColumnType.CHECKBOX
                val isNumber   = activeCol?.type == ColumnType.NUMBER || activeCol?.type == ColumnType.MONEY
                val isDate     = activeCol?.type == ColumnType.DATE

                Text(text = "Column", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable {
                        val idx = visibleColumns.indexOfFirst { it.id == activeColId }
                        activeColId = visibleColumns[(idx + 1) % visibleColumns.size].id
                        filterOperator = "contains"
                        textInput = ""
                        textInputMax = ""
                    }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = activeCol?.name ?: "", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                Text(text = "Condition", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp))

                val operatorOptions: List<Pair<String, String>> = when {
                    isCheckbox -> listOf(
                        "unchecked" to "Hide Checked rows",
                        "checked"   to "Hide Unchecked rows"
                    )
                    isNumber -> listOf(
                        "equals"    to "Equals",
                        "not_equals" to "Does not equal",
                        "gt"        to "Greater than (>)",
                        "gte"       to "Greater than or equal (≥)",
                        "lt"        to "Less than (<)",
                        "lte"       to "Less than or equal (≤)",
                        "between"   to "Between (range)",
                        "not_empty" to "Is not empty",
                        "empty"     to "Is empty"
                    )
                    isDate -> listOf(
                        "equals"    to "On exactly date",
                        "before"    to "Is before date",
                        "after"     to "Is after date",
                        "between"   to "Between two dates",
                        "not_empty" to "Is scheduled (Not empty)",
                        "empty"     to "Is unscheduled (Empty)"
                    )
                    else -> listOf(
                        "contains"     to "Contains text",
                        "not_contains" to "Does not contain",
                        "equals"       to "Is exactly",
                        "not_equals"   to "Is not",
                        "starts_with"  to "Starts with",
                        "ends_with"    to "Ends with",
                        "not_empty"    to "Is not empty",
                        "empty"        to "Is empty",
                        "priority"     to "Priority status is"
                    )
                }

                if (operatorOptions.none { it.first == filterOperator }) filterOperator = operatorOptions.first().first

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    operatorOptions.forEach { (op, label) ->
                        val isSelected = filterOperator == op
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterOperator = op; textInput = ""; textInputMax = "" },
                            label = { Text(label, fontFamily = PoppinsFont, fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }

                val needsTextInput   = filterOperator in listOf("contains", "equals", "not_equals", "gt", "gte", "lt", "lte", "before", "after", "starts_with", "ends_with")
                val needsRangeInput  = filterOperator == "between"
                val needsPriorityPicker = filterOperator == "priority"

                if (needsTextInput) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput, onValueChange = { textInput = it },
                        placeholder = { Text(text = if (isNumber) "Enter number…" else "Enter value…", fontSize = 14.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 14.sp),
                        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (needsRangeInput) {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = textInput, onValueChange = { textInput = it },
                            placeholder = { Text(if (isDate) "Start" else "Min", fontSize = 14.sp) },
                            singleLine = true, modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 14.sp),
                            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = textInputMax, onValueChange = { textInputMax = it },
                            placeholder = { Text(if (isDate) "End" else "Max", fontSize = 14.sp) },
                            singleLine = true, modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 14.sp),
                            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (needsPriorityPicker) {
                    Spacer(Modifier.height(10.dp))
                    Text(text = "Priority level", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                        listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                            val isSelected = filterPriority == p
                            val chipColor = when (p) {
                                "Urgent" -> MaterialTheme.colorScheme.error
                                "High"   -> MaterialTheme.colorScheme.tertiary
                                else     -> MaterialTheme.colorScheme.primary
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterPriority = p; textInput = p },
                                label = { Text(p, fontFamily = PoppinsFont, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chipColor, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { closeSheet() }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                        Text("Cancel", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            val colId = activeColId ?: return@Button
                            val canApply = when {
                                isCheckbox -> true
                                filterOperator in listOf("not_empty", "empty") -> true
                                filterOperator == "priority" -> filterPriority.isNotBlank()
                                filterOperator == "between"  -> textInput.isNotBlank() && textInputMax.isNotBlank()
                                else -> textInput.isNotBlank()
                            }
                            if (canApply) applyAction {
                                actions.onAddDbFilter(
                                    block.id, colId, filterOperator,
                                    when (filterOperator) {
                                        "priority" -> filterPriority.trim()
                                        "between"  -> "${textInput.trim()}|${textInputMax.trim()}"
                                        else       -> textInput.trim()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Apply", fontFamily = PoppinsFont, fontSize = 14.sp)
                    }
                }
            }

            // ── TAG SELECTION ────────────────────────────────────────────────────────
            DbSheetType.TAG_SELECTION -> {
                var tagSearchQuery by remember { mutableStateOf("") }
                val row = block.rows.find { it.id == activeRowId }
                if (row != null) {
                    val currentTagIds = row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()

                    OutlinedTextField(value = tagSearchQuery, onValueChange = { tagSearchQuery = it }, placeholder = { Text("Search or create a tag...", fontSize = 14.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 14.sp), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(12.dp))

                    val filteredTags = globalTags.filter { it.name.contains(tagSearchQuery, ignoreCase = true) }
                    val exactMatchExists = globalTags.any { it.name.equals(tagSearchQuery.trim(), ignoreCase = true) }

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        if (tagSearchQuery.isNotBlank() && !exactMatchExists) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val colors = listOf("#E03E3E", "#D9730D", "#DFAB01", "#0F7B6C", "#0B6E99", "#6940A5", "#9065B0")
                                        val newTagId = actions.onCreateGlobalTag(tagSearchQuery.trim(), colors.random())
                                        currentTagIds.add(newTagId)
                                        actions.onUpdateDbCell(block.id, row.id, activeColId ?: return@clickable, currentTagIds.joinToString(","))
                                        tagSearchQuery = ""
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Create \"${tagSearchQuery.trim()}\"", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

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
                                        actions.onUpdateDbCell(block.id, row.id, activeColId ?: return@clickable, currentTagIds.joinToString(","))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = tagColor.copy(alpha = 0.15f)) {
                                    Text(text = tag.name, fontSize = 14.sp, fontFamily = PoppinsFont, color = tagColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // ── FILE OPTIONS ─────────────────────────────────────────────────────────
            DbSheetType.FILE_OPTIONS -> {
                val row = block.rows.find { it.id == activeRowId }
                val col = visibleColumns.find { it.id == activeColId }
                if (row != null && col != null) {
                    val currentFiles = row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {

                        if (col.type == ColumnType.AUDIO) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).clickable {
                                        if (isRecording) {
                                            isRecording = false
                                            actions.onStopDbAudioRecording(block.id, row.id, col.id, false)
                                        } else {
                                            isRecording = true
                                            actions.onStartRecording()
                                        }
                                    }
                                )
                                Spacer(Modifier.width(12.dp))
                                if (isRecording) {
                                    val mins = recordingDuration / 60
                                    val secs = recordingDuration % 60
                                    Text(
                                        text = "Recording... ${mins}:${secs.toString().padStart(2, '0')}",
                                        fontFamily = PoppinsFont, color = MaterialTheme.colorScheme.error, fontSize = 14.sp
                                    )
                                } else {
                                    Text("Tap mic to record audio", fontFamily = PoppinsFont, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }

                        currentFiles.forEach { resourceEntry ->
                            val parts = resourceEntry.split("|")
                            val cleanFileName  = parts[0].substringAfterLast("/")
                            val resourceName   = if (parts.size > 1) parts[1] else cleanFileName

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (col.type == ColumnType.AUDIO) {
                                            if (playingFileUri == cleanFileName) {
                                                playingFileUri = null
                                                actions.onStopAudio()
                                            } else {
                                                if (playingFileUri != null) actions.onStopAudio()
                                                playingFileUri = cleanFileName
                                                actions.onPlayAudio(cleanFileName) {
                                                    if (playingFileUri == cleanFileName) playingFileUri = null
                                                }
                                            }
                                        } else {
                                            actions.onOpenFile(cleanFileName, "*/*")
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = if (col.type == ColumnType.AUDIO) {
                                        if (playingFileUri == cleanFileName) Icons.Default.Pause else Icons.Default.PlayArrow
                                    } else Icons.Default.InsertDriveFile

                                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = resourceName, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.8f))
                                }

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp).clickable {
                                        if (playingFileUri == cleanFileName) {
                                            playingFileUri = null
                                            actions.onStopAudio()
                                        }
                                        currentFiles.remove(resourceEntry)
                                        actions.onUpdateDbCell(block.id, row.id, col.id, currentFiles.joinToString(","))
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val rId  = row.id
                                    val cId  = col.id
                                    val isAud = col.type == ColumnType.AUDIO
                                    applyAction { actions.onRequestDbFilePicker(block.id, rId, cId, isAud) }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (col.type == ColumnType.AUDIO) "Upload audio track" else "Attach a new file",
                                fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // ── PRIORITY SELECTION ───────────────────────────────────────────────────
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
                                    val rowId = row.id
                                    val colId = activeColId ?: return@clickable
                                    applyAction { actions.onUpdateDbCell(block.id, rowId, colId, label) }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
                                Text(text = label, fontSize = 14.sp, fontFamily = PoppinsFont, color = color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            if (current == label) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (current.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DbOptionRow(icon = Icons.Default.Close, text = "Clear", color = MaterialTheme.colorScheme.error) {
                            val rowId = row.id
                            val colId = activeColId ?: return@DbOptionRow
                            applyAction { actions.onUpdateDbCell(block.id, rowId, colId, "") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── AGGREGATION ──────────────────────────────────────────────────────────
            DbSheetType.AGGREGATION -> {
                val col = visibleColumns.find { it.id == activeColId }
                if (col != null) {
                    val isNum = col.type == ColumnType.NUMBER || col.type == ColumnType.FORMULA || col.type == ColumnType.MONEY
                    val currentAgg = columnAggregations[col.id] ?: "None"

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    closeSheet()
                                    val newMap = columnAggregations.toMutableMap()
                                    newMap.remove(col.id)
                                    columnAggregations = newMap
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("None", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (currentAgg == "None") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        val groups = mutableListOf(
                            "Count"   to listOf("Count all", "Count values", "Count unique", "Count empty", "Count not empty"),
                            "Percent" to listOf("Percent empty", "Percent not empty")
                        )
                        if (isNum) groups.add("More options" to listOf("Sum", "Average", "Min", "Max", "Median", "Range"))

                        groups.forEach { (groupName, options) ->
                            val isExpanded = aggregationExpandedSection == groupName

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { aggregationExpandedSection = if (isExpanded) null else groupName }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(groupName, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                val rotation by animateFloatAsState(if (isExpanded) -90f else 90f)
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).rotate(rotation))
                            }

                            if (isExpanded) {
                                options.forEach { opt ->
                                    val isSelected = currentAgg == opt
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                closeSheet()
                                                val newMap = columnAggregations.toMutableMap()
                                                newMap[col.id] = opt
                                                columnAggregations = newMap
                                            }
                                            .padding(start = 40.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(opt, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── CURRENCY SELECTION ───────────────────────────────────────────────────
            DbSheetType.CURRENCY_SELECTION -> {
                val col = visibleColumns.find { it.id == activeColId }
                if (col != null) {
                    val currencies = listOf(
                        "$"  to "US Dollar",
                        "€"  to "Euro",
                        "£"  to "British Pound",
                        "¥"  to "Yen",
                        "₹"  to "Rupee",
                        "A$" to "Australian Dollar",
                        "C$" to "Canadian Dollar"
                    )
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        currencies.forEach { (symbol, name) ->
                            val isSelected = (columnCurrencies[col.id] ?: "$") == symbol
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        closeSheet()
                                        val newMap = columnCurrencies.toMutableMap()
                                        newMap[col.id] = symbol
                                        columnCurrencies = newMap
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$name ($symbol)", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).padding(vertical = 4.dp)) {
                    if (sheetTitle.isNotBlank()) {
                        Text(text = sheetTitle, fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp).padding(horizontal = 20.dp))
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
        // Title + Sort/Filter toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = block.title,
                onValueChange = { actions.onUpdateDbTitle(block.id, it) },
                textStyle = TextStyle(fontFamily = PoppinsFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    if (block.title.isEmpty()) {
                        Text(text = "Untitled Database", fontFamily = PoppinsFont, fontSize = 20.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
                },
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                enabled = !inSelectionMode
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val hasSort = block.activeSorts.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasSort) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = block.activeSorts.firstOrNull()?.columnId ?: visibleColumns.first().id
                                currentSheet = DbSheetType.SORT
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (hasSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.SORT)
                }

                Box {
                    val hasFilter = block.activeFilters.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = visibleColumns.first().id
                                textInput = ""
                                textInputMax = ""
                                filterOperator = "contains"
                                currentSheet = DbSheetType.FILTER
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (hasFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.FILTER)
                }
            }
        }

        // Active filter chips
        if (block.activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.activeFilters.forEach { filter ->
                    val colName = visibleColumns.find { it.id == filter.columnId }?.name ?: "?"
                    val label = when (filter.operator) {
                        "not_empty"    -> "$colName is not empty"
                        "empty"        -> "$colName is empty"
                        "checked"      -> "$colName is checked"
                        "unchecked"    -> "$colName is unchecked"
                        "priority"     -> "$colName = ${filter.value}"
                        "gt"           -> "$colName > ${filter.value}"
                        "lt"           -> "$colName < ${filter.value}"
                        "gte"          -> "$colName ≥ ${filter.value}"
                        "lte"          -> "$colName ≤ ${filter.value}"
                        "between"      -> { val p = filter.value.split("|"); if (p.size == 2) "$colName: ${p[0]} – ${p[1]}" else "$colName between" }
                        "before"       -> "$colName before ${filter.value}"
                        "after"        -> "$colName after ${filter.value}"
                        "starts_with"  -> "$colName starts with \"${filter.value}\""
                        "ends_with"    -> "$colName ends with \"${filter.value}\""
                        "not_contains" -> "$colName does not contain \"${filter.value}\""
                        "not_equals"   -> "$colName is not \"${filter.value}\""
                        else           -> "$colName ${filter.operator} \"${filter.value}\""
                    }

                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = label, fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp).clickable(enabled = !inSelectionMode) { actions.onRemoveDbFilter(block.id, filter) }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Table grid
        val bp = borderColor
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).haze(state = hazeState)) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.6.dp, borderColor1)
                ) {
                    Column {
                        // ── Header row ───────────────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .height(IntrinsicSize.Max)
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            visibleColumns.forEachIndexed { colIndex, col ->
                                val activeSort = block.activeSorts.find { it.columnId == col.id }

                                val typeIcon = when (col.type) {
                                    ColumnType.TEXT     -> Icons.Default.Subject
                                    ColumnType.NUMBER   -> Icons.Default.Numbers
                                    ColumnType.CHECKBOX -> Icons.Default.CheckBox
                                    ColumnType.DATE     -> Icons.Default.CalendarToday
                                    ColumnType.FORMULA  -> Icons.Default.Functions
                                    ColumnType.PHONE    -> Icons.Default.Phone
                                    ColumnType.EMAIL    -> Icons.Default.Email
                                    ColumnType.TAGS     -> Icons.Default.LocalOffer
                                    ColumnType.URL      -> Icons.Default.Link
                                    ColumnType.FILES    -> Icons.Default.AttachFile
                                    ColumnType.PRIORITY -> Icons.Default.Flag
                                    ColumnType.MONEY    -> Icons.Default.MonetizationOn
                                    ColumnType.AUDIO    -> Icons.Default.Mic
                                }

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .width(col.width.dp)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 44.dp)
                                            .drawBehind {
                                                val px = 0.5.dp.toPx()
                                                drawLine(bp, Offset(size.width, 0f), Offset(size.width, size.height), px)
                                                drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
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
                                                fontFamily = PoppinsFont, fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            if (activeSort != null) {
                                                if (block.activeSorts.size > 1) {
                                                    val layerIndex = block.activeSorts.indexOfFirst { it.columnId == col.id } + 1
                                                    Text(
                                                        text = "$layerIndex",
                                                        fontFamily = PoppinsFont, fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 2.dp)
                                                    )
                                                }
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
                                    .defaultMinSize(minHeight = 47.dp)
                                    .drawBehind {
                                        val px = 0.5.dp.toPx()
                                        drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                    }
                                    .clickable(enabled = !inSelectionMode) {
                                        actions.onAddDbColumn(block.id)
                                        coroutineScope.launch { delay(150); scrollState.animateScrollTo(scrollState.maxValue) }
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // ── Data rows ────────────────────────────────────────────────
                        visibleRows.forEach { row ->
                            Row(modifier = Modifier.height(IntrinsicSize.Max).defaultMinSize(minHeight = 44.dp)) {
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
                                                    val px = 0.5.dp.toPx()
                                                    drawLine(bp, Offset(size.width, 0f), Offset(size.width, size.height), px)
                                                    drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                                }
                                                .then(if (isHighlighted) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary) else Modifier)
                                                .pointerInput(inSelectionMode) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        var isLongPress = false
                                                        try {
                                                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                                                waitForUpOrCancellation()
                                                            }
                                                        } catch (e: PointerEventTimeoutCancellationException) {
                                                            isLongPress = true
                                                            currentEvent.changes.forEach { it.consume() }
                                                        }
                                                        if (isLongPress && !inSelectionMode) {
                                                            focusManager.clearFocus()
                                                            activeRowId = row.id
                                                            activeColId = col.id
                                                            currentSheet = DbSheetType.CELL_OPTIONS
                                                        }
                                                    }
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
                                                currencySymbol = columnCurrencies[col.id] ?: "$",
                                                isFormulaCurrency = formulaIsCurrency[col.id] == true,
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

                                // Trailing spacer — bottom edge only
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .drawBehind {
                                            val px = 0.5.dp.toPx()
                                            drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                        }
                                )
                            }
                        }
                    }
                }

                // ── Aggregation row ──────────────────────────────────────────────────
                Row(modifier = Modifier.height(IntrinsicSize.Max).defaultMinSize(minHeight = 36.dp)) {
                    visibleColumns.forEach { col ->
                        val aggType = columnAggregations[col.id]
                        val isActivelyEditing = currentSheet == DbSheetType.AGGREGATION && activeColId == col.id
                        val isCurr  = col.type == ColumnType.MONEY || (col.type == ColumnType.FORMULA && formulaIsCurrency[col.id] == true)
                        val prefix  = if (isCurr) (columnCurrencies[col.id] ?: "$") else ""

                        val displayValue = if (aggType == null) {
                            if (isActivelyEditing) "Calculate" else ""
                        } else {
                            val values  = visibleRows.mapNotNull { it.cells[col.id] }
                            val numbers = values.mapNotNull { it.toDoubleOrNull() }
                            fun Double.fmt() = if (this == this.toLong().toDouble()) this.toLong().toString() else ((this * 100.0).toLong() / 100.0).toString()

                            val result = when (aggType) {
                                "Count all"         -> visibleRows.size.toString()
                                "Count values"      -> values.count { it.isNotBlank() }.toString()
                                "Count unique"      -> values.filter { it.isNotBlank() }.distinct().size.toString()
                                "Count empty"       -> visibleRows.count { it.cells[col.id].isNullOrBlank() }.toString()
                                "Count not empty"   -> values.count { it.isNotBlank() }.toString()
                                "Percent empty"     -> if (visibleRows.isEmpty()) "0%" else "${(visibleRows.count { it.cells[col.id].isNullOrBlank() } * 100 / visibleRows.size)}%"
                                "Percent not empty" -> if (visibleRows.isEmpty()) "0%" else "${(values.count { it.isNotBlank() } * 100 / visibleRows.size)}%"
                                "Sum"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.sum().fmt()}"
                                "Average" -> if (numbers.isEmpty()) "" else "$prefix${(numbers.sum() / numbers.size).fmt()}"
                                "Min"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.minOrNull()?.fmt() ?: ""}"
                                "Max"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.maxOrNull()?.fmt() ?: ""}"
                                "Median"  -> {
                                    if (numbers.isEmpty()) ""
                                    else {
                                        val sorted = numbers.sorted()
                                        if (sorted.size % 2 == 0) "$prefix${((sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2).fmt()}"
                                        else "$prefix${sorted[sorted.size / 2].fmt()}"
                                    }
                                }
                                "Range" -> if (numbers.isEmpty()) "" else "$prefix${(numbers.maxOrNull()!! - numbers.minOrNull()!!).fmt()}"
                                else    -> ""
                            }
                            if (result.isEmpty()) aggType else "$aggType $result"
                        }

                        Box(
                            modifier = Modifier
                                .width(col.width.dp)
                                .fillMaxHeight()
                                .defaultMinSize(minHeight = 36.dp)
                                .clickable(enabled = !inSelectionMode) {
                                    activeColId = col.id
                                    currentSheet = DbSheetType.AGGREGATION
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = displayValue,
                                fontFamily = PoppinsFont, fontSize = 13.sp,
                                color = if (aggType == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        DesktopDbDropdown(activeColId == col.id && currentSheet == DbSheetType.AGGREGATION)
                    }
                    Box(modifier = Modifier.width(44.dp).fillMaxHeight().defaultMinSize(minHeight = 36.dp))
                }

                // ── Add row button ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !inSelectionMode) { actions.onAddDbRow(block.id) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(7.dp))
                    Text(text = "New Row", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (!isDesktopPlatform && currentSheet != DbSheetType.NONE) {
        InlyBottomSheet(expanded = true, onDismiss = { closeSheet() }, title = sheetTitle) { _ -> sheetContent() }
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
    currencySymbol: String = "$",
    isFormulaCurrency: Boolean = false,
    onValueChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onTagClick: () -> Unit,
    onFileClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    when (columnType) {
        ColumnType.TEXT, ColumnType.NUMBER, ColumnType.PHONE, ColumnType.EMAIL, ColumnType.URL, ColumnType.MONEY -> {
            var isFocused by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
            LaunchedEffect(value) {
                if (tfv.text != value) {
                    tfv = tfv.copy(text = value, selection = TextRange(tfv.selection.start.coerceAtMost(value.length)))
                }
            }

            Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inSelectionMode) focusRequester.requestFocus() }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (columnType == ColumnType.MONEY && (value.isNotBlank() || isFocused)) {
                        Text(text = currencySymbol, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                    }

                    val isSingleLine = columnType != ColumnType.TEXT
                    androidx.compose.runtime.key(isSingleLine) {
                        BasicTextField(
                            value = tfv,
                            onValueChange = { newValue ->
                                tfv = newValue
                                onValueChange(newValue.text)
                            },
                            enabled = !inSelectionMode,
                            textStyle = TextStyle(
                                fontFamily = PoppinsFont, fontSize = 14.sp,
                                color = if ((columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL) && value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if ((columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL) && value.isNotBlank()) TextDecoration.Underline else TextDecoration.None
                            ),
                            keyboardOptions = when (columnType) {
                                ColumnType.NUMBER, ColumnType.MONEY -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                ColumnType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                                ColumnType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email)
                                ColumnType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                                else -> KeyboardOptions.Default
                            },
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = isSingleLine,
                            modifier = Modifier.weight(1f).defaultMinSize(minWidth = cellWidth - 24.dp).focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused }
                        )
                    }

                    val isLinkType = columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL
                    if (isLinkType && value.isNotBlank() && !isFocused) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew, contentDescription = "Open Link", modifier = Modifier.size(16.dp).clickable {
                                when (columnType) {
                                    ColumnType.EMAIL -> try { uriHandler.openUri("mailto:$value") } catch (e: Exception) {}
                                    ColumnType.PHONE -> try { uriHandler.openUri("tel:$value") } catch (e: Exception) {}
                                    ColumnType.URL -> try {
                                        val url = if (!value.startsWith("http://") && !value.startsWith("https://")) "https://$value" else value
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {}
                                    else -> {}
                                }
                            }, tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        ColumnType.CHECKBOX -> {
            val isChecked = value == "true"
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = isChecked, onCheckedChange = { if (!inSelectionMode) onValueChange(it.toString()) }, modifier = Modifier.scale(0.9f).size(18.dp),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.surface, checkmarkColor = MaterialTheme.colorScheme.primary, uncheckedColor = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
        ColumnType.DATE -> {
            Text(
                text = value.ifEmpty { "—" }, fontFamily = PoppinsFont, fontSize = 14.sp,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !inSelectionMode) { onDateClick() }
            )
        }
        ColumnType.FORMULA -> {
            val formulaScrollState = rememberScrollState()
            val isInvalid = value.equals("NaN", ignoreCase = true) || value.startsWith("Error", ignoreCase = true)

            val displayValue = if (isInvalid) "" else {
                if (isFormulaCurrency && value.toDoubleOrNull() != null) {
                    val num = value.toDouble()
                    val fmt = if (num == num.toLong().toDouble()) num.toLong().toString() else ((num * 100.0).toLong() / 100.0).toString()
                    "$currencySymbol$fmt"
                } else value
            }

            Text(
                text = displayValue, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1,
                modifier = Modifier.horizontalScroll(formulaScrollState).mouseScrollable(formulaScrollState)
            )
        }
        ColumnType.TAGS -> {
            val activeTagIds = value.split(",").filter { it.isNotBlank() }
            val activeTags = activeTagIds.mapNotNull { id -> globalTags.find { it.tagId == id } }

            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onTagClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
                if (activeTags.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeTags.forEach { tag ->
                            val tagColor = try { Color(tag.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            Surface(shape = RoundedCornerShape(4.dp), color = tagColor.copy(alpha = 0.15f)) {
                                Text(text = tag.name, fontSize = 12.sp, fontFamily = PoppinsFont, color = tagColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        ColumnType.FILES, ColumnType.AUDIO -> {
            val resources = value.split(",").filter { it.isNotBlank() }

            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onFileClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
                if (resources.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        resources.forEach { resourceEntry ->
                            val parts = resourceEntry.split("|")
                            val cleanFileName = parts[0].substringAfterLast("/")
                            val resourceName = if (parts.size > 1) parts[1] else cleanFileName

                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    Icon(
                                        imageVector = if (columnType == ColumnType.AUDIO) Icons.Default.Mic else Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = resourceName, fontSize = 12.sp, fontFamily = PoppinsFont, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ColumnType.PRIORITY -> {
            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onPriorityClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
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
                    Surface(shape = RoundedCornerShape(4.dp), color = chipColor.copy(alpha = 0.15f)) {
                        Text(text = value, fontSize = 13.sp, fontFamily = PoppinsFont, color = chipColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }
    }
}