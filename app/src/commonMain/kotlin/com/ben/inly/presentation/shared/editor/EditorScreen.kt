package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.QuoteBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.model.ToggleBlock
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import com.ben.inly.domain.model.RowContainerBlock
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.isActive
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

private val DefaultCornerShape = RoundedCornerShape(12.dp)

private fun Modifier.customInlyShadow(shape: Shape): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.25f),
    ambientColor = Color.Black.copy(alpha = 0.10f)
)

data class SlashMenuItemData(
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

data class SlashMenuSectionData(
    val title: String,
    val items: List<SlashMenuItemData>
)

// MAIN  = the quick-action strip
// SLASH = the menu shown while typing "/" (driven by the typed query)
// MENU  = the full "everything" menu opened from the + button
enum class MobileMenuState { MAIN, SLASH, MENU }

object GlobalEditorState {
    var currentlyFocusedBlockId: String? = null
}

object EditorEventBus {
    val insertSlashEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cleanupSlashEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

@Stable
interface EditorActions {
    fun onClearFocusRequest()
    fun onUpdateText(id: String, text: String)
    fun onToggleCheckbox(id: String, checked: Boolean)
    fun onToggleExpand(id: String)
    fun onFocusBlock(id: String)
    fun onChangeBlockType(type: String)
    fun onToggleFormat(format: String)
    fun onAdjustIndentation(increase: Boolean)
    fun onEnterPressed(id: String, before: String, after: String)
    fun onBackspaceOnEmpty(id: String)
    fun onToggleSelection(id: String)
    fun onUpdateReminder(id: String, timestamp: Long?)
    fun onUrlSubmit(id: String, url: String)
    fun onImagePicked(id: String, uri: String)
    fun onDocumentPicked(id: String, uri: String)
    fun onAddBlankBlock()
    fun onInsertMediaBlock(type: String)
    fun onOutsideTap()
    fun onUpdateDbTitle(id: String, title: String)
    fun onAddDbRow(id: String)
    fun onAddDbColumn(id: String)
    fun onUpdateDbCell(blockId: String, rowId: String, colId: String, value: String)
    fun onUpdateDbColumn(blockId: String, colId: String, name: String, type: ColumnType)
    fun onUpdateDbSort(blockId: String, colId: String, isAscending: Boolean?)
    fun onAddDbFilter(blockId: String, colId: String, operator: String, value: String)
    fun onRemoveDbFilter(blockId: String, config: FilterConfig)
    fun onReorderDbColumns(blockId: String, from: Int, to: Int)
    fun onUpdateDbFormula(blockId: String, colId: String, expression: String)
    fun onDeleteDbColumn(blockId: String, colId: String)
    fun onDeleteDbRow(blockId: String, rowId: String)
    fun onAddDbRowAt(blockId: String, index: Int)
    fun onAddDbColumnAt(blockId: String, index: Int)
    fun onUpdateDbColumnWidth(blockId: String, colId: String, width: Int)
    fun onVoiceRecorded(id: String, filePath: String, duration: Int)
    fun onRemoveVoice(id: String)
    fun onStartRecording()
    fun onStopRecording(blockId: String, cancel: Boolean)
    fun onPlayAudio(filePath: String, onComplete: () -> Unit)
    fun onStopAudio()
    fun onDeleteImageBlock(id: String)
    fun onCreateGlobalTag(name: String, colorHex: String): String
    fun onRequestImagePicker(blockId: String)
    fun onRequestDocumentPicker(blockId: String)
    fun onOpenFile(filePath: String, mimeType: String)
    fun onUndo()
    fun onRedo()
    fun onRequestDbFilePicker(blockId: String, rowId: String, colId: String, isAudio: Boolean)
    fun onStopDbAudioRecording(blockId: String, rowId: String, colId: String, cancel: Boolean)
    fun onTogglePin()
    fun setScrollEnabled(enabled: Boolean) {}
    fun onUpdateSketch(id: String, strokes: List<com.ben.inly.domain.model.Stroke>)
    fun onMoveBlock(sourceId: String, targetId: String, zone: DropTargetZone)
    fun onUpdateColumnWeights(rowId: String, weights: List<Float>)
    fun onAddBlockAbove(id: String)
    fun onAddBlockBelow(id: String)
    fun onUpdateDbAggregation(blockId: String, colId: String, aggregationType: String?)
    fun onUpdateDbCurrency(blockId: String, colId: String, symbol: String)
    fun onUpdateDbFormulaCurrency(blockId: String, colId: String, enabled: Boolean)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    blocks: List<NoteBlock>,
    globalTags: List<TagEntity>,
    actions: EditorActions,
    focusRequest: FocusRequest?,
    selectedBlockIds: Set<String>,
    bottomContentPadding: Dp = 0.dp,
    topContentPadding: Dp = 0.dp,
    toolbarOffset: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    hazeState: HazeState,
    mobileMenuState: MobileMenuState = MobileMenuState.MAIN,
    onMobileMenuStateChange: (MobileMenuState) -> Unit = {},
    slashQuery: String = "",
    onSlashQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    val currentBlocks by rememberUpdatedState(blocks)

    var localFocusRequest by remember { mutableStateOf<FocusRequest?>(null) }
    val activeFocusRequest = focusRequest ?: localFocusRequest

    var showSlashMenu by remember { mutableStateOf(false) }

    val latestBlocks by rememberUpdatedState(blocks)
    val latestActiveBlockId by rememberUpdatedState(activeBlockId)
    val latestSlashQuery by rememberUpdatedState(slashQuery)
    val latestMobileMenuState by rememberUpdatedState(mobileMenuState)
    val latestOnMobileMenuStateChange by rememberUpdatedState(onMobileMenuStateChange)
    val latestOnSlashQueryChange by rememberUpdatedState(onSlashQueryChange)

    val clearSlashAndExecute: (() -> Unit) -> Unit = { executionBlock ->
        latestActiveBlockId?.let { id ->
            val block = findBlockRecursive(latestBlocks, id)
            val currentText = when (block) {
                is TextBlock -> block.text
                is HeadingBlock -> block.text
                is CheckboxBlock -> block.text
                is BulletedListBlock -> block.text
                is NumberedListBlock -> block.text
                is ToggleBlock -> block.text
                is QuoteBlock -> block.text
                else -> null
            }
            if (currentText != null) {
                val lastSlashIndex = currentText.lastIndexOf('/')
                if (lastSlashIndex != -1 && lastSlashIndex == currentText.length - 1 - latestSlashQuery.length) {
                    actions.onUpdateText(id, currentText.substring(0, lastSlashIndex))
                }
            }
        }

        if (isDesktopPlatform) showSlashMenu = false
        latestOnMobileMenuStateChange(MobileMenuState.MAIN)
        latestOnSlashQueryChange("")

        executionBlock()
    }

    val wrappedActions = remember(actions) {
        object : EditorActions by actions {
            override fun onClearFocusRequest() {
                localFocusRequest = null
                actions.onClearFocusRequest()
            }
            override fun onUpdateText(id: String, text: String) {
                actions.onUpdateText(id, text)
                val lastSlashIndex = text.lastIndexOf('/')
                if (lastSlashIndex != -1) {
                    if (isDesktopPlatform) {
                        showSlashMenu = true
                    } else {
                        latestOnMobileMenuStateChange(MobileMenuState.SLASH)
                    }
                    latestOnSlashQueryChange(text.substring(lastSlashIndex + 1))
                } else {
                    if (isDesktopPlatform) {
                        showSlashMenu = false
                    } else if (latestMobileMenuState == MobileMenuState.SLASH) {
                        latestOnMobileMenuStateChange(MobileMenuState.MAIN)
                    }
                    latestOnSlashQueryChange("")
                }
            }
            override fun onChangeBlockType(type: String) = clearSlashAndExecute { actions.onChangeBlockType(type) }
            override fun onToggleFormat(format: String) = clearSlashAndExecute { actions.onToggleFormat(format) }
            override fun onAdjustIndentation(increase: Boolean) = clearSlashAndExecute { actions.onAdjustIndentation(increase) }
            override fun onInsertMediaBlock(type: String) = clearSlashAndExecute { actions.onInsertMediaBlock(type) }
            override fun onTogglePin() = actions.onTogglePin()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            EditorEventBus.insertSlashEvent.collect {
                val targetId = GlobalEditorState.currentlyFocusedBlockId ?: latestActiveBlockId
                if (targetId != null) {
                    val block = findBlockRecursive(latestBlocks, targetId)
                    val currentText = when (block) {
                        is TextBlock -> block.text
                        is HeadingBlock -> block.text
                        is CheckboxBlock -> block.text
                        is BulletedListBlock -> block.text
                        is NumberedListBlock -> block.text
                        is ToggleBlock -> block.text
                        is QuoteBlock -> block.text
                        else -> null
                    }
                    if (currentText != null) {
                        wrappedActions.onUpdateText(targetId, "$currentText/")
                        localFocusRequest = FocusRequest(id = targetId, placeCursorAtEnd = true)
                    }
                }
            }
        }
        launch {
            EditorEventBus.cleanupSlashEvent.collect {
                val targetId = GlobalEditorState.currentlyFocusedBlockId ?: latestActiveBlockId
                if (targetId != null) {
                    val block = findBlockRecursive(latestBlocks, targetId)
                    val currentText = when (block) {
                        is TextBlock -> block.text
                        is HeadingBlock -> block.text
                        is CheckboxBlock -> block.text
                        is BulletedListBlock -> block.text
                        is NumberedListBlock -> block.text
                        is ToggleBlock -> block.text
                        is QuoteBlock -> block.text
                        else -> null
                    }
                    if (currentText != null) {
                        val lastSlashIndex = currentText.lastIndexOf('/')
                        if (lastSlashIndex != -1 && lastSlashIndex == currentText.length - 1 - latestSlashQuery.length) {
                            actions.onUpdateText(targetId, currentText.substring(0, lastSlashIndex))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(activeFocusRequest?.nonce) {
        val request = activeFocusRequest ?: return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos {}

        val index = currentBlocks.indexOfFirst { it.id == request.id }
        if (index != -1) {
            val hasHeader = if (headerContent != null) 1 else 0
            val hasStats = if (currentBlocks.any { it is CheckboxBlock }) 1 else 0
            val targetLazyColumnIndex = index + hasHeader + hasStats

            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetLazyColumnIndex }

            if (itemInfo == null) {
                try {
                    val viewportHeight = layoutInfo.viewportSize.height
                    val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                    listState.scrollToItem(index = targetLazyColumnIndex, scrollOffset = offset)
                } catch (_: Exception) {}
            } else {
                val itemBottom = itemInfo.offset + itemInfo.size
                val viewportBottom = layoutInfo.viewportEndOffset

                if (itemBottom > viewportBottom) {
                    try {
                        listState.animateScrollBy((itemBottom - viewportBottom).toFloat() + 60f)
                    } catch (_: Exception) {}
                } else if (itemInfo.offset < layoutInfo.viewportStartOffset) {
                    try {
                        listState.animateScrollBy((itemInfo.offset - layoutInfo.viewportStartOffset).toFloat() - 60f)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val density = LocalDensity.current
    var rootPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    val dragState = remember { mutableStateOf(DragDropState()) }
    val boundsRegistry = remember { BlockBoundsRegistry() }

    val dynamicBottomPadding by animateDpAsState(
        targetValue = if (mobileMenuState != MobileMenuState.MAIN) 280.dp else 100.dp,
        label = "menuPadding"
    )

    LaunchedEffect(mobileMenuState) {
        if (mobileMenuState != MobileMenuState.MAIN) {
            activeBlockId?.let { id ->
                val index = currentBlocks.indexOfFirst { it.id == id }
                if (index != -1) {
                    val hasHeader = if (headerContent != null) 1 else 0
                    val hasStats = if (currentBlocks.any { it is CheckboxBlock }) 1 else 0
                    val targetIndex = index + hasHeader + hasStats

                    androidx.compose.runtime.withFrameNanos {}

                    val layoutInfo = listState.layoutInfo
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }

                    if (itemInfo != null) {
                        val menuHeightPx = with(density) { 340.dp.toPx() }
                        val viewportBottom = layoutInfo.viewportEndOffset
                        val menuTopPx = viewportBottom - menuHeightPx
                        val itemBottomPx = itemInfo.offset + itemInfo.size

                        if (itemBottomPx > menuTopPx) {
                            try {
                                listState.animateScrollBy((itemBottomPx - menuTopPx) + 60f)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    // THE hit-test. One effect for the whole editor.
    LaunchedEffect(dragState.value.pointerPositionInWindow, dragState.value.isDragging) {
        val s = dragState.value
        if (!s.isDragging || s.draggedBlockId == null) return@LaunchedEffect

        val hit = boundsRegistry.hitTest(s.pointerPositionInWindow)
        if (hit == null || hit.first == s.draggedBlockId) {
            if (s.hoveredBlockId != null || s.activeDropZone != DropTargetZone.NONE) {
                dragState.value = s.copy(hoveredBlockId = null, activeDropZone = DropTargetZone.NONE)
            }
            return@LaunchedEffect
        }

        val (id, rect) = hit
        val localX = s.pointerPositionInWindow.x - rect.left
        val localY = s.pointerPositionInWindow.y - rect.top

        val zone = when {
            localX < rect.width * 0.15f -> DropTargetZone.LEFT
            localX > rect.width * 0.85f -> DropTargetZone.RIGHT
            localY < rect.height * 0.5f -> DropTargetZone.TOP
            else                        -> DropTargetZone.BOTTOM
        }

        if (s.hoveredBlockId != id || s.activeDropZone != zone) {
            dragState.value = s.copy(hoveredBlockId = id, activeDropZone = zone)
        }
    }

    // Auto-scroll while dragging near viewport edges.
    LaunchedEffect(dragState.value.isDragging) {
        if (!dragState.value.isDragging) return@LaunchedEffect

        val edgeBandPx = with(density) { 120.dp.toPx() }
        val maxSpeedPxPerFrame = with(density) { 18.dp.toPx() }

        while (isActive && dragState.value.isDragging) {
            val layoutInfo = listState.layoutInfo
            val viewportTop = layoutInfo.viewportStartOffset.toFloat()
            val viewportBottom = layoutInfo.viewportEndOffset.toFloat()

            val pointerYInViewport =
                dragState.value.pointerPositionInWindow.y - rootPositionInWindow.y

            val distFromTop = pointerYInViewport - viewportTop
            val distFromBottom = viewportBottom - pointerYInViewport

            val delta = when {
                distFromTop < edgeBandPx -> {
                    val ratio = (1f - (distFromTop / edgeBandPx)).coerceIn(0f, 1f)
                    -maxSpeedPxPerFrame * ratio
                }
                distFromBottom < edgeBandPx -> {
                    val ratio = (1f - (distFromBottom / edgeBandPx)).coerceIn(0f, 1f)
                    maxSpeedPxPerFrame * ratio
                }
                else -> 0f
            }

            if (delta != 0f) {
                try { listState.scrollBy(delta) } catch (_: Exception) {}
            }

            withFrameNanos {}
        }
    }

    val immutableTags = remember(globalTags) { globalTags.toImmutableList() }
    val immutableSelectedIds = remember(selectedBlockIds) { selectedBlockIds.toImmutableSet() }

    val onFocusBlock: (String) -> Unit = remember(wrappedActions) {
        { focusedId ->
            activeBlockId = focusedId
            GlobalEditorState.currentlyFocusedBlockId = focusedId
            wrappedActions.onFocusBlock(focusedId)
        }
    }
    val onDismissSlash: () -> Unit = remember { { showSlashMenu = false } }

    CompositionLocalProvider(
        LocalDragDropState provides dragState,
        LocalBlockBoundsRegistry provides boundsRegistry
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { rootPositionInWindow = it.boundsInWindow().topLeft }
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                activeBlockId = null
                                GlobalEditorState.currentlyFocusedBlockId = null
                                localFocusRequest = null
                                showSlashMenu = false
                                onMobileMenuStateChange(MobileMenuState.MAIN)
                                wrappedActions.onOutsideTap()
                            },
                            onDoubleTap = {
                                val lastBlock = currentBlocks.lastOrNull() ?: return@detectTapGestures
                                val isMediaBlock = lastBlock is BookmarkBlock
                                        || lastBlock is ImageBlock
                                        || lastBlock is DocumentBlock
                                        || lastBlock is DatabaseBlock
                                        || lastBlock is VoiceBlock

                                if (isMediaBlock) {
                                    wrappedActions.onFocusBlock(lastBlock.id)
                                    wrappedActions.onAddBlankBlock()
                                } else {
                                    activeBlockId = lastBlock.id
                                    wrappedActions.onFocusBlock(lastBlock.id)
                                    localFocusRequest = FocusRequest(id = lastBlock.id, placeCursorAtEnd = true)
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(top = topContentPadding, bottom = 0.dp)
            ) {
                if (headerContent != null) {
                    item(key = "page_header", contentType = "PageHeader") {
                        headerContent()
                    }
                }

                val allTasks = blocks.filterIsInstance<CheckboxBlock>()
                if (allTasks.isNotEmpty()) {
                    item(key = "stats_header", contentType = "StatsHeader") {
                        val doneCount = allTasks.count { it.isChecked }
                        val pendingCount = allTasks.size - doneCount

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskBadge(
                                icon = Icons.Default.RadioButtonUnchecked,
                                label = "$pendingCount Pending"
                            )
                            TaskBadge(icon = Icons.Default.CheckCircle, label = "$doneCount Done")
                        }
                    }
                }

                items(
                    items = blocks,
                    key = { it.id },
                    contentType = { it::class.simpleName }
                ) { block ->
                    val isActive = activeBlockId == block.id
                    val targetedFocusRequest = when {
                        activeFocusRequest == null -> null
                        block is RowContainerBlock -> activeFocusRequest
                        activeFocusRequest.id == block.id -> activeFocusRequest
                        else -> null
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        NoteBlockItem(
                            block = block,
                            globalTags = immutableTags,
                            actions = wrappedActions,
                            focusRequest = targetedFocusRequest,
                            selectedBlockIds = immutableSelectedIds,
                            inSelectionMode = isSelectionMode,
                            activeBlockId = activeBlockId,
                            onFocus = onFocusBlock,
                            showSlashMenu = showSlashMenu,
                            slashQuery = slashQuery,
                            onDismissSlashMenu = onDismissSlash
                        )
                    }
                }
                item(key = "bottom_tap_area", contentType = "BottomTapArea") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dynamicBottomPadding + bottomContentPadding + toolbarOffset)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        val lastBlock = latestBlocks.lastOrNull()
                                            ?: return@detectTapGestures
                                        val isMediaBlock = lastBlock is BookmarkBlock
                                                || lastBlock is ImageBlock
                                                || lastBlock is DocumentBlock
                                                || lastBlock is DatabaseBlock
                                                || lastBlock is VoiceBlock

                                        if (isMediaBlock) {
                                            wrappedActions.onFocusBlock(lastBlock.id)
                                            wrappedActions.onAddBlankBlock()
                                        } else {
                                            activeBlockId = lastBlock.id
                                            wrappedActions.onFocusBlock(lastBlock.id)
                                            localFocusRequest = FocusRequest(
                                                id = lastBlock.id,
                                                placeCursorAtEnd = true
                                            )
                                        }
                                    },
                                    onTap = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        activeBlockId = null
                                        GlobalEditorState.currentlyFocusedBlockId = null
                                        localFocusRequest = null
                                        showSlashMenu = false
                                        onMobileMenuStateChange(MobileMenuState.MAIN)
                                        wrappedActions.onOutsideTap()
                                    }
                                )
                            }
                    )
                }
            }

            // Desktop drag ghost
            val state = dragState.value
            if (isDesktopPlatform && state.isDragging && state.draggedBlockId != null) {
                val draggedBlock = remember(state.draggedBlockId, blocks) {
                    findBlockRecursive(blocks, state.draggedBlockId!!)
                }

                val ghostWidth = with(density) { state.draggedBlockSize.width.toDp() }

                Box(
                    modifier = Modifier
                        .offset {
                            val topLeftInWindow = state.pointerPositionInWindow - state.grabOffsetInBlock
                            IntOffset(
                                (topLeftInWindow.x - rootPositionInWindow.x).roundToInt(),
                                (topLeftInWindow.y - rootPositionInWindow.y).roundToInt()
                            )
                        }
                        .let { if (ghostWidth > 0.dp) it.width(ghostWidth) else it.widthIn(max = 320.dp) }
                        .zIndex(1000f)
                        .graphicsLayer {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            alpha = 0.9f
                            shadowElevation = with(density) { 18.dp.toPx() }
                            shape = RoundedCornerShape(10.dp)
                            clip = false
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    DragGhostContent(draggedBlock)
                }
            }
        }
    }
}

private fun findBlockRecursive(blocks: List<NoteBlock>, id: String): NoteBlock? {
    for (b in blocks) {
        if (b.id == id) return b
        if (b is RowContainerBlock) {
            b.columns.forEach { col ->
                findBlockRecursive(col.blocks, id)?.let { return it }
            }
        }
    }
    return null
}

@Composable
private fun DragGhostContent(block: NoteBlock?) {
    when (block) {
        is HeadingBlock -> Text(
            text = block.text.ifEmpty { "Heading" },
            fontFamily = PoppinsFont,
            fontSize = if (block.level == 1) 18.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        is CheckboxBlock -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)
            )
            GhostText(block.text.ifEmpty { "To-do" })
        }
        is BulletedListBlock -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            GhostText(block.text.ifEmpty { "List item" })
        }
        is NumberedListBlock -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${block.number}.", fontFamily = PoppinsFont, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            GhostText(block.text.ifEmpty { "List item" })
        }
        is ToggleBlock -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            GhostText(block.text.ifEmpty { "Toggle" })
        }
        is QuoteBlock -> GhostText(block.text.ifEmpty { "Quote" })
        is TextBlock -> GhostText(block.text.ifEmpty { "Empty" })
        is ImageBlock -> GhostMediaLabel(Icons.Default.Image, "Image")
        is DocumentBlock -> GhostMediaLabel(Icons.Default.InsertDriveFile, "File")
        is BookmarkBlock -> GhostMediaLabel(Icons.Default.Link, "Bookmark")
        is DatabaseBlock -> GhostMediaLabel(Icons.Default.GridOn, "Database")
        is VoiceBlock -> GhostMediaLabel(Icons.Default.Mic, "Voice note")
        else -> GhostText("Block")
    }
}

@Composable
private fun GhostText(text: String) {
    Text(
        text = text,
        fontFamily = PoppinsFont,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun GhostMediaLabel(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(label, fontFamily = PoppinsFont, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditorToolbar(
    mobileMenuState: MobileMenuState,
    onMenuStateChange: (MobileMenuState) -> Unit,
    query: String,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectCurrentBlock: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    if (isDesktopPlatform) return

    val keyboardController = LocalSoftwareKeyboardController.current
    val tint = MaterialTheme.colorScheme.primary
    val iconSize = 19.dp

    KmpBackHandler(enabled = mobileMenuState != MobileMenuState.MAIN) {
        onMenuStateChange(MobileMenuState.MAIN)
    }

    Surface(
        shape = DefaultCornerShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        modifier = modifier
            .fillMaxWidth()
            .customInlyShadow(DefaultCornerShape)
            .clip(DefaultCornerShape)
            .hazeChild(state = hazeState)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                when (mobileMenuState) {
                    MobileMenuState.MAIN -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                ToolbarButton(enabled = canUndo, onClick = onUndo) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo",
                                        tint = if (canUndo) tint else tint.copy(alpha = 0.3f),
                                        modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(enabled = canRedo, onClick = onRedo) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo",
                                        tint = if (canRedo) tint else tint.copy(alpha = 0.3f),
                                        modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = {
                                    keyboardController?.hide()
                                    onSelectCurrentBlock()
                                }) {
                                    Icon(Icons.Default.AdsClick, "Select Block", tint = tint, modifier = Modifier.size(iconSize))
                                }

                                ToolbarDivider(tint)

                                ToolbarButton(onClick = { onChangeBlockType("text") }) {
                                    Icon(Icons.AutoMirrored.Filled.Subject, "Text", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarLabel("H1", tint) { onChangeBlockType("h1") }
                                ToolbarLabel("H2", tint) { onChangeBlockType("h2") }
                                ToolbarButton(onClick = { onChangeBlockType("checkbox") }) {
                                    Icon(Icons.Default.CheckBox, "Checkbox", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onChangeBlockType("bullet") }) {
                                    Icon(Icons.Default.FormatListBulleted, "Bulleted list", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onChangeBlockType("number") }) {
                                    Icon(Icons.Default.FormatListNumbered, "Numbered list", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onChangeBlockType("toggle") }) {
                                    Icon(Icons.Default.ChevronRight, "Toggle list", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onChangeBlockType("quote") }) {
                                    Icon(Icons.Default.FormatQuote, "Quote", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onChangeBlockType("code") }) {
                                    Icon(Icons.Default.Code, "Code", tint = tint, modifier = Modifier.size(iconSize))
                                }

                                ToolbarDivider(tint)

                                ToolbarButton(onClick = { onToggleFormat("bold") }) {
                                    Icon(Icons.Default.FormatBold, "Bold", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onToggleFormat("italic") }) {
                                    Icon(Icons.Default.FormatItalic, "Italic", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onToggleFormat("strike") }) {
                                    Icon(Icons.Default.StrikethroughS, "Strikethrough", tint = tint, modifier = Modifier.size(iconSize))
                                }
                                ToolbarButton(onClick = { onToggleFormat("underline") }) {
                                    Icon(Icons.Default.FormatUnderlined, "Underline", tint = tint, modifier = Modifier.size(iconSize))
                                }

                                ToolbarDivider(tint)

                                ToolbarButton(onClick = { onMenuStateChange(MobileMenuState.MENU) }) {
                                    Icon(Icons.Default.Add, "More blocks", tint = tint, modifier = Modifier.size(iconSize + 1.dp))
                                }
                            }

                            ToolbarDivider(tint)
                            ToolbarButton(onClick = { keyboardController?.hide() }) {
                                Icon(Icons.Default.KeyboardHide, "Close Keyboard", tint = tint, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                    MobileMenuState.SLASH -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) })
                            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                DesktopSlashMenuContent(
                                    query = query,
                                    onChangeBlockType = {
                                        EditorEventBus.cleanupSlashEvent.tryEmit(Unit)
                                        onChangeBlockType(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onToggleFormat = {
                                        EditorEventBus.cleanupSlashEvent.tryEmit(Unit)
                                        onToggleFormat(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onAdjustIndentation = {
                                        EditorEventBus.cleanupSlashEvent.tryEmit(Unit)
                                        onAdjustIndentation(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onInsertMediaBlock = {
                                        EditorEventBus.cleanupSlashEvent.tryEmit(Unit)
                                        onInsertMediaBlock(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    }
                                )
                            }
                        }
                    }
                    MobileMenuState.MENU -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) })
                            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                                DesktopSlashMenuContent(
                                    query = "",
                                    onChangeBlockType = {
                                        onChangeBlockType(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onToggleFormat = {
                                        onToggleFormat(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onAdjustIndentation = {
                                        onAdjustIndentation(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    },
                                    onInsertMediaBlock = {
                                        onInsertMediaBlock(it)
                                        onMenuStateChange(MobileMenuState.MAIN)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ToolbarLabel(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = PoppinsFont, fontWeight = FontWeight.Normal, fontSize = 17.sp, color = tint)
    }
}

@Composable
private fun ToolbarDivider(tint: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(18.dp)
            .background(tint.copy(alpha = 0.2f))
    )
}

@Composable
private fun MenuDragHandle(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 15f) {
                        onClose()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun DesktopSlashMenuContent(
    query: String,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onInsertMediaBlock: (String) -> Unit
) {
    val sections = remember(
        onChangeBlockType,
        onToggleFormat,
        onAdjustIndentation,
        onInsertMediaBlock
    ) {
        listOf(
            SlashMenuSectionData("Basic Blocks", listOf(
                SlashMenuItemData("Text", Icons.AutoMirrored.Filled.Subject) { onChangeBlockType("text") },
                SlashMenuItemData("Heading 1", Icons.Default.Title) { onChangeBlockType("h1") },
                SlashMenuItemData("Heading 2", Icons.Default.Title) { onChangeBlockType("h2") },
                SlashMenuItemData("To-do List", Icons.Default.CheckBox) { onChangeBlockType("checkbox") },
                SlashMenuItemData("Bulleted List", Icons.Default.FormatListBulleted) { onChangeBlockType("bullet") },
                SlashMenuItemData("Numbered List", Icons.Default.FormatListNumbered) { onChangeBlockType("number") },
                SlashMenuItemData("Toggle List", Icons.Default.ChevronRight) { onChangeBlockType("toggle") },
                SlashMenuItemData("Quote", Icons.Default.FormatQuote) { onChangeBlockType("quote") },
                SlashMenuItemData("Code Block", Icons.Default.Code) { onChangeBlockType("code") }
            )),
            SlashMenuSectionData("Media & Links", listOf(
                SlashMenuItemData("Voice Note", Icons.Default.Mic) { onChangeBlockType("voice") },
                SlashMenuItemData("Image", Icons.Default.Image) { onInsertMediaBlock("image") },
                SlashMenuItemData("Document / File", Icons.Default.InsertDriveFile) { onInsertMediaBlock("document") },
                SlashMenuItemData("Web Bookmark", Icons.Default.BookmarkBorder) { onInsertMediaBlock("bookmark") },
                SlashMenuItemData("Database / Table", Icons.Default.TableChart) { onInsertMediaBlock("database") }
            )),
            SlashMenuSectionData("Inline Text Formatting", listOf(
                SlashMenuItemData("Bold Text", Icons.Default.FormatBold) { onToggleFormat("bold") },
                SlashMenuItemData("Italic Text", Icons.Default.FormatItalic) { onToggleFormat("italic") },
                SlashMenuItemData("Underline Text", Icons.Default.FormatUnderlined) { onToggleFormat("underline") },
                SlashMenuItemData("Strikethrough Text", Icons.Default.StrikethroughS) { onToggleFormat("strike") }
            )),
            SlashMenuSectionData("Indentation", listOf(
                SlashMenuItemData("Decrease Indent", Icons.AutoMirrored.Filled.FormatIndentDecrease) { onAdjustIndentation(false) },
                SlashMenuItemData("Increase Indent", Icons.AutoMirrored.Filled.FormatIndentIncrease) { onAdjustIndentation(true) }
            )),
//            SlashMenuSectionData("Plugins & Embeds", listOf(
//                SlashMenuItemData("Sketch Board", Icons.Default.Draw) { onInsertMediaBlock("sketch") }
//            )),
        )
    }

    val filteredSections = sections.map { section ->
        section.copy(items = section.items.filter { item ->
            query.isBlank() || item.label.contains(query, ignoreCase = true)
        })
    }.filter { it.items.isNotEmpty() }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (filteredSections.isEmpty()) {
            Text(
                text = "No results found",
                fontSize = 13.sp,
                fontFamily = PoppinsFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            filteredSections.forEachIndexed { index, section ->
                SlashMenuHeader(section.title)

                section.items.forEach { item ->
                    SlashMenuItem(
                        text = item.label,
                        icon = item.icon,
                        onClick = item.action
                    )
                }

                if (index < filteredSections.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SlashMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SlashMenuHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PoppinsFont,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun TaskBadge(icon: ImageVector, label: String) {
    Surface(shape = DefaultCornerShape, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, fontSize = 12.sp, fontFamily = PoppinsFont, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SelectionModeObserver(isSelectionMode: Boolean, onSelectionModeChange: (Boolean) -> Unit) {
    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }
}

@Composable
fun BlockSelectionPill(
    isVisible: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onAddBlockAbove: () -> Unit,
    onAddBlockBelow: () -> Unit,
    onTogglePin: () -> Unit,
    isSelectionPinned: Boolean = false,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val isDesktop = isDesktopPlatform

    val pillColor = if (isDesktop) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    val tint = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Surface(
            shape = DefaultCornerShape,
            color = pillColor,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .customInlyShadow(DefaultCornerShape)
                .clip(DefaultCornerShape)
                .then(if (isDesktop) Modifier else Modifier.hazeChild(state = hazeState))
        ) {
            val scrollState = rememberScrollState()
            val divider = @Composable {
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val iconSize = 18.dp
                Icon(Icons.Default.Close, null, modifier = Modifier.size(iconSize).clickable { onClearSelection() }, tint = tint)
                Text("$selectedCount", fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tint)
                divider()

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelectionPinned) tint.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onTogglePin() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        if (isSelectionPinned) "Unpin Block" else "Pin Block",
                        modifier = Modifier.size(iconSize),
                        tint = tint
                    )
                }
                divider()

                Icon(Icons.Default.SelectAll, "Select All", modifier = Modifier.size(iconSize).clickable { onSelectAll() }, tint = tint)
                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(iconSize).clickable { onCopy() }, tint = tint)
                Icon(Icons.Default.ContentCut, "Cut", modifier = Modifier.size(iconSize).clickable { onCut() }, tint = tint)
                divider()
                Icon(Icons.Default.ArrowUpward, "Add above", modifier = Modifier.size(iconSize).clickable { onAddBlockAbove() }, tint = tint)
                Icon(Icons.Default.ArrowDownward, "Add below", modifier = Modifier.size(iconSize).clickable { onAddBlockBelow() }, tint = tint)
                divider()
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(iconSize).clickable { onDelete() }, tint = tint)
            }
        }
    }
}