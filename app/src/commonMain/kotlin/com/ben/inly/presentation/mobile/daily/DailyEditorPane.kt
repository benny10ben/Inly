package com.ben.inly.presentation.mobile.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.Stroke
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.EditorToolbar
import com.ben.inly.presentation.shared.editor.GlobalEditorState
import com.ben.inly.presentation.shared.editor.MobileMenuState
import com.ben.inly.presentation.shared.editor.components.DropTargetZone
import com.ben.inly.presentation.mobile.home.note.SubNotePanel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Single-day daily editor pane. The caller selects the day via viewModel.selectDate(date);
 * this renders viewModel.visibleBlocks for whatever day is currently loaded. No pager.
 *
 * Used by the desktop merged screen (right panel) and by DailyScreen's desktop branch.
 * Owns its own editor menu/keyboard state and its sub-note panel.
 */
@Composable
fun DailyEditorPane(
    viewModel: DailyEditorViewModel,
    hazeState: HazeState,
    bottomContentPadding: Dp = 0.dp,
    isSidebarVisible: Boolean = true,
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onNavigateToEditor: (String) -> Unit = {},
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> },
    onSelectionModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val allLinkableNotes by viewModel.allLinkableNotes.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val blocks by viewModel.visibleBlocks.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val globalTags by viewModel.globalTags.collectAsState()

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val selectedBlocksList = blocks.filter { it.id in selectedBlockIds }
    val isSelectionPinned = selectedBlocksList.isNotEmpty() && selectedBlocksList.all { it.isPinned }

    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var isKeyboardOpen by remember { mutableStateOf(false) }
    var previousImeBottom by remember { mutableIntStateOf(0) }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && imeBottom >= previousImeBottom) {
            isKeyboardOpen = true
        } else if (imeBottom < previousImeBottom) {
            isKeyboardOpen = false
        }
        if (imeBottom == 0) {
            isKeyboardOpen = false
        }
        previousImeBottom = imeBottom
    }

    var mobileMenuState by remember { mutableStateOf(MobileMenuState.MAIN) }
    var slashQuery by remember { mutableStateOf("") }

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen && mobileMenuState != MobileMenuState.MAIN) {
            mobileMenuState = MobileMenuState.MAIN
        }
    }

    val showToolbar = !isSelectionMode && (isKeyboardOpen || isDesktopPlatform)

    var subNotePanelId by remember { mutableStateOf<String?>(null) }

    val actions = remember(viewModel, onOpenFile) {
        object : EditorActions {
            override fun onClearFocusRequest() = viewModel.clearFocusRequest()
            override fun onUpdateText(id: String, text: String) = viewModel.updateBlockText(id, text)
            override fun onToggleCheckbox(id: String, checked: Boolean) = viewModel.toggleCheckbox(id, checked)
            override fun onToggleExpand(id: String) = viewModel.toggleToggleBlock(id)
            override fun onFocusBlock(id: String) = viewModel.setFocusedBlock(id)
            override fun onChangeBlockType(type: String) = viewModel.changeFocusedBlockType(type)
            override fun onToggleFormat(format: String) = viewModel.toggleFormat(format)
            override fun onAdjustIndentation(increase: Boolean) = viewModel.adjustIndentation(increase)
            override fun onEnterPressed(id: String, before: String, after: String) = viewModel.handleEnter(id, before, after)
            override fun onBackspaceOnEmpty(id: String) = viewModel.handleBackspaceOnEmpty(id)
            override fun onToggleSelection(id: String) = viewModel.toggleSelection(id)
            override fun onUpdateReminder(id: String, timestamp: Long?) = viewModel.updateReminder(id, timestamp)
            override fun onUrlSubmit(id: String, url: String) = viewModel.handleUrlSubmit(id, url)
            override fun onImagePicked(id: String, uri: String) = viewModel.handleImagePicked(id, uri)
            override fun onDocumentPicked(id: String, uri: String) = viewModel.handleDocumentPicked(id, uri)
            override fun onAddBlankBlock() = viewModel.addBlankBlockBelowFocused()
            override fun onInsertMediaBlock(type: String) = viewModel.insertNewMediaBlock(type)
            override fun onOutsideTap() {}
            override fun onUpdateDbTitle(id: String, title: String) = viewModel.updateDbTitle(id, title)
            override fun onAddDbRow(id: String) = viewModel.addDbRow(id)
            override fun onAddDbColumn(id: String) = viewModel.addDbColumn(id)
            override fun onUpdateDbCell(blockId: String, rowId: String, colId: String, value: String) = viewModel.updateDbCell(blockId, rowId, colId, value)
            override fun onUpdateDbColumn(blockId: String, colId: String, name: String, type: ColumnType) = viewModel.updateDbColumn(blockId, colId, name, type)
            override fun onUpdateDbSort(blockId: String, colId: String, isAscending: Boolean?) = viewModel.updateDbSort(blockId, colId, isAscending)
            override fun onAddDbFilter(blockId: String, colId: String, operator: String, value: String) = viewModel.addDbFilter(blockId, colId, operator, value)
            override fun onRemoveDbFilter(blockId: String, config: FilterConfig) = viewModel.removeDbFilter(blockId, config)
            override fun onReorderDbColumns(blockId: String, from: Int, to: Int) = viewModel.reorderDbColumns(blockId, from, to)
            override fun onUpdateDbFormula(blockId: String, colId: String, expression: String) = viewModel.updateDbFormula(blockId, colId, expression)
            override fun onDeleteDbColumn(blockId: String, colId: String) = viewModel.deleteDbColumn(blockId, colId)
            override fun onDeleteDbRow(blockId: String, rowId: String) = viewModel.deleteDbRow(blockId, rowId)
            override fun onAddDbRowAt(blockId: String, index: Int) = viewModel.addDbRowAt(blockId, index)
            override fun onAddDbColumnAt(blockId: String, index: Int) = viewModel.addDbColumnAt(blockId, index)
            override fun onUpdateDbColumnWidth(blockId: String, colId: String, width: Int) = viewModel.updateDbColumnWidth(blockId, colId, width)
            override fun onVoiceRecorded(id: String, filePath: String, duration: Int) = viewModel.handleVoiceRecorded(id, filePath, duration)
            override fun onRemoveVoice(id: String) = viewModel.handleRemoveVoice(id)
            override fun onDeleteImageBlock(id: String) = viewModel.deleteImageBlock(id)
            override fun onCreateGlobalTag(name: String, colorHex: String): String = viewModel.createGlobalTag(name, colorHex)
            override fun onRequestImagePicker(blockId: String) {
                onPickImage { path -> viewModel.handleImagePicked(blockId, path) }
            }
            override fun onRequestCamera(blockId: String) {
                onTakePhoto { path -> viewModel.handleImagePicked(blockId, path) }
            }
            override fun onRequestDocumentPicker(blockId: String) {
                onPickDocument { path -> viewModel.handleDocumentPicked(blockId, path) }
            }
            override fun onRequestDbFilePicker(blockId: String, rowId: String, colId: String, isAudio: Boolean) {
                onPickDocument { path ->
                    viewModel.handleDbFilePicked(blockId, rowId, colId, path)
                }
            }
            override fun onStopDbAudioRecording(blockId: String, rowId: String, colId: String, cancel: Boolean) {
                viewModel.stopDbHardwareRecording(blockId, rowId, colId, cancel)
            }
            override fun onOpenFile(filePath: String, mimeType: String) {
                onOpenFile(filePath, mimeType)
            }
            override fun onStartRecording() = viewModel.startHardwareRecording()
            override fun onStopRecording(blockId: String, cancel: Boolean) = viewModel.stopHardwareRecording(blockId, cancel)
            override fun onPlayAudio(filePath: String, onComplete: () -> Unit) = viewModel.playAudio(filePath, onComplete)
            override fun onStopAudio() = viewModel.stopAudio()
            override fun onUndo() = viewModel.undo()
            override fun onRedo() = viewModel.redo()
            override fun onTogglePin() = viewModel.togglePinSelectedBlocks()
            override fun onUpdateSketch(id: String, strokes: List<Stroke>) =
                viewModel.updateSketchStrokes(id, strokes)
            override fun onMoveBlock(sourceId: String, targetId: String, zone: DropTargetZone) =
                viewModel.moveBlock(sourceId, targetId, zone)
            override fun onUpdateColumnWeights(rowId: String, weights: List<Float>) =
                viewModel.updateColumnWeights(rowId, weights)
            override fun onAddBlockAbove(id: String) = viewModel.addBlockAbove(id)
            override fun onAddBlockBelow(id: String) = viewModel.addBlockBelow(id)
            override fun onUpdateDbAggregation(blockId: String, colId: String, aggregationType: String?) =
                viewModel.updateDbAggregation(blockId, colId, aggregationType)
            override fun onUpdateDbCurrency(blockId: String, colId: String, symbol: String) =
                viewModel.updateDbCurrency(blockId, colId, symbol)
            override fun onUpdateDbFormulaCurrency(blockId: String, colId: String, enabled: Boolean) =
                viewModel.updateDbFormulaCurrency(blockId, colId, enabled)
            override fun onNoteLinkClick(noteId: String) {
                if (isDesktopPlatform) {
                    subNotePanelId = noteId
                } else {
                    onNavigateToEditor(noteId)
                }
            }
            override fun onCreateLinkedNote(title: String): String {
                return viewModel.createLinkedNote(title)
            }
            override fun onOpenDatabaseNote(blockId: String, rowId: String, colId: String, existingNoteId: String?) {
                viewModel.openDatabaseNote(blockId, rowId, colId, existingNoteId) { resolvedNoteId ->
                    if (isDesktopPlatform) {
                        subNotePanelId = resolvedNoteId
                    } else {
                        onNavigateToEditor(resolvedNoteId)
                    }
                }
            }
            override suspend fun getNoteTitle(noteId: String): String {
                return viewModel.getNoteTitle(noteId)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditorScreen(
            blocks = blocks,
            allLinkableNotes = allLinkableNotes,
            globalTags = globalTags,
            actions = actions,
            focusRequest = focusRequest,
            selectedBlockIds = selectedBlockIds,
            hazeState = hazeState,
            mobileMenuState = mobileMenuState,
            onMobileMenuStateChange = { mobileMenuState = it },
            slashQuery = slashQuery,
            onSlashQueryChange = { slashQuery = it },
            bottomContentPadding = bottomContentPadding,
            topContentPadding = if (isDesktopPlatform) {
                if (!isSidebarVisible) 72.dp else 16.dp
            } else {
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 150.dp
            },
            modifier = Modifier.fillMaxSize().haze(state = hazeState)
        )

        AnimatedVisibility(
            visible = showToolbar,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 250, delayMillis = 100, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(durationMillis = 250, delayMillis = 100)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(durationMillis = 200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                .padding(bottom = 8.dp, start = if (isDesktopPlatform) 16.dp else 6.dp, end = if (isDesktopPlatform) 16.dp else 6.dp)
        ) {
            EditorToolbar(
                mobileMenuState = mobileMenuState,
                onMenuStateChange = { mobileMenuState = it },
                query = slashQuery,
                hazeState = hazeState,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { actions.onUndo() },
                onRedo = { actions.onRedo() },
                onChangeBlockType = { actions.onChangeBlockType(it) },
                onToggleFormat = { actions.onToggleFormat(it) },
                onAdjustIndentation = { actions.onAdjustIndentation(it) },
                onInsertMediaBlock = { actions.onInsertMediaBlock(it) },
                onSelectCurrentBlock = {
                    GlobalEditorState.currentlyFocusedBlockId?.let { id ->
                        actions.onToggleSelection(id)
                    }
                }
            )
        }

        BlockSelectionPill(
            isVisible = isSelectionMode,
            selectedCount = selectedBlockIds.size,
            onClearSelection = { viewModel.clearSelection() },
            onSelectAll = { viewModel.selectAllBlocks() },
            onCopy = {
                clipboardManager.setText(AnnotatedString(viewModel.getSelectedText()))
                viewModel.clearSelection()
            },
            onCut = {
                clipboardManager.setText(AnnotatedString(viewModel.cutSelectedBlocks()))
            },
            onAddBlockAbove = { viewModel.addBlockAboveSelection() },
            onAddBlockBelow = { viewModel.addBlockBelowSelection() },
            onDelete = { viewModel.deleteSelectedBlocks() },
            onTogglePin = { actions.onTogglePin() },
            isSelectionPinned = isSelectionPinned,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
        )

        if (subNotePanelId != null) {
            SubNotePanel(
                noteId = subNotePanelId!!,
                onClose = { subNotePanelId = null },
                onExpand = { noteId ->
                    subNotePanelId = null
                    onNavigateToEditor(noteId)
                },
                onPickImage = onPickImage,
                onPickDocument = onPickDocument,
                onOpenFile = onOpenFile,
                onExportMarkdown = onExportMarkdown,
                onExportPdf = onExportPdf
            )
        }
    }
}