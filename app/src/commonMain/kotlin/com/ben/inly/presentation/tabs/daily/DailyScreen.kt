package com.ben.inly.presentation.tabs.daily

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.Stroke
import com.ben.inly.domain.sync.SyncPairingData
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.tabs.home.note.SubNotePanel
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.components.DropTargetZone
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.SelectionModeObserver
import com.ben.inly.presentation.shared.editor.MobileMenuState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.daysUntil
import com.ben.inly.presentation.shared.editor.EditorToolbar
import com.ben.inly.presentation.shared.editor.GlobalEditorState
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import com.ben.inly.data.local.room.CalendarTaskEntity
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.UserSettings
import com.ben.inly.presentation.sync.SyncPairingDialog
import com.ben.inly.presentation.sync.SyncScannerDialog
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.presentation.sync.generateSecureToken
import com.ben.inly.presentation.sync.getLocalNetworkIp
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

private val HORIZONTAL_PADDING = 16.dp
private val PANEL_PADDING = 16.dp
private val DefaultCornerShape = RoundedCornerShape(12.dp)
private val DesktopPanelShape = RoundedCornerShape(12.dp)

private fun Modifier.customInlyShadow(shape: Shape): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.25f),
    ambientColor = Color.Black.copy(alpha = 0.10f)
)

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

@Composable
fun DailyScreen(
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onClearSearch: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    desktopBottomBar: (@Composable () -> Unit)? = null,
    isSidebarVisible: Boolean = true,
    sidebarWidth: Dp = 340.dp,
    onToggleSidebar: () -> Unit = {},
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> },
    showAddNoteDialog: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    viewModel: DailyEditorViewModel = koinViewModel(),
    settingsManager: SettingsManager = koinInject(),
    syncViewModel: SyncViewModel = koinViewModel()
) {
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    val hazeState = remember { HazeState() }
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val allLinkableNotes by viewModel.allLinkableNotes.collectAsState()

    val searchResults by viewModel.dailySearchResults.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val blocks by viewModel.visibleBlocks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val loadedDateString by viewModel.loadedDateString.collectAsState()
    val previewCache by viewModel.previewCache.collectAsState()

    val initialDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val initialPage = remember { Int.MAX_VALUE / 2 }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val selectedBlocksList = blocks.filter { it.id in selectedBlockIds }
    val isSelectionPinned = selectedBlocksList.isNotEmpty() && selectedBlocksList.all { it.isPinned }

    var showScheduledTasksSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }

    // User Settings & Sync State
    var showSettingsMenu by remember { mutableStateOf(false) }
    var isSettingsOpenDesktop by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var activePairingData by remember { mutableStateOf<SyncPairingData?>(null) }
    var showMobileScannerDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val syncState by syncViewModel.syncStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(syncState) {
        if (syncState != "Idle" && syncState != "Syncing...") {
            snackbarHostState.showSnackbar(message = syncState)
            syncViewModel.resetSyncStatus()
        }
    }

    val showToolbar = !isSelectionMode && !isSearchActive && !showAddNoteDialog && (isKeyboardOpen || isDesktopPlatform)

    val globalTags by viewModel.globalTags.collectAsState()
    val calendarTaskMap by viewModel.calendarTaskMap.collectAsState()

    var subNotePanelId by remember { mutableStateOf<String?>(null) }

    SelectionModeObserver(isSelectionMode, onSelectionModeChange)

    KmpBackHandler(enabled = showSettingsMenu) {
        showSettingsMenu = false
    }
    KmpBackHandler(enabled = showCalendarSheet) {
        showCalendarSheet = false
    }
    KmpBackHandler(enabled = isSearchActive) {
        onClearSearch()
    }
    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                viewModel.selectDate(initialDate.plus((page - initialPage).toLong(), DateTimeUnit.DAY))
            }
    }

    LaunchedEffect(selectedDate) {
        val targetPage = initialPage + initialDate.daysUntil(selectedDate)
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            if (abs(pagerState.currentPage - targetPage) > 3) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
        val keepDates = (-2..2).map { offset ->
            selectedDate.plus(offset.toLong(), DateTimeUnit.DAY).toString()
        }.toSet()
        viewModel.evictPreviewCache(keepDates)
    }

    var isListScrollEnabled by remember { mutableStateOf(true) }

    val sharedEditorActions = remember(viewModel, onOpenFile) {
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

    val settingsMenuSlot = @Composable {
        UserSettings(
            expanded = showSettingsMenu,
            onDismiss = { showSettingsMenu = false },
            onNavigateToSettings = {
                showSettingsMenu = false
                if (isDesktopPlatform) {
                    isSettingsOpenDesktop = true
                } else {
                    onNavigateToSettings()
                }
            },
            onNavigateToTrash = {
                showSettingsMenu = false
                onNavigateToTrash()
            },
            onShowPairingCode = {
                showSettingsMenu = false
                val currentIp = getLocalNetworkIp()
                val newToken = generateSecureToken()
                val newEncryptionKey = generateSecureToken() + generateSecureToken()
                settingsManager.saveSyncAuthToken(newToken)
                settingsManager.saveSyncEncryptionKey(newEncryptionKey)
                activePairingData = SyncPairingData(
                    ipAddress = currentIp,
                    port = 8080,
                    authToken = newToken,
                    encryptionKey = newEncryptionKey
                )
                showPairingDialog = true
            },
            onScanPairingCode = {
                showSettingsMenu = false
                showMobileScannerDialog = true
            },
            onSyncNow = {
                showSettingsMenu = false
                syncViewModel.triggerManualSync()
            }
        )
    }

    val leftPanelContent = @Composable {
        Column(modifier = Modifier.fillMaxSize()) {
            StaticDateHeader(
                selectedDate = selectedDate,
                taskMap = calendarTaskMap,
                onDateSelected = { viewModel.selectDate(it) },
                onCalendarIconClick = { showCalendarSheet = true },
                onNotificationsClick = { showScheduledTasksSheet = true },
                onSettingsClick = { showSettingsMenu = true },
                onToggleSidebar = onToggleSidebar,
                settingsMenu = settingsMenuSlot,
                modifier = Modifier.fillMaxWidth().zIndex(2f)
            )

            Box(modifier = Modifier.weight(1f)) {
                this@Column.AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = HORIZONTAL_PADDING,
                            end = HORIZONTAL_PADDING,
                            top = 10.dp,
                            bottom = 10.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults, key = { it.noteId }) { meta ->
                            DailySearchResultCard(
                                note = meta,
                                onClick = {
                                    meta.dateString?.let { viewModel.selectDate(LocalDate.parse(it)) }
                                    onClearSearch()
                                    isSettingsOpenDesktop = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    val rightPanelContent = @Composable {
        var mobileMenuState by remember { mutableStateOf(MobileMenuState.MAIN) }
        var slashQuery by remember { mutableStateOf("") }

        LaunchedEffect(isKeyboardOpen) {
            if (!isKeyboardOpen && mobileMenuState != MobileMenuState.MAIN) {
                mobileMenuState = MobileMenuState.MAIN
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            if (isDesktopPlatform && !isSidebarVisible) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open Sidebar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .haze(state = hazeState),
                beyondViewportPageCount = 1
            ) { page ->
                val pageDate = initialDate.plus((page - initialPage).toLong(), DateTimeUnit.DAY)
                val pageDateString = pageDate.toString()

                LaunchedEffect(pageDateString) {
                    viewModel.prefetchDateIfNeeded(pageDateString)
                }

                val isCurrentActivePage =
                    pageDate == selectedDate && loadedDateString == pageDateString

                val displayBlocks: List<NoteBlock> = if (isCurrentActivePage) {
                    blocks
                } else {
                    previewCache[pageDateString] ?: emptyList()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    EditorScreen(
                        blocks = displayBlocks,
                        allLinkableNotes = allLinkableNotes,
                        globalTags = globalTags,
                        actions = sharedEditorActions,
                        focusRequest = if (isCurrentActivePage) focusRequest else null,
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
                        }
                    )
                }
            }

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
                    onUndo = { sharedEditorActions.onUndo() },
                    onRedo = { sharedEditorActions.onRedo() },
                    onChangeBlockType = { sharedEditorActions.onChangeBlockType(it) },
                    onToggleFormat = { sharedEditorActions.onToggleFormat(it) },
                    onAdjustIndentation = { sharedEditorActions.onAdjustIndentation(it) },
                    onInsertMediaBlock = { sharedEditorActions.onInsertMediaBlock(it) },
                    onSelectCurrentBlock = {
                        GlobalEditorState.currentlyFocusedBlockId?.let { id ->
                            sharedEditorActions.onToggleSelection(id)
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
                onTogglePin = { sharedEditorActions.onTogglePin() },
                isSelectionPinned = isSelectionPinned,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            if (isDesktopPlatform) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isSidebarVisible,
                        enter = expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = PANEL_PADDING,
                                    end = PANEL_PADDING
                                )
                                .width(sidebarWidth)
                                .fillMaxHeight()
                                .customInlyShadow(DesktopPanelShape)
                                .clip(DesktopPanelShape)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    leftPanelContent()
                                }
                                desktopBottomBar?.invoke()
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSidebarVisible,
                        enter = fadeIn(tween(280)),
                        exit = fadeOut(tween(280))
                    ) {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }

                    // Right panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = PANEL_PADDING, end = PANEL_PADDING, top = 5.dp)
                            .customInlyShadow(DesktopPanelShape)
                            .clip(DesktopPanelShape)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (isSettingsOpenDesktop) {
                            com.ben.inly.presentation.settings.SettingsScreen(
                                onNavigateBack = { isSettingsOpenDesktop = false }
                            )
                        } else {
                            rightPanelContent()

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
                }

            } else {
                Box(Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        rightPanelContent()

                        AnimatedVisibility(
                            visible = searchQuery.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp)
                        ) {
                            Surface(color = MaterialTheme.colorScheme.background) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = HORIZONTAL_PADDING,
                                        end = HORIZONTAL_PADDING,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(searchResults, key = { it.noteId }) { meta ->
                                        DailySearchResultCard(
                                            note = meta,
                                            onClick = {
                                                meta.dateString?.let {
                                                    viewModel.selectDate(LocalDate.parse(it))
                                                }
                                                onClearSearch()
                                                isSettingsOpenDesktop = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    StaticDateHeader(
                        selectedDate = selectedDate,
                        taskMap = calendarTaskMap,
                        onDateSelected = {
                            viewModel.selectDate(it)
                            isSettingsOpenDesktop = false
                         },
                        onCalendarIconClick = { showCalendarSheet = true },
                        onNotificationsClick = { showScheduledTasksSheet = true },
                        onSettingsClick = { showSettingsMenu = true },
                        onToggleSidebar = onToggleSidebar,
                        settingsMenu = settingsMenuSlot,
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(2f)
                    )
                }
            }

            // Sync Dialogs
            if (showPairingDialog && activePairingData != null) {
                SyncPairingDialog(
                    pairingData = activePairingData,
                    onDismiss = { showPairingDialog = false }
                )
            }

            if (showMobileScannerDialog) {
                SyncScannerDialog(
                    onDismiss = { showMobileScannerDialog = false },
                    onScanned = { pairingData ->
                        showMobileScannerDialog = false
                        settingsManager.saveSyncIpAddress(pairingData.ipAddress)
                        settingsManager.saveSyncPort(pairingData.port)
                        settingsManager.saveSyncAuthToken(pairingData.authToken)
                        settingsManager.saveSyncEncryptionKey(pairingData.encryptionKey)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Paired with ${pairingData.ipAddress}!")
                        }
                    }
                )
            }

            if (showScheduledTasksSheet) {
                val todayTasks = calendarTaskMap[initialDate] ?: emptyList()
                val tomorrowTasks = calendarTaskMap[initialDate.plus(1, DateTimeUnit.DAY)] ?: emptyList()

                InlyBottomSheet(
                    expanded = true,
                    onDismiss = { showScheduledTasksSheet = false },
                    title = "Upcoming Tasks",
                ) { closeAnd ->

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (todayTasks.isEmpty() && tomorrowTasks.isEmpty()) {
                            Text(
                                "No tasks scheduled for today or tomorrow.",
                                fontFamily = PoppinsFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            if (todayTasks.isNotEmpty()) {
                                TaskDaySection("Today", todayTasks, viewModel)
                            }

                            if (tomorrowTasks.isNotEmpty()) {
                                TaskDaySection("Tomorrow", tomorrowTasks, viewModel)
                            }
                        }
                    }
                }
            }

            if (showCalendarSheet) {
                InlyBottomSheet(
                    expanded = true,
                    onDismiss = { showCalendarSheet = false },
                    title = null,
                    subtitle = null
                ) { closeAnd ->
                    BottomSheetMonthCalendar(
                        selectedDate = selectedDate,
                        today = initialDate,
                        taskMap = calendarTaskMap,
                        onDateSelected = {
                            viewModel.selectDate(it)
                            closeAnd { showCalendarSheet = false }
                        },
                        onGoToToday = {
                            viewModel.selectDate(initialDate)
                            closeAnd { showCalendarSheet = false }
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 66.dp)
            ) { data ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .hazeChild(state = hazeState)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = data.visuals.message,
                            fontFamily = PoppinsFont,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySearchResultCard(note: NoteMetadataEntity, onClick: () -> Unit) {
    val formattedDate = remember(note.dateString) {
        if (note.dateString == null) return@remember "Unknown Date"
        try {
            val date = LocalDate.parse(note.dateString!!)
            val months = arrayOf(
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val dayOfWeekStr =
                date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            "$dayOfWeekStr, ${months[date.monthNumber]} ${date.dayOfMonth}, ${date.year}"
        } catch (e: Exception) {
            note.dateString ?: "Unknown Date"
        }
    }

    val bgColor = if (isDesktopPlatform) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = DefaultCornerShape,
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultCornerShape)
            .noRippleClickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = formattedDate,
                fontFamily = PoppinsFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.snippet.ifEmpty { "Empty note..." },
                fontFamily = PoppinsFont,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun StaticDateHeader(
    selectedDate: LocalDate,
    taskMap: Map<LocalDate, List<CalendarTaskEntity>>,
    onDateSelected: (LocalDate) -> Unit,
    onCalendarIconClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleSidebar: () -> Unit,
    settingsMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .then(if (isDesktopPlatform) Modifier else Modifier.statusBarsPadding())
            .padding(top = if (isDesktopPlatform) 16.dp else 10.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 4.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Hamburger & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDesktopPlatform) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val isToday = selectedDate == Clock.System.todayIn(TimeZone.currentSystemDefault())
                val titleText = if (isToday) "today" else {
                    val shortDay = selectedDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    "$shortDay ${selectedDate.dayOfMonth}"
                }

                Text(
                    text = titleText,
                    fontFamily = PoppinsFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .offset(x = if (isDesktopPlatform) (-6).dp else 0.dp)
                        .padding(top = 10.dp, bottom = 8.dp)
                        .noRippleClickable { onCalendarIconClick() }
                )
            }

            // Right Side: Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.offset(y = if (isDesktopPlatform) (-2).dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onNotificationsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                    settingsMenu()
                }
            }
        }

        CollapsedWeekStrip(
            selectedDate = selectedDate,
            today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            modifier = Modifier.padding(vertical = 4.dp),
            onDateSelected = onDateSelected
        )
    }
}

@Composable
private fun TaskDaySection(
    dayTitle: String,
    tasks: List<CalendarTaskEntity>,
    viewModel: DailyEditorViewModel
) {
    val groupedTasks = remember(tasks) {
        tasks.groupBy { task ->
            val timestamp = task.reminderTimestamp
            if (timestamp == null || timestamp == 0L) {
                "All Day"
            } else {
                val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                val hour = dt.hour
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                "$displayHour:00 $amPm"
            }
        }.toSortedMap(compareBy { label ->
            if (label == "All Day") -1 else {
                val isPm = label.contains("PM")
                var h = label.substringBefore(":").toInt()
                if (h == 12 && !isPm) h = 0
                if (isPm && h != 12) h += 12
                h
            }
        })
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        // Day Header
        Text(
            text = dayTitle,
            fontFamily = PoppinsFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sub-sections for each Hour
        groupedTasks.forEach { (hourLabel, hourTasks) ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {

                // Hour Title (e.g., 9:00 AM)
                Text(
                    text = hourLabel,
                    fontFamily = PoppinsFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                hourTasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isChecked,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleCalendarTask(task, isChecked)
                            },
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = task.text.ifBlank { "Empty task" },
                            fontFamily = PoppinsFont,
                            fontSize = 15.sp,
                            color = if (task.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                            textDecoration = if (task.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}