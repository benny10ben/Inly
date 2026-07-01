package com.ben.inly.presentation.mobile.home.note

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.SelectionModeObserver
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.editor.EditorToolbar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import coil3.compose.AsyncImage
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.editor.components.DropTargetZone
import com.ben.inly.presentation.shared.editor.GlobalEditorState
import com.ben.inly.presentation.shared.editor.MobileMenuState
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalDensity
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.repository.EmojiRepository
import com.ben.inly.domain.util.showFeedback
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import com.ben.inly.presentation.shared.components.TopBarIconButton
import dev.chrisbanes.haze.hazeChild
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.painter.Painter
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import inly.app.generated.resources.Res
import inly.app.generated.resources.chevron_left
import inly.app.generated.resources.code
import inly.app.generated.resources.copy
import inly.app.generated.resources.ellipsis
import inly.app.generated.resources.file_chart_pie
import inly.app.generated.resources.file_code_corner
import inly.app.generated.resources.image
import inly.app.generated.resources.laugh
import inly.app.generated.resources.share
import inly.app.generated.resources.smile_plus
import inly.app.generated.resources.trash_2
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

enum class MenuLevel { MAIN, EXPORT, ICON, COVER }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onNavigateToEditor: (String) -> Unit = {},
    showBackButton: Boolean = true,
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> },
    viewModel: NoteEditorViewModel = koinViewModel(key = noteId)
) {

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val hazeState = remember { HazeState() }
    val clipboardManager = LocalClipboardManager.current
    val blocks by viewModel.visibleBlocks.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val noteIcon by viewModel.noteIcon.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val coverImagePath by viewModel.coverImagePath.collectAsState()

    // Word Count States
    val showWordCount by viewModel.showWordCount.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()

    var mobileMenuState by remember { mutableStateOf(MobileMenuState.MAIN) }
    var slashQuery by remember { mutableStateOf("") }

    val allLinkableNotes by viewModel.allLinkableNotes.collectAsState()

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, noteId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadNote(noteId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showIconPicker by remember { mutableStateOf(false) }

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val selectedBlocksList = blocks.filter { it.id in selectedBlockIds }
    val isSelectionPinned = selectedBlocksList.isNotEmpty() && selectedBlocksList.all { it.isPinned }

    var showOptionsMenu by remember { mutableStateOf(false) }
    var subNotePanelId by remember { mutableStateOf<String?>(null) }
    val globalTags by viewModel.globalTags.collectAsState()

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var isKeyboardOpen by remember { mutableStateOf(false) }
    var previousImeBottom by remember { mutableIntStateOf(0) }

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen && mobileMenuState != MobileMenuState.MAIN) {
            mobileMenuState = MobileMenuState.MAIN
        }
    }

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

    val showToolbar = !isSelectionMode && (isKeyboardOpen || isDesktopPlatform || mobileMenuState != MobileMenuState.MAIN)

    SelectionModeObserver(isSelectionMode, onSelectionModeChange)

    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    KmpBackHandler(enabled = mobileMenuState != MobileMenuState.MAIN) {
        mobileMenuState = MobileMenuState.MAIN
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80 }
    }

    val scope = rememberCoroutineScope()

    val handleToggleFavorite: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250.milliseconds); viewModel.toggleFavorite() }
    }
    val handleToggleWordCount: () -> Unit = {
        showOptionsMenu = false
        viewModel.toggleWordCount()
    }
    val handleAddIcon: () -> Unit = {
        showOptionsMenu = false
        showIconPicker = true
    }
    val handleRemoveIcon: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250.milliseconds); viewModel.updateIcon(null) }
    }
    val handleAddCover: () -> Unit = {
        showOptionsMenu = false
        scope.launch {
            delay((if (!isDesktopPlatform) 250L else 100L).milliseconds)
            withContext(Dispatchers.IO) {
                onPickImage { path -> viewModel.handleCoverImagePicked(path) }
            }
        }
    }
    val handleRemoveCover: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250.milliseconds); viewModel.removeCoverImage() }
    }
    val handleMoveToTrash: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250.milliseconds); viewModel.moveToTrash { onNavigateBack() } }
    }

    val handleCopyPlain: () -> Unit = {
        showOptionsMenu = false
        val text = viewModel.generatePlainTextExport()
        clipboardManager.setText(AnnotatedString(text))
        showFeedback("Copied to clipboard")
    }

    val handleCopyMarkdown: () -> Unit = {
        showOptionsMenu = false
        val md = viewModel.generateMarkdownExport()
        clipboardManager.setText(AnnotatedString(md))
        showFeedback("Copied as Markdown")
    }

    val handleDownloadMarkdown: () -> Unit = {
        showOptionsMenu = false
        val safeTitle = noteTitle.ifBlank { "Untitled_Note" }.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val content = com.ben.inly.domain.util.ExportEngine.generateMarkdown(blocks, noteTitle)
        onExportMarkdown("$safeTitle.md", content)
    }

    val handleDownloadPdf: () -> Unit = {
        showOptionsMenu = false
        scope.launch {
            delay(300.milliseconds)
            val safeTitle = noteTitle.ifBlank { "Untitled_Note" }.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            onExportPdf("$safeTitle.pdf", noteTitle, blocks)
        }
    }

    var isListScrollEnabled by remember { mutableStateOf(true) }

    val editorActions = remember(viewModel, onOpenFile) {
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
            override fun setScrollEnabled(enabled: Boolean) {
                isListScrollEnabled = enabled
            }
            override fun onUpdateSketch(id: String, strokes: List<com.ben.inly.domain.model.Stroke>) =
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = paddingValues.calculateBottomPadding()))
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                EditorScreen(
                    blocks = blocks,
                    allLinkableNotes = allLinkableNotes,
                    actions = editorActions,
                    listState = listState,
                    focusRequest = focusRequest,
                    selectedBlockIds = selectedBlockIds,
                    hazeState = hazeState,
                    mobileMenuState = mobileMenuState,
                    onMobileMenuStateChange = { mobileMenuState = it },
                    slashQuery = slashQuery,
                    onSlashQueryChange = { slashQuery = it },
                    headerContent = {
                        NoteHeader(
                            noteIcon = noteIcon,
                            noteTitle = noteTitle,
                            coverImagePath = coverImagePath,
                            showIconPicker = showIconPicker,
                            onDismissIconPicker = { showIconPicker = false },
                            onIconChange = { viewModel.updateIcon(it) },
                            onTitleChange = { viewModel.updateTitle(it) },
                            onIconClick = { showIconPicker = true }
                        )
                    },
                    globalTags = globalTags,
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
                        onUndo = { editorActions.onUndo() },
                        onRedo = { editorActions.onRedo() },
                        onChangeBlockType = { editorActions.onChangeBlockType(it) },
                        onToggleFormat = { editorActions.onToggleFormat(it) },
                        onAdjustIndentation = { editorActions.onAdjustIndentation(it) },
                        onInsertMediaBlock = { editorActions.onInsertMediaBlock(it) },
                        onSelectCurrentBlock = {
                            GlobalEditorState.currentlyFocusedBlockId?.let { id ->
                                editorActions.onToggleSelection(id)
                            }
                        }
                    )
                }

                // Word Count Overlay
                AnimatedVisibility(
                    visible = showWordCount && !isSelectionMode,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding()
                        .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                        .padding(
                            bottom = if (isDesktopPlatform) {14.dp} else {if (showToolbar) 68.dp else 14.dp},
                            end = 16.dp
                        )
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .hazeChild(state = hazeState)
                    ) {
                        Text(
                            text = "$wordCount words",
                            fontFamily = PoppinsFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                NoteTopBar(
                    showOptionsMenu = showOptionsMenu,
                    showBackButton = showBackButton,
                    onDismissOptionsMenu = { showOptionsMenu = false },
                    hazeState = hazeState,
                    onBackClick = {
                        if (isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            onNavigateBack()
                        }
                    },
                    onOptionsClick = { showOptionsMenu = true },
                    desktopMenuContent = {
                        NoteOptionsDesktopMenu(
                            isFavorite = isFavorite,
                            hasIcon = noteIcon != null,
                            hasCover = coverImagePath != null,
                            showWordCount = showWordCount,
                            onDismiss = { showOptionsMenu = false },
                            onToggleFavorite = handleToggleFavorite,
                            onAddIcon = handleAddIcon,
                            onRemoveIcon = handleRemoveIcon,
                            onAddCover = handleAddCover,
                            onRemoveCover = handleRemoveCover,
                            onToggleWordCount = handleToggleWordCount,
                            onMoveToTrash = handleMoveToTrash,
                            onCopyPlain = handleCopyPlain,
                            onCopyMarkdown = handleCopyMarkdown,
                            onDownloadMarkdown = handleDownloadMarkdown,
                            onDownloadPdf = handleDownloadPdf
                        )
                    }
                )

                BlockSelectionPill(
                    isVisible = isSelectionMode,
                    selectedCount = selectedBlockIds.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllBlocks() },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(viewModel.getSelectedText()))
                        viewModel.clearSelection()
                    },
                    onCut = { clipboardManager.setText(AnnotatedString(viewModel.cutSelectedBlocks())) },
                    onAddBlockAbove = { viewModel.addBlockAboveSelection() },
                    onAddBlockBelow = { viewModel.addBlockBelowSelection() },
                    onDelete = { viewModel.deleteSelectedBlocks() },
                    onTogglePin = { viewModel.togglePinSelectedBlocks() },
                    isSelectionPinned = isSelectionPinned,
                    hazeState = hazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
                )

                if (!isDesktopPlatform) {
                    NoteOptionsBottomSheet(
                        expanded = showOptionsMenu,
                        isFavorite = isFavorite,
                        hasIcon = noteIcon != null,
                        hasCover = coverImagePath != null,
                        showWordCount = showWordCount,
                        onDismiss = { showOptionsMenu = false },
                        onToggleFavorite = handleToggleFavorite,
                        onAddIcon = handleAddIcon,
                        onRemoveIcon = handleRemoveIcon,
                        onAddCover = handleAddCover,
                        onRemoveCover = handleRemoveCover,
                        onToggleWordCount = handleToggleWordCount,
                        onMoveToTrash = handleMoveToTrash,
                        onCopyPlain = handleCopyPlain,
                        onCopyMarkdown = handleCopyMarkdown,
                        onDownloadMarkdown = handleDownloadMarkdown,
                        onDownloadPdf = handleDownloadPdf
                    )
                }

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

                if (!isDesktopPlatform) {
                    InlyBottomSheet(
                        expanded = showIconPicker,
                        onDismiss = { showIconPicker = false },
                        title = "Choose Icon",
                        applyNavPadding = false
                    ) { closeAnd ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                        ) {
                            CategorizedEmojiPicker(
                                onEmojiSelected = { emoji ->
                                    viewModel.updateIcon(emoji)
                                    showIconPicker = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Note header — cover image, icon, title
@Composable
private fun NoteHeader(
    noteIcon: String?,
    noteTitle: String,
    coverImagePath: String?,
    showIconPicker: Boolean,
    onDismissIconPicker: () -> Unit,
    onIconChange: (String?) -> Unit,
    onTitleChange: (String) -> Unit,
    onIconClick: () -> Unit
) {
    val mediaStorageHelper: com.ben.inly.domain.util.MediaStorageHelper = org.koin.compose.koinInject()

    val topPadding by animateDpAsState(
        targetValue = if (noteIcon != null) 48.dp else 16.dp,
        label = "TopPadding"
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.fillMaxWidth()) {

            if (coverImagePath != null || noteIcon != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (coverImagePath != null) {
                        val absolutePath = mediaStorageHelper.getAbsoluteMediaPath(coverImagePath)
                        val file = java.io.File(absolutePath)

                        val context = coil3.compose.LocalPlatformContext.current
                        val request = remember(absolutePath) {
                            coil3.request.ImageRequest.Builder(context)
                                .data(file)
                                .memoryCacheKey(absolutePath)
                                .diskCacheKey(absolutePath)
                                .build()
                        }

                        AsyncImage(
                            model = request,
                            contentDescription = "Cover Image",
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(100.dp)
                        )
                    }

                    if (noteIcon != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp)
                                .graphicsLayer {
                                    translationY = 36.dp.toPx()
                                }
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onIconClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = noteIcon,
                                style = TextStyle(
                                    fontSize = 58.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 58.sp
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                )
            }

            if (isDesktopPlatform) {
                InlyDesktopMenu(
                    expanded = showIconPicker,
                    onDismissRequest = onDismissIconPicker
                ) {
                    Box(modifier = Modifier.size(width = 340.dp, height = 380.dp)) {
                        CategorizedEmojiPicker(
                            onEmojiSelected = { emoji ->
                                onIconChange(emoji)
                                onDismissIconPicker()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                if (noteTitle.isEmpty()) {
                    Text(
                        text = "Untitled",
                        fontFamily = PoppinsFont,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                }
                BasicTextField(
                    value = noteTitle,
                    onValueChange = { onTitleChange(it) },
                    textStyle = TextStyle(
                        fontFamily = PoppinsFont,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// CategorizedEmojiPicker
@Composable
fun CategorizedEmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!EmojiRepository.isLoaded) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        return
    }

    val categoryNames = EmojiRepository.categories
    val flatEmojiList = EmojiRepository.flatList
    val categoryEmojiLists = EmojiRepository.categoryEmojiLists

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(200)
        val q = searchQuery.lowercase()
        searchResults = withContext(Dispatchers.Default) {
            flatEmojiList.mapNotNullTo(ArrayList(32)) { (emoji, keywords) ->
                emoji.takeIf { keywords.contains(q) }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { categoryNames.size })
    val coroutineScope = rememberCoroutineScope()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val onTabClick: (Int) -> Unit = remember(pagerState, coroutineScope) {
        { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search emojis...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontFamily = PoppinsFont
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (searchQuery.isNotEmpty()) {
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No emojis found",
                        fontFamily = PoppinsFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 8.dp,
                        bottom = navBarPadding + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it },
                        contentType = { "emoji_item" }
                    ) { emoji ->
                        EmojiGridItem(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                    }
                }
            }
        } else {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                categoryNames.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = { onTabClick(index) },
                        text = {
                            Text(
                                text = title,
                                fontFamily = PoppinsFont,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val activeEmojis = categoryEmojiLists[page]

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 12.dp,
                        bottom = navBarPadding + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = activeEmojis,
                        key = { it },
                        contentType = { "emoji_item" }
                    ) { emoji ->
                        EmojiGridItem(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiGridItem(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Top bar (back + options)
@Composable
private fun NoteTopBar(
    onBackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    showBackButton: Boolean = true,
    showOptionsMenu: Boolean = false,
    hazeState: HazeState? = null,
    onDismissOptionsMenu: () -> Unit = {},
    desktopMenuContent: @Composable () -> Unit = {}
) {
    val defaultBgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.statusBarsPadding())
            .padding(top = if (isDesktopPlatform) 14.dp else 18.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            TopBarIconButton(
                icon = painterResource(Res.drawable.chevron_left),
                contentDescription = "Back",
                bgColor = defaultBgColor,
                tint = defaultContentColor,
                hazeState = hazeState,
                onClick = onBackClick
            )
        } else {
            Spacer(Modifier.size(1.dp))
        }

        Box {
            TopBarIconButton(
                icon = painterResource(Res.drawable.ellipsis),
                contentDescription = "Options",
                bgColor = defaultBgColor,
                tint = defaultContentColor,
                hazeState = hazeState,
                onClick = onOptionsClick
            )

            if (isDesktopPlatform) {
                InlyDesktopMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = onDismissOptionsMenu
                ) {
                    desktopMenuContent()
                }
            }
        }
    }
}

// Options menus (BottomSheet & Desktop Popup)
@Composable
fun NoteOptionsDesktopMenu(
    isFavorite: Boolean,
    hasIcon: Boolean,
    hasCover: Boolean,
    showWordCount: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddIcon: () -> Unit,
    onRemoveIcon: () -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onToggleWordCount: () -> Unit,
    onMoveToTrash: () -> Unit,
    onCopyPlain: () -> Unit = {},
    onCopyMarkdown: () -> Unit = {},
    onDownloadMarkdown: () -> Unit = {},
    onDownloadPdf: () -> Unit = {}
) {
    var currentMenu by remember { mutableStateOf(MenuLevel.MAIN) }

    Column(modifier = Modifier.width(240.dp).padding(vertical = 4.dp)) {
        AnimatedContent(
            targetState = currentMenu,
            transitionSpec = {
                if (targetState != MenuLevel.MAIN && initialState == MenuLevel.MAIN) {
                    (slideInHorizontally(tween(200)) { it } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(200)) { -it / 2 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200)))
                }
            },
            label = "DesktopMenuTransition"
        ) { targetMenu ->
            Column(modifier = Modifier.fillMaxWidth()) {
                when (targetMenu) {
                    MenuLevel.MAIN -> {
                        DesktopMenuItem(painterResource(Res.drawable.smile_plus), "Icon Options") { currentMenu = MenuLevel.ICON }
                        DesktopMenuItem(painterResource(Res.drawable.image), "Cover Options") { currentMenu = MenuLevel.COVER }

                        val favIcon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder
                        val favText = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                        DesktopMenuItem(favIcon, favText) { onDismiss(); onToggleFavorite() }

                        val wordCountText = if (showWordCount) "Hide Word Count" else "Show Word Count"
                        DesktopMenuItem(Icons.Default.FormatSize, wordCountText) { onDismiss(); onToggleWordCount() }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        DesktopMenuItem(painterResource(Res.drawable.share), "Export Note") { currentMenu = MenuLevel.EXPORT }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        DesktopMenuItem(painterResource(Res.drawable.trash_2), "Move to Trash", isDestructive = true) {
                            onDismiss(); onMoveToTrash()
                        }
                    }

                    MenuLevel.EXPORT -> {
                        DesktopMenuItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        DesktopMenuItem(painterResource(Res.drawable.copy), "Copy Text") { onDismiss(); onCopyPlain() }
                        DesktopMenuItem(painterResource(Res.drawable.code), "Copy as Markdown") { onDismiss(); onCopyMarkdown() }
                        DesktopMenuItem(painterResource(Res.drawable.file_code_corner), "Download .md") { onDismiss(); onDownloadMarkdown() }
                        DesktopMenuItem(painterResource(Res.drawable.file_chart_pie), "Download PDF") { onDismiss(); onDownloadPdf() }
                    }

                    MenuLevel.ICON -> {
                        DesktopMenuItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        DesktopMenuItem(painterResource(Res.drawable.smile_plus), if (hasIcon) "Change Icon" else "Add Icon") {
                            onDismiss()
                            onAddIcon()
                        }
                        if (hasIcon) {
                            DesktopMenuItem(painterResource(Res.drawable.trash_2), "Remove Icon", isDestructive = true) {
                                onDismiss()
                                onRemoveIcon()
                            }
                        }
                    }

                    MenuLevel.COVER -> {
                        DesktopMenuItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        DesktopMenuItem(painterResource(Res.drawable.image), if (hasCover) "Change Cover" else "Add Cover") { onDismiss(); onAddCover() }
                        if (hasCover) {
                            DesktopMenuItem(painterResource(Res.drawable.trash_2), "Remove Cover", isDestructive = true) { onDismiss(); onRemoveCover() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMenuItem(
    icon: Painter,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
private fun DesktopMenuItem(
    icon: ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun NoteOptionsBottomSheet(
    expanded: Boolean,
    isFavorite: Boolean,
    hasIcon: Boolean,
    hasCover: Boolean,
    showWordCount: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddIcon: () -> Unit,
    onRemoveIcon: () -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onToggleWordCount: () -> Unit,
    onMoveToTrash: () -> Unit,
    onCopyPlain: () -> Unit = {},
    onCopyMarkdown: () -> Unit = {},
    onDownloadMarkdown: () -> Unit = {},
    onDownloadPdf: () -> Unit = {}
) {
    var currentMenu by remember { mutableStateOf(MenuLevel.MAIN) }

    // Reset to the main menu after the bottom sheet closes
    LaunchedEffect(expanded) {
        if (!expanded) {
            delay(300.milliseconds)
            currentMenu = MenuLevel.MAIN
        }
    }

    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = null
    ) { closeAnd ->
        AnimatedContent(
            targetState = currentMenu,
            transitionSpec = {
                // Native Material "Fade Through" transition for Bottom Sheets
                (fadeIn(animationSpec = tween(220, delayMillis = 90))) togetherWith
                        fadeOut(animationSpec = tween(90)) using
                        SizeTransform(clip = false)
            },
            label = "MenuTransition"
        ) { targetMenu ->

            Column(modifier = Modifier.fillMaxWidth()) {
                when (targetMenu) {
                    MenuLevel.MAIN -> {
                        BottomSheetOptionItem(painterResource(Res.drawable.smile_plus), "Icon Options") { currentMenu = MenuLevel.ICON }
                        BottomSheetOptionItem(painterResource(Res.drawable.image), "Cover Options") { currentMenu = MenuLevel.COVER }

                        val favIcon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder
                        val favText = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                        BottomSheetOptionItem(favIcon, favText) { closeAnd { onToggleFavorite() } }

                        val wordCountText = if (showWordCount) "Hide Word Count" else "Show Word Count"
                        BottomSheetOptionItem(Icons.Default.FormatSize, wordCountText) { closeAnd { onToggleWordCount() } }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        BottomSheetOptionItem(painterResource(Res.drawable.share), "Export Note") { currentMenu = MenuLevel.EXPORT }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        BottomSheetOptionItem(painterResource(Res.drawable.trash_2), "Move to Trash", isDestructive = true) {
                            closeAnd { onMoveToTrash() }
                        }

                        InlyButtonPrimary(
                            text = "Close",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }

                    MenuLevel.EXPORT -> {
                        BottomSheetOptionItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        BottomSheetOptionItem(painterResource(Res.drawable.copy), "Copy Text") { closeAnd { onCopyPlain() } }
                        BottomSheetOptionItem(painterResource(Res.drawable.code), "Copy as Markdown") { closeAnd { onCopyMarkdown() } }
                        BottomSheetOptionItem(painterResource(Res.drawable.file_code_corner), "Download .md") { closeAnd { onDownloadMarkdown() } }
                        BottomSheetOptionItem(painterResource(Res.drawable.file_chart_pie), "Download PDF") { closeAnd { onDownloadPdf() } }
                    }

                    MenuLevel.ICON -> {
                        BottomSheetOptionItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        BottomSheetOptionItem(painterResource(Res.drawable.smile_plus), if (hasIcon) "Change Icon" else "Add Icon") {
                            onAddIcon()
                            onDismiss()
                        }
                        if (hasIcon) {
                            BottomSheetOptionItem(painterResource(Res.drawable.trash_2), "Remove Icon", isDestructive = true) {
                                onRemoveIcon()
                                onDismiss()
                            }
                        }
                    }

                    MenuLevel.COVER -> {
                        BottomSheetOptionItem(painterResource(Res.drawable.chevron_left), "Back to Options") { currentMenu = MenuLevel.MAIN }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        BottomSheetOptionItem(painterResource(Res.drawable.image), if (hasCover) "Change Cover" else "Add Cover") { closeAnd { onAddCover() } }
                        if (hasCover) {
                            BottomSheetOptionItem(painterResource(Res.drawable.trash_2), "Remove Cover", isDestructive = true) { closeAnd { onRemoveCover() } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetOptionItem(
    icon: ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
private fun BottomSheetOptionItem(
    icon: Painter,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = textColor
        )
    }
}