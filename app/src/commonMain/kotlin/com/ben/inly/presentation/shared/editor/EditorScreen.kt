package com.ben.inly.presentation.shared.editor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
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
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.VoiceBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.LocalInlyExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DefaultCornerShape = RoundedCornerShape(6.dp)

private fun Modifier.customInlyShadow(shape: Shape): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.25f),
    ambientColor = Color.Black.copy(alpha = 0.10f)
)

/**
 * Defines all possible user interactions within the editor.
 * Grouping these into an interface prevents passing dozens of individual lambdas down the tree.
 */
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
}

/**
 * The core text and media editor surface used across the app.
 */
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
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    val currentBlocks by rememberUpdatedState(blocks)

    LaunchedEffect(focusRequest?.id) {
        focusRequest?.let { request ->
            val id = request.id
            activeBlockId = id

            var requester = focusRequesters[id]
            var attempts = 0
            while (requester == null && attempts < 10) {
                delay(10)
                requester = focusRequesters[id]
                attempts++
            }

            try { requester?.requestFocus() } catch (_: Exception) {}
            actions.onClearFocusRequest()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = 60.dp + bottomContentPadding + toolbarOffset + 40.dp
            )
        ) {
            if (headerContent != null) {
                item(key = "page_header", contentType = "PageHeader") {
                    headerContent()
                }
            }

            // Task summary badge
            item(key = "stats_header", contentType = "StatsHeader") {
                val allTasks = blocks.filterIsInstance<CheckboxBlock>()
                if (allTasks.isNotEmpty()) {
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
                        TaskBadge(
                            icon = Icons.Default.CheckCircle,
                            label = "$doneCount Done"
                        )
                    }
                }
            }

            items(
                items = blocks,
                key = { it.id },
                contentType = { it::class.simpleName }
            ) { block ->
                val req = remember(block.id) {
                    focusRequesters.getOrPut(block.id) { FocusRequester() }
                }
                DisposableEffect(block.id) {
                    onDispose { focusRequesters.remove(block.id) }
                }

                val isSelected = selectedBlockIds.contains(block.id)
                val isActive = activeBlockId == block.id
                val targetedFocusRequest = if (focusRequest?.id == block.id) focusRequest else null

                NoteBlockItem(
                    block = block,
                    globalTags = globalTags,
                    actions = actions,
                    focusRequest = targetedFocusRequest,
                    focusRequester = req,
                    isSelected = isSelected,
                    inSelectionMode = isSelectionMode,
                    isActiveBlock = isActive,
                    onFocus = {
                        activeBlockId = block.id
                        actions.onFocusBlock(block.id)
                    }
                )
            }

            item(key = "outside_tap_area", contentType = "TapArea") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    activeBlockId = null
                                    actions.onOutsideTap()
                                },
                                onDoubleTap = {
                                    scope.launch {
                                        val lastBlock = currentBlocks.lastOrNull() ?: return@launch
                                        val isMediaBlock = lastBlock is BookmarkBlock
                                                || lastBlock is ImageBlock
                                                || lastBlock is DocumentBlock
                                                || lastBlock is DatabaseBlock
                                                || lastBlock is VoiceBlock
                                        if (isMediaBlock) {
                                            actions.onFocusBlock(lastBlock.id)
                                            actions.onAddBlankBlock()
                                        } else {
                                            activeBlockId = lastBlock.id
                                            actions.onFocusBlock(lastBlock.id)
                                            focusRequesters[lastBlock.id]?.requestFocus()
                                            keyboardController?.show()
                                        }
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

// Task badge — extracted from inline lambda for clarity
@Composable
private fun TaskBadge(icon: ImageVector, label: String) {
    Surface(shape = DefaultCornerShape, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 12.sp,
                fontFamily = BricolageFont,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Selection mode observer
@Composable
fun SelectionModeObserver(isSelectionMode: Boolean, onSelectionModeChange: (Boolean) -> Unit) {
    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }
}

// Block selection pill
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
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val isDesktop = isDesktopPlatform

    val pillColor = if (isDesktop) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    val tint = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

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
                Box(
                    Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(tint.copy(alpha = 0.2f))
                )
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
                Text("$selectedCount", fontFamily = BricolageFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tint)
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

// Editor toolbar
@Composable
fun EditorToolbar(
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDesktop = isDesktopPlatform
    val scrollState = rememberScrollState()

    val toolbarColor = if (isDesktop) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    val tint = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Surface(
        shape = DefaultCornerShape,
        color = toolbarColor,
        modifier = modifier
            .fillMaxWidth()
            .customInlyShadow(DefaultCornerShape)
            .clip(DefaultCornerShape)
            .then(if (isDesktop) Modifier else Modifier.hazeChild(state = hazeState))
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .then(if (isDesktop) Modifier.mouseScrollable(scrollState) else Modifier)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val iconSize = Modifier.size(21.dp)
                    val divider = @Composable {
                        Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                    }

                    // --- TOOLBAR ITEMS ---
                    IconButton(onClick = { onChangeBlockType("text") }) { Icon(Icons.AutoMirrored.Filled.Subject, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onChangeBlockType("h1") }) { Text("H1", fontFamily = BricolageFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tint) }
                    IconButton(onClick = { onChangeBlockType("h2") }) { Text("H2", fontFamily = BricolageFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tint) }
                    divider()

                    IconButton(onClick = { onChangeBlockType("checkbox") }) { Icon(Icons.Default.CheckBox, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onChangeBlockType("bullet") }) { Icon(Icons.Default.FormatListBulleted, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onChangeBlockType("number") }) { Icon(Icons.Default.FormatListNumbered, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onChangeBlockType("toggle") }) { Icon(Icons.Default.ChevronRight, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onChangeBlockType("code") }) { Icon(Icons.Default.Code, null, tint = tint, modifier = iconSize) }
                    divider()

                    IconButton(onClick = { onToggleFormat("bold") }) { Icon(Icons.Default.FormatBold, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onToggleFormat("italic") }) { Icon(Icons.Default.FormatItalic, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onToggleFormat("underline") }) { Icon(Icons.Default.FormatUnderlined, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onToggleFormat("strike") }) { Icon(Icons.Default.StrikethroughS, null, tint = tint, modifier = iconSize) }
                    divider()

                    IconButton(onClick = { onChangeBlockType("voice") }) { Icon(Icons.Default.Mic, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onInsertMediaBlock("image") }) { Icon(Icons.Default.Image, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onInsertMediaBlock("document") }) { Icon(Icons.Default.InsertDriveFile, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onInsertMediaBlock("bookmark") }) { Icon(Icons.Default.BookmarkBorder, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onInsertMediaBlock("database") }) { Icon(Icons.Default.TableChart, null, tint = tint, modifier = iconSize) }
                    divider()

                    IconButton(onClick = { onAdjustIndentation(false) }) { Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, null, tint = tint, modifier = iconSize) }
                    IconButton(onClick = { onAdjustIndentation(true) }) { Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, null, tint = tint, modifier = iconSize) }
                }

                // Keyboard dismiss (MOBILE ONLY)
                if (!isDesktopPlatform) {
                    Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.3f)))
                    IconButton(
                        onClick = { keyboardController?.hide() },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardHide, "Close", tint = tint, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }
    }
}