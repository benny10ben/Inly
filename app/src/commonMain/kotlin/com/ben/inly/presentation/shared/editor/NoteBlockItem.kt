package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.QuoteBlock
import com.ben.inly.domain.model.RowContainerBlock
import com.ben.inly.domain.model.SketchBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.zIndex
import com.ben.inly.domain.model.SolidDividerBlock
import com.ben.inly.domain.model.ThreeDotDividerBlock
import com.ben.inly.presentation.shared.components.MinimalDatePickerDialog
import com.ben.inly.presentation.shared.components.MinimalTimePickerDialog
import com.ben.inly.presentation.shared.components.ReminderPresetMenu
import com.ben.inly.presentation.shared.components.TimePresetMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import com.ben.inly.presentation.shared.editor.blockViews.DatabaseBlockView
import com.ben.inly.presentation.shared.editor.blockViews.DocumentBlockView
import com.ben.inly.presentation.shared.editor.blockViews.ImageBlockView
import com.ben.inly.presentation.shared.editor.blockViews.AudioBlockView
import com.ben.inly.presentation.shared.editor.blockViews.BookmarkBlockView
import com.ben.inly.presentation.shared.editor.blockViews.plugins.SketchCanvasBlockView
import com.ben.inly.presentation.shared.editor.components.DesktopCursor
import com.ben.inly.presentation.shared.editor.components.DragDropState
import com.ben.inly.presentation.shared.editor.components.DropTargetZone
import com.ben.inly.presentation.shared.editor.components.LocalBlockBoundsRegistry
import com.ben.inly.presentation.shared.editor.components.LocalDragDropState
import com.ben.inly.presentation.shared.editor.components.desktopPointerCursor

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

val DefaultBlockShape = RoundedCornerShape(12.dp)

// All NoteBlock subtypes are @Immutable: never mutate in place, always .copy()
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBlockItem(
    block: NoteBlock,
    globalTags: ImmutableList<TagEntity>,
    actions: EditorActions,
    focusRequest: FocusRequest?,
    selectedBlockIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    activeBlockId: String?,
    onFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSlashMenu: Boolean = false,
    slashQuery: String = "",
    onDismissSlashMenu: () -> Unit = {},
) {
    // RECURSIVE LAYOUT: ROW CONTAINERS
    if (block is RowContainerBlock) {
        if (!isDesktopPlatform) {
            Column(modifier = modifier.fillMaxWidth()) {
                block.columns.forEach { column ->
                    column.blocks.filter { !it.isDeleted }.forEach { nestedBlock ->
                        NoteBlockItem(
                            block = nestedBlock,
                            globalTags = globalTags,
                            actions = actions,
                            focusRequest = if (focusRequest?.id == nestedBlock.id) focusRequest else null,
                            selectedBlockIds = selectedBlockIds,
                            inSelectionMode = inSelectionMode,
                            activeBlockId = activeBlockId,
                            onFocus = onFocus,
                            showSlashMenu = showSlashMenu,
                            slashQuery = slashQuery,
                            onDismissSlashMenu = onDismissSlashMenu
                        )
                    }
                }
            }
            return
        }

        var rowWidthPx by remember { mutableFloatStateOf(1f) }
        var currentWeights by remember(block.columns.map { it.id }) {
            mutableStateOf(block.columns.map { it.weight })
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onGloballyPositioned { rowWidthPx = it.size.width.toFloat() }
        ) {
            block.columns.forEachIndexed { index, column ->
                Column(
                    modifier = Modifier.weight(currentWeights[index])
                ) {
                    column.blocks.filter { !it.isDeleted }.forEach { nestedBlock ->
                        NoteBlockItem(
                            block = nestedBlock,
                            globalTags = globalTags,
                            actions = actions,
                            focusRequest = if (focusRequest?.id == nestedBlock.id) focusRequest else null,
                            selectedBlockIds = selectedBlockIds,
                            inSelectionMode = inSelectionMode,
                            activeBlockId = activeBlockId,
                            onFocus = onFocus,
                            showSlashMenu = showSlashMenu,
                            slashQuery = slashQuery,
                            onDismissSlashMenu = onDismissSlashMenu
                        )
                    }
                }

                if (index < block.columns.lastIndex) {
                    var isDividerHovered by remember { mutableStateOf(false) }
                    var isDragging by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                            .desktopPointerCursor(DesktopCursor.RESIZE_HORIZONTAL)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        when (event.type) {
                                            PointerEventType.Enter -> isDividerHovered = true
                                            PointerEventType.Exit -> if (!isDragging) isDividerHovered = false
                                        }
                                    }
                                }
                            }
                            .pointerInput(rowWidthPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        isDividerHovered = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        isDividerHovered = false
                                        actions.onUpdateColumnWeights(block.id, currentWeights)
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        isDividerHovered = false
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val totalWeight = currentWeights.sum()
                                    val weightDelta = (dragAmount / rowWidthPx) * totalWeight
                                    val newWeights = currentWeights.toMutableList()
                                    val newLeft = newWeights[index] + weightDelta
                                    val newRight = newWeights[index + 1] - weightDelta
                                    val minWeight = totalWeight * 0.1f
                                    if (newLeft > minWeight && newRight > minWeight) {
                                        newWeights[index] = newLeft
                                        newWeights[index + 1] = newRight
                                        currentWeights = newWeights
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(if (isDividerHovered || isDragging) 4.dp else 2.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        isDividerHovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }
        return
    }

    // STANDARD BLOCK LOGIC
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val isActiveBlock = block.id == activeBlockId
    val isSelected = selectedBlockIds.contains(block.id)

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
        is BookmarkBlock, is ImageBlock, is DocumentBlock, is DatabaseBlock, is VoiceBlock, is SketchBlock -> ""
        else -> ""
    }

    // LOCAL STATE FOR INSTANT TYPING
    var showPresetMenu by remember { mutableStateOf(false) }
    var showTimePresetMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // DRAG & DROP STATE
    val dragState = LocalDragDropState.current
    val boundsRegistry = LocalBlockBoundsRegistry.current
    var isHovered by remember { mutableStateOf(false) }
    var blockBounds by remember { mutableStateOf<Rect?>(null) }
    var handlePositionInWindow by remember { mutableStateOf(Offset.Zero) }

    var gutterZone by remember { mutableStateOf(0) }

    val isBeingDragged = dragState.value.isDragging && dragState.value.draggedBlockId == block.id
    val sourceAlpha by animateFloatAsState(
        targetValue = if (isBeingDragged) 0.4f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "dragSourceAlpha"
    )
    val sourceScale by animateFloatAsState(
        targetValue = if (isBeingDragged) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "dragSourceScale"
    )

    DisposableEffect(block.id) {
        onDispose { boundsRegistry.remove(block.id) }
    }

    // FOCUS MANAGEMENT
    LaunchedEffect(isFocused, imeBottom, blockHeight) {
        if (isFocused && imeBottom > 0) {
            val bufferPx = with(density) { 100.dp.toPx() }
            val targetRect = androidx.compose.ui.geometry.Rect(left = 0f, top = 0f, right = 1f, bottom = blockHeight + bufferPx)
            bringIntoViewRequester.bringIntoView(targetRect)
        }
    }

    LaunchedEffect(focusRequest?.nonce) {
        if (focusRequest != null && focusRequest.id == block.id) {
            delay(50)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) { }
            actions.onClearFocusRequest()
        }
    }

    // STYLING
    val baseStyle = when (block) {
        is HeadingBlock -> TextStyle(
            fontFamily = PoppinsFont,
            fontSize = if (block.level == 1) {
                if (isDesktopPlatform) 32.sp else 26.sp
            } else {
                if (isDesktopPlatform) 24.sp else 20.sp
            },
            lineHeight = if (block.level == 1) {
                if (isDesktopPlatform) 40.sp else 32.sp
            } else {
                if (isDesktopPlatform) 32.sp else 26.sp
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        is CodeBlock -> TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = if (isDesktopPlatform) 15.sp else 15.sp,
            lineHeight = if (isDesktopPlatform) 22.sp else 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        is QuoteBlock -> TextStyle(
            fontFamily = PoppinsFont,
            fontSize = if (isDesktopPlatform) 16.sp else 15.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = if (isDesktopPlatform) 32.sp else 28.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        else -> TextStyle(
            fontFamily = PoppinsFont,
            fontSize = if (isDesktopPlatform) 16.sp else 15.sp,
            lineHeight = if (isDesktopPlatform) 28.sp else 24.sp,
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
        backgroundColor = MaterialTheme.colorScheme.surface
    )

    val isTextBased = block !is BookmarkBlock && block !is ImageBlock && block !is DocumentBlock && block !is DatabaseBlock && block !is VoiceBlock && block !is SketchBlock && block !is SolidDividerBlock && block !is ThreeDotDividerBlock
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

    // DROP INDICATOR
    val insertLineZone = if (isDesktopPlatform && !dragState.value.isDragging) gutterZone else 0
    val insertLineAlpha by animateFloatAsState(
        targetValue = if (insertLineZone != 0) 0.6f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "insertLineAlpha"
    )
    val isDropTarget = dragState.value.isDragging && dragState.value.hoveredBlockId == block.id
    val dropZone = if (isDropTarget) dragState.value.activeDropZone else DropTargetZone.NONE
    val indicatorColor = MaterialTheme.colorScheme.primary
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (dropZone != DropTargetZone.NONE) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "dropIndicatorAlpha"
    )

    // RENDER BLOCK CONTENT
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = sourceAlpha
                scaleX = sourceScale
                scaleY = sourceScale
            }
            .fillMaxWidth()
            .background(selectionBg)
            .onGloballyPositioned { layoutCoordinates ->
                val b = layoutCoordinates.boundsInWindow()
                blockBounds = b
                boundsRegistry.update(block.id, b)
            }
            .drawWithContent {
                drawContent()

                // drag-drop insertion line
                if (indicatorAlpha > 0.01f && dropZone != DropTargetZone.NONE) {
                    val stroke = 3.dp.toPx()
                    val dotR = 4.dp.toPx()
                    val c = indicatorColor.copy(alpha = indicatorAlpha)
                    when (dropZone) {
                        DropTargetZone.TOP -> {
                            val y = stroke
                            drawLine(c, Offset(dotR * 2, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(dotR, y))
                        }
                        DropTargetZone.BOTTOM -> {
                            val y = size.height - stroke
                            drawLine(c, Offset(dotR * 2, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(dotR, y))
                        }
                        DropTargetZone.LEFT -> {
                            val x = stroke
                            drawLine(c, Offset(x, dotR * 2), Offset(x, size.height), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(x, dotR))
                        }
                        DropTargetZone.RIGHT -> {
                            val x = size.width - stroke
                            drawLine(c, Offset(x, dotR * 2), Offset(x, size.height), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(x, dotR))
                        }
                        else -> {}
                    }
                }

                // hover-insert line (synced with + button)
                if (isDesktopPlatform && !dragState.value.isDragging && insertLineAlpha > 0.01f) {
                    val stroke = 2.dp.toPx()
                    val dotR = 3.dp.toPx()
                    val c = indicatorColor.copy(alpha = insertLineAlpha)
                    when (insertLineZone) {
                        -1 -> {
                            val y = stroke
                            drawLine(c, Offset(dotR * 2, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(dotR, y))
                        }
                        1 -> {
                            val y = size.height - stroke
                            drawLine(c, Offset(dotR * 2, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
                            drawCircle(c, dotR, Offset(dotR, y))
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> {
                                isHovered = false
                                gutterZone = 0
                            }
                            PointerEventType.Move -> {
                                if (isDesktopPlatform && !inSelectionMode) {
                                    val y = event.changes.firstOrNull()?.position?.y ?: 0f
                                    val h = blockBounds?.height ?: 0f
                                    gutterZone = when {
                                        h > 0f && y < 6.dp.toPx()         -> -1
                                        h > 0f && y > h - 6.dp.toPx()     ->  1
                                        else                                ->  0
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = {
                    if (!dragState.value.isDragging) actions.onToggleSelection(block.id)
                }
            )
    ) {
        // Desktop slash menu
        if (isDesktopPlatform && isActiveBlock && showSlashMenu) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onDismissSlashMenu,
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .width(290.dp)
                    .heightIn(max = 400.dp)
            ) {
                DesktopSlashMenuContent(
                    query = slashQuery,
                    onChangeBlockType = { actions.onChangeBlockType(it) },
                    onToggleFormat = { actions.onToggleFormat(it) },
                    onAdjustIndentation = { actions.onAdjustIndentation(it) },
                    onInsertMediaBlock = { actions.onInsertMediaBlock(it) }
                )
            }
        }

        // + ABOVE overlay
        if (isDesktopPlatform) {
            val showInsert = isHovered && !inSelectionMode && !dragState.value.isDragging
            AnimatedVisibility(
                visible = showInsert && gutterZone == -1,
                enter = fadeIn(tween(80)),
                exit  = fadeOut(tween(80)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = (-7).dp)
                    .zIndex(10f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                        .clickable { actions.onAddBlockAbove(block.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert block above",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // + BELOW overlay
            AnimatedVisibility(
                visible = showInsert && gutterZone == 1,
                enter = fadeIn(tween(80)),
                exit  = fadeOut(tween(80)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp, y = 7.dp)
                    .zIndex(10f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                        .clickable { actions.onAddBlockBelow(block.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert block below",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .padding(vertical = internalVerticalPadding),
            verticalAlignment = Alignment.Top
        ) {
            // DRAG HANDLE UI (DESKTOP)
            if (isDesktopPlatform) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                        .offset(x = (-8).dp)
                        .onGloballyPositioned {
                            handlePositionInWindow = it.boundsInWindow().topLeft
                        }
                ) {
                    val isThisDragged = dragState.value.draggedBlockId == block.id
                    val showHandle = !inSelectionMode && (isThisDragged || (isHovered && gutterZone == 0))

                    this@Row.AnimatedVisibility(
                        visible = showHandle,
                        enter = fadeIn(tween(80)),
                        exit  = fadeOut(tween(80)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .desktopPointerCursor(DesktopCursor.HAND)
                                .pointerInput(block.id) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val bounds = blockBounds
                                            val pointerInWindow = handlePositionInWindow + offset
                                            val grab = if (bounds != null) pointerInWindow - bounds.topLeft else Offset.Zero
                                            val size = if (bounds != null) {
                                                IntSize(bounds.width.toInt(), bounds.height.toInt())
                                            } else IntSize.Zero
                                            dragState.value = DragDropState(
                                                isDragging = true,
                                                draggedBlockId = block.id,
                                                pointerPositionInWindow = pointerInWindow,
                                                grabOffsetInBlock = grab,
                                                draggedBlockSize = size
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = dragState.value
                                            dragState.value = current.copy(
                                                pointerPositionInWindow = current.pointerPositionInWindow + dragAmount
                                            )
                                        },
                                        onDragEnd = {
                                            val finalState = dragState.value
                                            if (finalState.isValidDrop) {
                                                actions.onMoveBlock(
                                                    sourceId = finalState.draggedBlockId!!,
                                                    targetId = finalState.hoveredBlockId!!,
                                                    zone = finalState.activeDropZone
                                                )
                                            }
                                            dragState.value = DragDropState()
                                        },
                                        onDragCancel = { dragState.value = DragDropState() }
                                    )
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Drag Block",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

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
                                modifier = Modifier.scale(0.9f).size(16.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.surface,
                                    checkmarkColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        is BulletedListBlock -> Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(textStyle.color))
                        is NumberedListBlock -> Text("${block.number}.", style = textStyle.copy(fontSize = 17.sp))
                        is ToggleBlock -> {
                            val rotation by animateFloatAsState(if (block.isExpanded) 90f else 0f, label = "toggleRotation")
                            Icon(
                                Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp).rotate(rotation).clickable { actions.onToggleExpand(block.id) }
                            )
                        }
                        else -> {}
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val DefaultBlockShape = RoundedCornerShape(12.dp)
            val textFieldWrapperModifier = (if (block is CodeBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, DefaultBlockShape).padding(12.dp)
            } else if (block is QuoteBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp).drawBehind {
                    drawLine(color = primaryColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 4.dp.toPx())
                }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
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
                    if (currentlyFocused) onFocus(block.id)
                }

            Column(modifier = textFieldWrapperModifier) {
                if (isTextBased) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {

                            IsolatedEditorTextField(
                                initialText = text,
                                blockId = block.id,
                                isCodeBlock = block is CodeBlock,
                                textStyle = textStyle,
                                inSelectionMode = inSelectionMode,
                                focusRequester = focusRequester,
                                onUpdateText = { id, newText -> actions.onUpdateText(id, newText) },
                                onEnterPressed = { id, before, after -> actions.onEnterPressed(id, before, after) },
                                onBackspaceOnEmpty = { id -> actions.onBackspaceOnEmpty(id) },
                                onTextLayout = { textLayoutResult = it }
                            )

                        }

                        if (!isFocused && !inSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(block.id) {
                                        detectTapGestures(
                                            onTap = {
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            },
                                            onDoubleTap = {
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
                                            Icons.Default.CalendarToday, "Date",
                                            modifier = Modifier.size(15.dp),
                                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    ReminderPresetMenu(
                                        expanded = showPresetMenu,
                                        onDismiss = { showPresetMenu = false },
                                        onPresetSelected = {
                                            actions.onUpdateReminder(
                                                block.id,
                                                it
                                            )
                                        },
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
                                            Icons.Default.AccessTime, "Time",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    TimePresetMenu(
                                        expanded = showTimePresetMenu,
                                        onDismiss = { showTimePresetMenu = false },
                                        onPresetSelected = {
                                            actions.onUpdateReminder(
                                                block.id,
                                                it
                                            )
                                        },
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
                            is BookmarkBlock -> BookmarkBlockView(
                                block, inSelectionMode,
                                { actions.onToggleSelection(block.id) },
                                { actions.onUrlSubmit(block.id, it) }
                            )
                            is ImageBlock -> ImageBlockView(
                                block, inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) },
                                onRequestPicker = { actions.onRequestImagePicker(block.id) },
                                onRequestCamera = { actions.onRequestCamera(block.id) },
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
                            is VoiceBlock -> AudioBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) },
                                onRemoveVoice = { actions.onRemoveVoice(block.id) },
                                onStartRecording = { actions.onStartRecording() },
                                onStopRecording = { cancel -> actions.onStopRecording(block.id, cancel) },
                                onPlayAudio = { path, onComplete -> actions.onPlayAudio(path, onComplete) },
                                onStopAudio = { actions.onStopAudio() }
                            )
                            is SketchBlock -> SketchCanvasBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onStrokesChanged = { actions.onUpdateSketch(block.id, it) },
                                onScrollEnabledChange = { actions.setScrollEnabled(it) }
                            )
                            is SolidDividerBlock -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .height(1.5.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                                )
                            }
                            is ThreeDotDividerBlock -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dotSize = 6.dp
                                    val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                    Spacer(Modifier.width(16.dp))
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                    Spacer(Modifier.width(16.dp))
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        if (block.isPinned) {
            Icon(
                Icons.Default.PushPin, "Pinned",
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
                        val currentInstant = block.reminderTimestamp?.let {
                            Instant.fromEpochMilliseconds(it)
                        } ?: Clock.System.now()
                        val currentDt = currentInstant.toLocalDateTime(tz)
                        val newDt = LocalDateTime(
                            currentDt.year, currentDt.monthNumber, currentDt.dayOfMonth,
                            hour, minute, 0, 0
                        )
                        actions.onUpdateReminder(
                            block.id,
                            newDt.toInstant(tz).toEpochMilliseconds()
                        )
                        showTimePicker = false
                    }
                )
            }
        }

        if (inSelectionMode) {
            Box(Modifier.matchParentSize().clickable(onClick = { actions.onToggleSelection(block.id) }))
        }
    }
}

@Composable
fun IsolatedEditorTextField(
    initialText: String,
    blockId: String,
    isCodeBlock: Boolean,
    textStyle: TextStyle,
    inSelectionMode: Boolean,
    focusRequester: FocusRequester,
    onUpdateText: (String, String) -> Unit,
    onEnterPressed: (String, String, String) -> Unit,
    onBackspaceOnEmpty: (String) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var tfv by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    var lastSentText by remember { mutableStateOf(initialText) }

    // Sync DOWN
    LaunchedEffect(initialText) {
        if (tfv.text != initialText && initialText != lastSentText) {
            val wasAtEnd = tfv.selection.start == tfv.text.length

            val safeStart = if (wasAtEnd) initialText.length else tfv.selection.start.coerceAtMost(initialText.length)
            val safeEnd = if (wasAtEnd) initialText.length else tfv.selection.end.coerceAtMost(initialText.length)

            tfv = tfv.copy(text = initialText, selection = TextRange(safeStart, safeEnd))
        }
    }

    // Sync UP (Debounced)
    LaunchedEffect(tfv.text) {
        if (tfv.text != initialText) {

            val lastSlashIndex = tfv.text.lastIndexOf('/')
            val isActivelySearching = lastSlashIndex != -1 && !tfv.text.substring(lastSlashIndex).contains(" ")

            if (!isActivelySearching) {
                delay(400L)
            }

            lastSentText = tfv.text
            onUpdateText(blockId, tfv.text)
        }
    }

    BasicTextField(
        value = tfv,
//        keyboardOptions = KeyboardOptions(
//            keyboardType = KeyboardType.Text,
//            autoCorrectEnabled = false,   // older Compose: autoCorrect = false
//            capitalization = KeyboardCapitalization.Sentences
//        ),
        onTextLayout = onTextLayout,
        onValueChange = { newValue ->
            val newText = newValue.text
            if (!isCodeBlock && newText.contains('\n')) {
                val splitIndex = newText.indexOf('\n')
                val textBefore = newText.substring(0, splitIndex)
                val textAfter = newText.substring(splitIndex + 1).replace("\n", "")

                tfv = newValue.copy(text = textBefore, selection = TextRange(textBefore.length))

                onUpdateText(blockId, textBefore)
                onEnterPressed(blockId, textBefore, textAfter)
            } else {
                tfv = newValue
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                val isBackspace = event.key == Key.Backspace
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter

                if (isBackspace && tfv.text.isEmpty()) {
                    if (event.type == KeyEventType.KeyDown) {
                        onBackspaceOnEmpty(blockId)
                    }
                    return@onPreviewKeyEvent true
                }
                if (isEnter && !isCodeBlock) {
                    if (event.type == KeyEventType.KeyDown) {
                        val cursor = tfv.selection.start
                        val textBefore = tfv.text.substring(0, cursor)
                        val textAfter = tfv.text.substring(cursor)
                        onEnterPressed(blockId, textBefore, textAfter)
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