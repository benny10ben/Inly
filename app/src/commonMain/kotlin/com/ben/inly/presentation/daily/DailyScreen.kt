package com.ben.inly.presentation.daily

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.sync.SyncPairingData
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.SyncPairingDialog
import com.ben.inly.presentation.shared.SyncScannerDialog
import com.ben.inly.presentation.shared.UserSettings
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.SelectionModeObserver
import com.ben.inly.presentation.shared.sync.SyncViewModel
import com.ben.inly.presentation.shared.sync.generateSecureToken
import com.ben.inly.presentation.shared.sync.getLocalNetworkIp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.daysUntil
import com.ben.inly.presentation.shared.editor.EditorToolbar
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onClearSearch: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    onNavigateToTrash: () -> Unit = {},
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    desktopBottomBar: (@Composable () -> Unit)? = null,
    isSidebarVisible: Boolean = true,
    sidebarWidth: Dp = 340.dp,
    onToggleSidebar: () -> Unit = {},
    onSidebarWidthChange: (Dp) -> Unit = {},
    settingsManager: SettingsManager = koinInject(),
    syncViewModel: SyncViewModel = koinViewModel(),
    viewModel: DailyEditorViewModel = koinViewModel()
) {
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    val hazeState = remember { HazeState() }
    val density = LocalDensity.current

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

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

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    val showToolbar = !isSelectionMode && !isSearchActive && (isKeyboardOpen || isDesktopPlatform)

    val globalTags by viewModel.globalTags.collectAsState()

    var showNotesMenu by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var activePairingData by remember { mutableStateOf<SyncPairingData?>(null) }
    var showMobileScannerDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val syncState by syncViewModel.syncStatus.collectAsState()

    LaunchedEffect(syncState) {
        if (syncState != "Idle") {
            snackbarHostState.showSnackbar(message = syncState)
        }
    }

    SelectionModeObserver(isSelectionMode, onSelectionModeChange)

    KmpBackHandler(enabled = isSearchActive || isSelectionMode) {
        if (isSearchActive) {
            onClearSearch()
        } else if (isSelectionMode) {
            viewModel.clearSelection()
        }
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
            if (kotlin.math.abs(pagerState.currentPage - targetPage) > 3) {
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

    // Hoisted EditorActions to share between pager and toolbar
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
            override fun onRequestDocumentPicker(blockId: String) {
                onPickDocument { path -> viewModel.handleDocumentPicked(blockId, path) }
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
        }
    }

    // Date picker sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = Instant.parse("${selectedDate}T00:00:00Z")
                    .toEpochMilliseconds()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .padding(horizontal = HORIZONTAL_PADDING)
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent)
                )
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.selectDate(
                                Instant.fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.UTC).date
                            )
                        }
                        showBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = DefaultCornerShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Confirm Date",
                        fontFamily = PoppinsFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // LEFT PANEL (Desktop) — date header + search results
    val leftPanelContent = @Composable {
        Column(modifier = Modifier.fillMaxSize()) {
            StaticDateHeader(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                onCalendarIconClick = { showBottomSheet = true },
                onToggleSidebar = onToggleSidebar,
                showNotesMenu = showNotesMenu,
                onNotesMenuToggle = { showNotesMenu = it },
                onNavigateToTrash = onNavigateToTrash,
                settingsManager = settingsManager,
                syncViewModel = syncViewModel,
                onShowPairingCode = {
                    activePairingData = it
                    showPairingDialog = true
                },
                onScanPairingCode = { showMobileScannerDialog = true },
                modifier = Modifier.fillMaxWidth()
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
                            horizontal = HORIZONTAL_PADDING,
                            vertical = 10.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults, key = { it.noteId }) { meta ->
                            DailySearchResultCard(
                                note = meta,
                                onClick = {
                                    meta.dateString?.let { viewModel.selectDate(LocalDate.parse(it)) }
                                    onClearSearch()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // RIGHT PANEL — pager + editor
    val rightPanelContent = @Composable {
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
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open Sidebar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        globalTags = globalTags,
                        actions = sharedEditorActions, // <-- Using the hoisted actions
                        focusRequest = if (isCurrentActivePage) focusRequest else null,
                        selectedBlockIds = selectedBlockIds,
                        hazeState = hazeState,
                        bottomContentPadding = bottomContentPadding,
                        topContentPadding = if (isDesktopPlatform) {
                            if (!isSidebarVisible) 72.dp else 16.dp
                        } else 0.dp
                    )
                }
            }

            // Editor toolbar
            AnimatedVisibility(
                visible = showToolbar,
                enter = fadeIn(tween(120)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(80)) + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                EditorToolbar(
                    hazeState = hazeState,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { sharedEditorActions.onUndo() },
                    onRedo = { sharedEditorActions.onRedo() },
                    onChangeBlockType = { sharedEditorActions.onChangeBlockType(it) },
                    onToggleFormat = { sharedEditorActions.onToggleFormat(it) },
                    onAdjustIndentation = { sharedEditorActions.onAdjustIndentation(it) },
                    onInsertMediaBlock = { sharedEditorActions.onInsertMediaBlock(it) }
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
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
            )
        }
    }

    // MAIN SCAFFOLD
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 300.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .statusBarsPadding()
        ) {
            if (isDesktopPlatform) {
                // DESKTOP: side-by-side panels
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
                                    top = PANEL_PADDING,
                                    bottom = PANEL_PADDING,
                                    start = PANEL_PADDING
                                )
                                .width(sidebarWidth)
                                .fillMaxHeight()
                                .customInlyShadow(DesktopPanelShape)
                                .clip(DesktopPanelShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    leftPanelContent()
                                }
                                desktopBottomBar?.invoke()
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(PANEL_PADDING)
                            .padding(top = 4.dp)
                            .customInlyShadow(DesktopPanelShape)
                            .clip(DesktopPanelShape)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        rightPanelContent()
                    }
                }

            } else {
                // MOBILE: vertical stack
                Column(Modifier.fillMaxSize()) {
                    StaticDateHeader(
                        selectedDate = selectedDate,
                        onDateSelected = { viewModel.selectDate(it) },
                        onCalendarIconClick = { showBottomSheet = true },
                        onToggleSidebar = onToggleSidebar,
                        showNotesMenu = showNotesMenu,
                        onNotesMenuToggle = { showNotesMenu = it },
                        onNavigateToTrash = onNavigateToTrash,
                        settingsManager = settingsManager,
                        syncViewModel = syncViewModel,
                        onShowPairingCode = {
                            activePairingData = it
                            showPairingDialog = true
                        },
                        onScanPairingCode = { showMobileScannerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        rightPanelContent()

                        this@Column.AnimatedVisibility(
                            visible = searchQuery.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(color = MaterialTheme.colorScheme.background) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        horizontal = HORIZONTAL_PADDING,
                                        vertical = 10.dp
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sync Dialogs for Daily Screen UserSettings
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onDateSelected: (LocalDate) -> Unit,
    onCalendarIconClick: () -> Unit,
    onToggleSidebar: () -> Unit,
    showNotesMenu: Boolean,
    onNotesMenuToggle: (Boolean) -> Unit,
    onNavigateToTrash: () -> Unit,
    settingsManager: SettingsManager,
    syncViewModel: SyncViewModel,
    onShowPairingCode: (SyncPairingData) -> Unit,
    onScanPairingCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = HORIZONTAL_PADDING,
                    end = HORIZONTAL_PADDING,
                    top = if (isDesktopPlatform) 16.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isDesktopPlatform) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = if (isDesktopPlatform) (-6).dp else 0.dp)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    val day = selectedDate.dayOfMonth
                    val suffix = when {
                        day in 11..13 -> "th"
                        day % 10 == 1 -> "st"
                        day % 10 == 2 -> "nd"
                        day % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    val months = arrayOf(
                        "", "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )
                    val headerText = if (selectedDate == Clock.System.todayIn(
                            TimeZone.currentSystemDefault()
                        )
                    ) {
                        "Today"
                    } else {
                        "${months[selectedDate.monthNumber]} $day$suffix"
                    }

                    Text(
                        text = headerText,
                        fontFamily = PoppinsFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onNotesMenuToggle(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    UserSettings(
                        expanded = showNotesMenu,
                        onDismiss = { onNotesMenuToggle(false) },
                        onNavigateToTrash = {
                            onNavigateToTrash()
                            onNotesMenuToggle(false)
                        },
                        onShowPairingCode = {
                            onNotesMenuToggle(false)
                            val currentIp = getLocalNetworkIp()
                            val newToken = generateSecureToken()
                            val newEncryptionKey = generateSecureToken() + generateSecureToken()
                            settingsManager.saveSyncAuthToken(newToken)
                            settingsManager.saveSyncEncryptionKey(newEncryptionKey)
                            onShowPairingCode(
                                SyncPairingData(
                                    ipAddress = currentIp,
                                    port = 8080,
                                    authToken = newToken,
                                    encryptionKey = newEncryptionKey
                                )
                            )
                        },
                        onScanPairingCode = {
                            onNotesMenuToggle(false)
                            onScanPairingCode()
                        },
                        onSyncNow = {
                            onNotesMenuToggle(false)
                            syncViewModel.triggerManualSync()
                        }
                    )
                }
            }
        }

        CalendarStrip(selectedDate = selectedDate, onDateSelected = onDateSelected)
    }
}