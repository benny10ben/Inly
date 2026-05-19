package com.ben.inly.presentation.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.sync.SyncPairingData
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.notes.notes.StandaloneNoteScreen
import com.ben.inly.presentation.notes.overview.bookmarks.BookmarksScreen
import com.ben.inly.presentation.notes.overview.documents.DocumentsScreen
import com.ben.inly.presentation.notes.overview.images.ImagesScreen
import com.ben.inly.presentation.notes.overview.reminders.RemindersScreen
import com.ben.inly.presentation.shared.SyncPairingDialog
import com.ben.inly.presentation.shared.SyncScannerDialog
import com.ben.inly.presentation.shared.UserSettings
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.sync.generateSecureToken
import com.ben.inly.presentation.shared.sync.getLocalNetworkIp
import com.ben.inly.presentation.shared.trash.TrashScreen
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import okio.Path.Companion.toPath

private val HORIZONTAL_PADDING = 16.dp
private val PANEL_PADDING = 16.dp
private val DefaultCornerShape = RoundedCornerShape(6.dp)
private val DesktopPanelShape = RoundedCornerShape(6.dp)

// Mouse-wheel → horizontal scroll (desktop)
@Composable
private fun Modifier.mouseScrollable(scrollState: ScrollableState): Modifier {
    val scope = rememberCoroutineScope()
    return this.pointerInput(scrollState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.firstOrNull()
                    val delta = change?.scrollDelta?.y ?: change?.scrollDelta?.x ?: 0f
                    if (delta != 0f) {
                        scope.launch { scrollState.scrollBy(delta * 75f) }
                        change?.consume()
                    }
                }
            }
        }
    }
}

// No-ripple helper
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

// NotesScreen
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    searchQuery: String,
    isSearchActive: Boolean,
    onClearSearch: () -> Unit,
    viewModel: NotesViewModel = koinViewModel(),
    settingsManager: SettingsManager = koinInject(),
    onNavigateBack: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onNavigateToEditor: (String) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToImages: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onNavigateToTrash: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    desktopBottomBar: (@Composable () -> Unit)? = null,
    isSidebarVisible: Boolean = true,
    sidebarWidth: Dp = 340.dp,
    onToggleSidebar: () -> Unit = {},
    onSidebarWidthChange: (Dp) -> Unit = {},
    syncViewModel: SyncViewModel = koinViewModel(),
) {
    val hazeState = remember { HazeState() }

    val isLoading         by viewModel.isLoading.collectAsState()
    val subFolders        by viewModel.currentSubFolders.collectAsState()
    val breadcrumbs       by viewModel.breadcrumbs.collectAsState()
    val notes             by viewModel.notes.collectAsState()
    val recentNotes       by viewModel.recentNotes.collectAsState()
    val selectedFolderId  by viewModel.selectedFolderId.collectAsState()
    val selectedNoteIds   by viewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val favoriteNotes     by viewModel.favoriteNotes.collectAsState()

    val remindersCount  by viewModel.remindersCount.collectAsState()
    val bookmarksCount  by viewModel.bookmarksCount.collectAsState()
    val imagesCount     by viewModel.imagesCount.collectAsState()
    val documentsCount  by viewModel.documentsCount.collectAsState()

    val currentSortType  by viewModel.sortType.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()

    val lastOpenedState by settingsManager.lastOpenedDesktopStateFlow.collectAsState(initial = "")

    var showSortMenu  by remember { mutableStateOf(false) }
    var showNotesMenu by remember { mutableStateOf(false) }

    // Mobile-only dialog flags
    var showAddNoteDialog   by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }

    // Desktop anchor-popup flags + their input state
    var showAddNotePopup   by remember { mutableStateOf(false) }
    var showAddFolderPopup by remember { mutableStateOf(false) }
    var addNoteInput       by remember { mutableStateOf("") }
    var addFolderInput     by remember { mutableStateOf("") }

    var desktopSelectedNoteId      by remember { mutableStateOf<String?>(null) }
    var isTrashOpenDesktop         by remember { mutableStateOf(false) }
    var hasAutoSelectedInitialNote by remember { mutableStateOf(false) }
    var isRemindersOpenDesktop     by remember { mutableStateOf(false) }
    var isImagesOpenDesktop        by remember { mutableStateOf(false) }
    var isDocumentsOpenDesktop     by remember { mutableStateOf(false) }
    var isBookmarksOpenDesktop     by remember { mutableStateOf(false) }

    // Header Toggle States
    var isFavoritesExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded     by remember { mutableStateOf(true) }
    var isRecentsExpanded   by remember { mutableStateOf(true) }

    // Scroll States for horizontal rows
    val favListState    = rememberLazyListState()
    val folderListState = rememberLazyListState()
    val recentListState = rememberLazyListState()

    val isSelectionMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

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

    KmpBackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }
    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }
    LaunchedEffect(searchQuery)     { viewModel.updateSearchQuery(searchQuery) }

    LaunchedEffect(isDesktopPlatform, lastOpenedState, isLoading) {
        if (isDesktopPlatform && !hasAutoSelectedInitialNote && !isLoading) {
            when {
                lastOpenedState == "TRASH" -> { isTrashOpenDesktop = true; desktopSelectedNoteId = null }
                lastOpenedState.isNotEmpty() -> { isTrashOpenDesktop = false; desktopSelectedNoteId = lastOpenedState }
                else -> desktopSelectedNoteId = recentNotes.firstOrNull()?.noteId ?: notes.firstOrNull()?.noteId
            }
            hasAutoSelectedInitialNote = true
        }
    }

    LaunchedEffect(desktopSelectedNoteId, isTrashOpenDesktop) {
        if (hasAutoSelectedInitialNote && isDesktopPlatform) {
            val stateToSave = if (isTrashOpenDesktop) "TRASH" else desktopSelectedNoteId ?: ""
            settingsManager.saveLastOpenedDesktopState(stateToSave)
        }
    }

    val handleCreateFolder = { name: String ->
        viewModel.createNewFolder(name)
        showAddFolderDialog = false
    }

    val handleCreateNote = { title: String ->
        viewModel.createNewNote(title = title, forceHomeFolder = false) { newNoteId ->
            if (isDesktopPlatform) {
                desktopSelectedNoteId = newNoteId
                isTrashOpenDesktop = false; isRemindersOpenDesktop = false
                isImagesOpenDesktop = false; isDocumentsOpenDesktop = false
                isBookmarksOpenDesktop = false
            } else {
                onNavigateToEditor(newNoteId)
            }
        }
        showAddNoteDialog = false
    }

    // Left panel content — shared between mobile & desktop sidebar
    val leftPanelContent = @Composable {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardWidth = (maxWidth - (HORIZONTAL_PADDING * 2) - 10.dp) / 2

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        top = if (isDesktopPlatform) 16.dp else 10.dp,
                        bottom = bottomContentPadding + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    verticalItemSpacing = 10.dp,
                    modifier = Modifier.fillMaxSize()
                ) {

                    // ── Breadcrumbs + overflow menu ─────────────────────────
                    if (!isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = HORIZONTAL_PADDING,
                                        end = HORIZONTAL_PADDING - 8.dp,
                                        bottom = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isDesktopPlatform) {
                                        IconButton(
                                            onClick = onToggleSidebar,
                                            modifier = Modifier.offset(x = (-8).dp)
                                        ) {
                                            Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    BreadcrumbTrail(
                                        selectedFolderId = selectedFolderId,
                                        breadcrumbs = breadcrumbs,
                                        onNavigate = {
                                            viewModel.selectFolder(it)
                                            desktopSelectedNoteId = null
                                            isTrashOpenDesktop = false
                                            isRemindersOpenDesktop = false
                                            isImagesOpenDesktop = false
                                            isDocumentsOpenDesktop = false
                                            isBookmarksOpenDesktop = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .offset(x = if (isDesktopPlatform) (-6).dp else 0.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showNotesMenu = true }) {
                                        Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    UserSettings(
                                        expanded = showNotesMenu,
                                        onDismiss = { showNotesMenu = false },
                                        onNavigateToTrash = {
                                            if (isDesktopPlatform) {
                                                isTrashOpenDesktop = true
                                                desktopSelectedNoteId = null
                                                isRemindersOpenDesktop = false
                                                isImagesOpenDesktop = false
                                                isDocumentsOpenDesktop = false
                                                isBookmarksOpenDesktop = false
                                                showNotesMenu = false
                                            } else {
                                                onNavigateToTrash()
                                                showNotesMenu = false
                                            }
                                        },
                                        onShowPairingCode = {
                                            showNotesMenu = false

                                            val currentIp = getLocalNetworkIp()
                                            val newToken = generateSecureToken()
                                            val newEncryptionKey = generateSecureToken()

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
                                            showNotesMenu = false
                                            showMobileScannerDialog = true
                                        },
                                        onSyncNow = {
                                            showNotesMenu = false
                                            syncViewModel.triggerManualSync()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── Overview cards ──────────────────────────────────────
                    if (selectedFolderId == null && !isSelectionMode && searchQuery.isBlank()) {
                        item {
                            Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Reminders", "$remindersCount left", onClick = {
                                    if (isDesktopPlatform) {
                                        desktopSelectedNoteId = null; isTrashOpenDesktop = false
                                        isRemindersOpenDesktop = true
                                    } else onNavigateToReminders()
                                })
                            }
                        }
                        item {
                            Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Bookmarks", "$bookmarksCount saved", onClick = {
                                    if (isDesktopPlatform) {
                                        desktopSelectedNoteId = null; isTrashOpenDesktop = false
                                        isRemindersOpenDesktop = false; isImagesOpenDesktop = false
                                        isDocumentsOpenDesktop = false; isBookmarksOpenDesktop = true
                                    } else onNavigateToBookmarks()
                                })
                            }
                        }
                        item {
                            Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Images", "$imagesCount saved", onClick = {
                                    if (isDesktopPlatform) {
                                        desktopSelectedNoteId = null; isTrashOpenDesktop = false
                                        isRemindersOpenDesktop = false; isImagesOpenDesktop = true
                                    } else onNavigateToImages()
                                })
                            }
                        }
                        item {
                            Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Documents", "$documentsCount attached", onClick = {
                                    if (isDesktopPlatform) {
                                        desktopSelectedNoteId = null; isTrashOpenDesktop = false
                                        isRemindersOpenDesktop = false; isImagesOpenDesktop = false
                                        isDocumentsOpenDesktop = true
                                    } else onNavigateToDocuments()
                                })
                            }
                        }
                    }

                    // ── Favorites ───────────────────────────────────────────
                    if (selectedFolderId == null && favoriteNotes.isNotEmpty() && !isSelectionMode && searchQuery.isBlank()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .noRippleClickable { isFavoritesExpanded = !isFavoritesExpanded }
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Favorites",
                                        fontFamily = BricolageFont,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isFavoritesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Favorites",
                                        modifier = Modifier.padding(start = 4.dp).size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (isFavoritesExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(
                                    state = favListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .mouseScrollable(favListState),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)
                                ) {
                                    items(favoriteNotes, key = { "fav_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(
                                                note = note,
                                                isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = {
                                                    if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
                                                    else if (isDesktopPlatform) { desktopSelectedNoteId = note.noteId; isTrashOpenDesktop = false }
                                                    else onNavigateToEditor(note.noteId)
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Notes header ────────────────────────────────────────
                    if (notes.isNotEmpty() || !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .noRippleClickable { isNotesExpanded = !isNotesExpanded }
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Notes",
                                        fontFamily = BricolageFont,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Notes",
                                        modifier = Modifier.padding(start = 4.dp).size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isSelectionMode) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            Icon(
                                                Icons.Default.SwapVert, "Sort",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .noRippleClickable { showSortMenu = true },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isDesktopPlatform) {
                                                DropdownMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    DesktopSortMenu(
                                                        currentSortType = currentSortType,
                                                        currentSortOrder = currentSortOrder,
                                                        onDismiss = { showSortMenu = false },
                                                        onSortChanged = { type, order ->
                                                            viewModel.updateSort(type, order)
                                                            showSortMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // ── Add Note anchor ─────────────────
                                        Box {
                                            Icon(
                                                Icons.Default.Add, "New Note",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .noRippleClickable {
                                                        if (isDesktopPlatform) {
                                                            addNoteInput = ""
                                                            showAddNotePopup = true
                                                        } else {
                                                            showAddNoteDialog = true
                                                        }
                                                    },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isDesktopPlatform) {
                                                DropdownMenu(
                                                    expanded = showAddNotePopup,
                                                    onDismissRequest = { showAddNotePopup = false },
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .width(280.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(
                                                            horizontal = 16.dp,
                                                            vertical = 12.dp
                                                        )
                                                    ) {
                                                        Text(
                                                            "New Note",
                                                            fontFamily = BricolageFont,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(bottom = 10.dp)
                                                        )
                                                        OutlinedTextField(
                                                            value = addNoteInput,
                                                            onValueChange = { addNoteInput = it },
                                                            placeholder = {
                                                                Text(
                                                                    "Note title...",
                                                                    fontFamily = BricolageFont,
                                                                    fontSize = 13.sp
                                                                )
                                                            },
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = DefaultCornerShape,
                                                            textStyle = TextStyle(
                                                                fontFamily = BricolageFont,
                                                                fontSize = 14.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        )
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 10.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Button(
                                                                onClick = { showAddNotePopup = false },
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(38.dp),
                                                                shape = DefaultCornerShape,
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                                ),
                                                                elevation = ButtonDefaults.buttonElevation(0.dp)
                                                            ) {
                                                                Text(
                                                                    "Cancel",
                                                                    fontFamily = BricolageFont,
                                                                    fontSize = 13.sp
                                                                )
                                                            }
                                                            Button(
                                                                onClick = {
                                                                    if (addNoteInput.isNotBlank()) {
                                                                        handleCreateNote(addNoteInput.trim())
                                                                        showAddNotePopup = false
                                                                    }
                                                                },
                                                                enabled = addNoteInput.isNotBlank(),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(38.dp),
                                                                shape = DefaultCornerShape,
                                                                elevation = ButtonDefaults.buttonElevation(0.dp)
                                                            ) {
                                                                Text(
                                                                    "Create",
                                                                    fontFamily = BricolageFont,
                                                                    fontSize = 13.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isNotesExpanded) {
                        // ── Folder pills ────────────────────────────────────────
                        if (subFolders.isNotEmpty() || !isSelectionMode) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(
                                    state = folderListState,
                                    contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .mouseScrollable(folderListState)
                                ) {
                                    if (!isSelectionMode) {
                                        item {
                                            // ── Add Folder anchor ───────────
                                            Box {
                                                FolderPill(
                                                    name = "New",
                                                    isSelected = false,
                                                    isNewButton = true,
                                                    onClick = {
                                                        if (isDesktopPlatform) {
                                                            addFolderInput = ""
                                                            showAddFolderPopup = true
                                                        } else {
                                                            showAddFolderDialog = true
                                                        }
                                                    },
                                                    onLongClick = {}
                                                )
                                                if (isDesktopPlatform) {
                                                    DropdownMenu(
                                                        expanded = showAddFolderPopup,
                                                        onDismissRequest = { showAddFolderPopup = false },
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.surface)
                                                            .width(280.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                "New Folder",
                                                                fontFamily = BricolageFont,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(bottom = 10.dp)
                                                            )
                                                            OutlinedTextField(
                                                                value = addFolderInput,
                                                                onValueChange = { addFolderInput = it },
                                                                placeholder = {
                                                                    Text(
                                                                        "e.g. Personal, Work...",
                                                                        fontFamily = BricolageFont,
                                                                        fontSize = 13.sp
                                                                    )
                                                                },
                                                                singleLine = true,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                shape = DefaultCornerShape,
                                                                textStyle = TextStyle(
                                                                    fontFamily = BricolageFont,
                                                                    fontSize = 14.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            )
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(top = 10.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Button(
                                                                    onClick = { showAddFolderPopup = false },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(38.dp),
                                                                    shape = DefaultCornerShape,
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    ),
                                                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                                                ) {
                                                                    Text(
                                                                        "Cancel",
                                                                        fontFamily = BricolageFont,
                                                                        fontSize = 13.sp
                                                                    )
                                                                }
                                                                Button(
                                                                    onClick = {
                                                                        if (addFolderInput.isNotBlank()) {
                                                                            handleCreateFolder(addFolderInput.trim())
                                                                            showAddFolderPopup = false
                                                                        }
                                                                    },
                                                                    enabled = addFolderInput.isNotBlank(),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(38.dp),
                                                                    shape = DefaultCornerShape,
                                                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                                                ) {
                                                                    Text(
                                                                        "Create",
                                                                        fontFamily = BricolageFont,
                                                                        fontSize = 13.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    items(subFolders, key = { it.folderId }) { folder ->
                                        FolderPill(
                                            name = folder.name,
                                            isSelected = selectedFolderIds.contains(folder.folderId),
                                            onClick = {
                                                if (isSelectionMode) viewModel.toggleFolderSelection(folder.folderId)
                                                else {
                                                    viewModel.selectFolder(folder.folderId)
                                                    desktopSelectedNoteId = null
                                                    isTrashOpenDesktop = false
                                                }
                                            },
                                            onLongClick = { viewModel.toggleFolderSelection(folder.folderId) }
                                        )
                                    }
                                }
                            }
                        }

                        // ── Note cards (staggered — natural height) ─────────────
                        itemsIndexed(notes, key = { _, note -> note.noteId }) { index, note ->
                            val sidePad = if (index % 2 == 0) Modifier.padding(start = HORIZONTAL_PADDING)
                            else Modifier.padding(end = HORIZONTAL_PADDING)
                            Box(modifier = sidePad) {
                                NoteCard(
                                    note = note,
                                    isSelected = selectedNoteIds.contains(note.noteId),
                                    onClick = {
                                        if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
                                        else if (isDesktopPlatform) {
                                            desktopSelectedNoteId = note.noteId
                                            isTrashOpenDesktop = false
                                        } else onNavigateToEditor(note.noteId)
                                    },
                                    onLongClick = { viewModel.toggleNoteSelection(note.noteId) }
                                )
                            }
                        }
                    }

                    // ── Recents ─────────────────────────────────────────────
                    if (selectedFolderId == null && recentNotes.isNotEmpty() && !isSelectionMode && searchQuery.isBlank()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .noRippleClickable { isRecentsExpanded = !isRecentsExpanded }
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recents",
                                        fontFamily = BricolageFont,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isRecentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Recents",
                                        modifier = Modifier.padding(start = 4.dp).size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (isRecentsExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(
                                    state = recentListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .mouseScrollable(recentListState),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)
                                ) {
                                    items(recentNotes, key = { "recent_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(
                                                note = note,
                                                isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = {
                                                    if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
                                                    else if (isDesktopPlatform) {
                                                        desktopSelectedNoteId = note.noteId
                                                        isTrashOpenDesktop = false
                                                    } else onNavigateToEditor(note.noteId)
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    // Main Scaffold
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                .haze(state = hazeState)
        ) {
            if (isDesktopPlatform) {
                Row(modifier = Modifier.fillMaxSize()) {

                    // Left panel
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
                                .padding(top = PANEL_PADDING, bottom = PANEL_PADDING, start = PANEL_PADDING)
                                .width(sidebarWidth)
                                .fillMaxHeight()
                                .shadow(4.dp, DesktopPanelShape)
                                .clip(DesktopPanelShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) { leftPanelContent() }
                                desktopBottomBar?.invoke()
                            }
                        }
                    }

                    // Right panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(PANEL_PADDING)
                            .padding(top = 4.dp)
                            .shadow(4.dp, DesktopPanelShape)
                            .clip(DesktopPanelShape)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (!isSidebarVisible) {
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
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Box(Modifier.fillMaxSize()) {
                            when {
                                isTrashOpenDesktop -> key("trash_view") {
                                    TrashScreen(onNavigateBack = { isTrashOpenDesktop = false })
                                }
                                isRemindersOpenDesktop -> key("reminders_view") {
                                    RemindersScreen(
                                        onNavigateBack = { isRemindersOpenDesktop = false },
                                        onOpenFile = onOpenFile
                                    )
                                }
                                isImagesOpenDesktop -> key("images_view") {
                                    ImagesScreen(
                                        onNavigateBack = { isImagesOpenDesktop = false },
                                        onTriggerImagePicker = { onPickImage { } }
                                    )
                                }
                                isDocumentsOpenDesktop -> key("documents_view") {
                                    DocumentsScreen(
                                        onNavigateBack = { isDocumentsOpenDesktop = false },
                                        onTriggerDocumentPicker = { onPickDocument { } },
                                        onOpenFile = onOpenFile
                                    )
                                }
                                isBookmarksOpenDesktop -> key("bookmarks_view") {
                                    BookmarksScreen(onNavigateBack = { isBookmarksOpenDesktop = false })
                                }
                                desktopSelectedNoteId != null -> key(desktopSelectedNoteId) {
                                    StandaloneNoteScreen(
                                        noteId = desktopSelectedNoteId!!,
                                        onNavigateBack = { desktopSelectedNoteId = null },
                                        onSelectionModeChange = onSelectionModeChange,
                                        onPickImage = onPickImage,
                                        onPickDocument = onPickDocument,
                                        onOpenFile = onOpenFile
                                    )
                                }
                                else -> DesktopEmptyState()
                            }
                        }
                    }
                }
            } else {
                leftPanelContent()
            }

            NotesSelectionPill(
                isVisible = isSelectionMode,
                selectedCount = selectedNoteIds.size + selectedFolderIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onDelete = { viewModel.deleteSelectedItems() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Mobile-only bottom sheets
            if (!isDesktopPlatform) {
                AddFolderBottomSheet(
                    expanded = showAddFolderDialog,
                    onDismiss = { showAddFolderDialog = false },
                    onCreate = handleCreateFolder
                )
                AddNoteBottomSheet(
                    expanded = showAddNoteDialog,
                    onDismiss = { showAddNoteDialog = false },
                    onCreate = handleCreateNote
                )
                SortBottomSheet(
                    expanded = showSortMenu,
                    currentSortType = currentSortType,
                    currentSortOrder = currentSortOrder,
                    onDismiss = { showSortMenu = false },
                    onSortChanged = { type, order ->
                        viewModel.updateSort(type, order)
                        showSortMenu = false
                    }
                )
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

                        // Save the connection secrets!
                        settingsManager.saveSyncIpAddress(pairingData.ipAddress)
                        settingsManager.saveSyncPort(pairingData.port)
                        settingsManager.saveSyncAuthToken(pairingData.authToken)

                        // SAVE THE ENCRYPTION KEY!
                        settingsManager.saveSyncEncryptionKey(pairingData.encryptionKey) // <--- ADD THIS LINE!

                        // TELL THE USER IT WORKED!
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Paired with ${pairingData.ipAddress}!")
                        }
                    }
                )
            }
        }
    }
}

// Desktop helpers
@Composable
private fun DesktopEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Edit, null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
            Text(
                "Select a note to start writing",
                fontFamily = BricolageFont,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun DesktopSortMenu(
    currentSortType: SortType,
    currentSortOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortChanged: (SortType, SortOrder) -> Unit
) {
    Column(modifier = Modifier.width(200.dp).padding(vertical = 4.dp)) {
        DesktopSortOptionItem("Last Edited", currentSortType == SortType.LAST_EDITED) {
            onDismiss(); onSortChanged(SortType.LAST_EDITED, currentSortOrder)
        }
        DesktopSortOptionItem("Date Created", currentSortType == SortType.DATE_CREATED) {
            onDismiss(); onSortChanged(SortType.DATE_CREATED, currentSortOrder)
        }
        DesktopSortOptionItem("Name (A-Z)", currentSortType == SortType.NAME) {
            onDismiss(); onSortChanged(SortType.NAME, currentSortOrder)
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        DesktopSortOptionItem("Ascending", currentSortOrder == SortOrder.ASCENDING) {
            onDismiss(); onSortChanged(currentSortType, SortOrder.ASCENDING)
        }
        DesktopSortOptionItem("Descending", currentSortOrder == SortOrder.DESCENDING) {
            onDismiss(); onSortChanged(currentSortType, SortOrder.DESCENDING)
        }
    }
}

@Composable
private fun DesktopSortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .noRippleClickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            fontFamily = BricolageFont,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check, "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Shared components
@Composable
fun OverviewCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = DefaultCornerShape,
        color = if (isDesktopPlatform) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultCornerShape)
            .noRippleClickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                fontFamily = BricolageFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontFamily = BricolageFont,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BreadcrumbTrail(
    selectedFolderId: String?,
    breadcrumbs: List<FolderEntity>,
    onNavigate: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        item {
            val isRoot = selectedFolderId == null
            Text(
                "Home",
                fontFamily = BricolageFont,
                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                fontSize = 20.sp,
                color = if (isRoot) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.noRippleClickable { onNavigate(null) }
            )
        }
        items(breadcrumbs) { folder ->
            Icon(
                Icons.Default.ChevronRight, null,
                modifier = Modifier.padding(horizontal = 6.dp).size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val isLast = folder.folderId == selectedFolderId
            Text(
                folder.name,
                fontFamily = BricolageFont,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                fontSize = 18.sp,
                color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.noRippleClickable { onNavigate(folder.folderId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderPill(
    name: String,
    isSelected: Boolean,
    isNewButton: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = when {
        isNewButton       -> MaterialTheme.colorScheme.primary
        isSelected        -> MaterialTheme.colorScheme.onSurface
        isDesktopPlatform -> MaterialTheme.colorScheme.background
        else              -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isNewButton -> MaterialTheme.colorScheme.onPrimary
        isSelected  -> MaterialTheme.colorScheme.background
        else        -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = DefaultCornerShape,
        color = bgColor,
        contentColor = textColor,
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp, minWidth = 72.dp)
            .clip(DefaultCornerShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(if (isNewButton) Icons.Default.Add else Icons.Default.Folder, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(name, fontFamily = BricolageFont, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            AnimatedVisibility(visible = isSelected && !isNewButton) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteMetadataEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = when {
        isSelected        -> MaterialTheme.colorScheme.onSurface
        isDesktopPlatform -> MaterialTheme.colorScheme.background
        else              -> MaterialTheme.colorScheme.surface
    }
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant

    val coverHeight  = 72.dp
    val iconOverhang = 12.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(DefaultCornerShape)
            .background(bgColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(Modifier.fillMaxSize()) {

            // Cover image area
            Box(modifier = Modifier.fillMaxWidth().height(coverHeight)) {
                if (note.coverImagePath != null) {
                    AsyncImage(
                        model = note.coverImagePath.removePrefix("file://").toPath(),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isSelected) 0.12f else 0.05f
                                )
                            )
                    )
                }
                if (note.isFavorite) {
                    Icon(
                        Icons.Default.Star, "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp)
                    )
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = 12.dp, end = 12.dp,
                        top = if (!note.icon.isNullOrEmpty()) iconOverhang + 10.dp else 10.dp,
                        bottom = 10.dp
                    )
            ) {
                Text(
                    note.title.ifEmpty { "Untitled" },
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    note.snippet.ifEmpty { "Empty note..." },
                    fontFamily = BricolageFont,
                    fontSize = 12.sp,
                    color = mutedColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
        }

        // Icon straddling the cover / body seam
        if (!note.icon.isNullOrEmpty()) {
            Text(
                text = note.icon,
                fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp)
                    .offset(y = coverHeight - iconOverhang)
            )
        }

        // Selection checkmark
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check, "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun NotesSelectionPill(
    isVisible: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val pillColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
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
                .padding(bottom = 28.dp)
                .shadow(6.dp, DefaultCornerShape, spotColor = Color.Black.copy(alpha = 0.2f))
        ) {
            val pillScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .horizontalScroll(pillScroll)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    Icons.Default.Close, "Clear",
                    modifier = Modifier.size(18.dp).noRippleClickable { onClearSelection() },
                    tint = tint
                )
                Text(
                    "$selectedCount",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = tint
                )
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                Icon(
                    Icons.Default.Delete, "Move to Trash",
                    modifier = Modifier.size(18.dp).noRippleClickable { onDelete() },
                    tint = tint
                )
            }
        }
    }
}

// Bottom sheets (mobile only)
@Composable
fun AddFolderBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "New Folder",
        subtitle = "Organize your notes with a new category."
    ) { closeAnd ->
        OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            placeholder = {
                Text(
                    "e.g. Personal, Work...",
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            textStyle = TextStyle(
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = DefaultCornerShape
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Button(
                onClick = { if (folderName.isNotBlank()) closeAnd { onCreate(folderName.trim()) } },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AddNoteBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var noteTitle by remember { mutableStateOf("") }
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "New Note",
        subtitle = "Give your note a title, or leave it blank."
    ) { closeAnd ->
        OutlinedTextField(
            value = noteTitle,
            onValueChange = { noteTitle = it },
            placeholder = {
                Text(
                    "Note title...",
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            textStyle = TextStyle(
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = DefaultCornerShape
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Button(
                onClick = { closeAnd { onCreate(noteTitle.trim()) } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Create", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun SortBottomSheet(
    expanded: Boolean,
    currentSortType: SortType,
    currentSortOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortChanged: (SortType, SortOrder) -> Unit
) {
    InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Sort By") { closeAnd ->
        SortOptionItem("Last Edited", currentSortType == SortType.LAST_EDITED) {
            closeAnd { onSortChanged(SortType.LAST_EDITED, currentSortOrder) }
        }
        SortOptionItem("Date Created", currentSortType == SortType.DATE_CREATED) {
            closeAnd { onSortChanged(SortType.DATE_CREATED, currentSortOrder) }
        }
        SortOptionItem("Name (A-Z)", currentSortType == SortType.NAME) {
            closeAnd { onSortChanged(SortType.NAME, currentSortOrder) }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        Text(
            "Order",
            fontFamily = BricolageFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        SortOptionItem("Ascending", currentSortOrder == SortOrder.ASCENDING) {
            closeAnd { onSortChanged(currentSortType, SortOrder.ASCENDING) }
        }
        SortOptionItem("Descending", currentSortOrder == SortOrder.DESCENDING) {
            closeAnd { onSortChanged(currentSortType, SortOrder.DESCENDING) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            fontFamily = BricolageFont,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check, "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}