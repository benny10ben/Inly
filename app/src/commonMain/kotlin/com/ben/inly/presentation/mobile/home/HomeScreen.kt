package com.ben.inly.presentation.mobile.home

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.sync.SyncPairingData
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.UserSettings
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.components.TopBarIconButton
import com.ben.inly.presentation.sync.SyncPairingDialog
import com.ben.inly.presentation.sync.SyncScannerDialog
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.presentation.sync.generateSecureToken
import com.ben.inly.presentation.sync.getLocalNetworkIp
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyButtonSecondary
import com.ben.inly.presentation.shared.components.InlyTextField
import inly.app.generated.resources.Res
import inly.app.generated.resources.arrow_up_down
import inly.app.generated.resources.ellipsis
import inly.app.generated.resources.file_plus_corner
import inly.app.generated.resources.folder
import inly.app.generated.resources.folder_plus
import org.jetbrains.compose.resources.painterResource

private val HORIZONTAL_PADDING = 16.dp
private val DefaultCornerShape = RoundedCornerShape(12.dp)

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

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    settingsManager: SettingsManager = koinInject(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onNavigateToEditor: (String) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToImages: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onNavigateToTrash: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onToggleSidebar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    syncViewModel: SyncViewModel = koinViewModel(),
) {
    val hazeState = remember { HazeState() }

    val isLoading by viewModel.isLoading.collectAsState()
    val subFolders by viewModel.currentSubFolders.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val favoriteNotes by viewModel.favoriteNotes.collectAsState()

    val remindersCount by viewModel.remindersCount.collectAsState()
    val bookmarksCount by viewModel.bookmarksCount.collectAsState()
    val imagesCount by viewModel.imagesCount.collectAsState()
    val documentsCount by viewModel.documentsCount.collectAsState()

    val currentSortType by viewModel.sortType.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showNotesMenu by remember { mutableStateOf(false) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }

    var showAddNotePopup by remember { mutableStateOf(false) }
    var showAddFolderPopup by remember { mutableStateOf(false) }
    var addNoteInput by remember { mutableStateOf("") }
    var addFolderInput by remember { mutableStateOf("") }

    var isFavoritesExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded by remember { mutableStateOf(true) }
    var isRecentsExpanded by remember { mutableStateOf(true) }

    val favListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    val recentListState = rememberLazyListState()

    val gridState = rememberLazyStaggeredGridState()

    val isSelectionMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    var showPairingDialog by remember { mutableStateOf(false) }
    var activePairingData by remember { mutableStateOf<SyncPairingData?>(null) }
    var showMobileScannerDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val syncState by syncViewModel.syncStatus.collectAsState()

    LaunchedEffect(syncState) {
        if (syncState != "Idle" && syncState != "Syncing...") {
            snackbarHostState.showSnackbar(message = syncState)
            syncViewModel.resetSyncStatus()
        }
    }

    KmpBackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }
    KmpBackHandler(enabled = selectedFolderId != null) { viewModel.navigateUp() }

    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }

    val handleCreateFolder = { name: String ->
        viewModel.createNewFolder(name)
        showAddFolderDialog = false
    }

    val handleCreateNote = { title: String ->
        viewModel.createNewNote(title = title, forceHomeFolder = false) { newNoteId ->
            onNavigateToEditor(newNoteId)
        }
        showAddNoteDialog = false
    }

    // Mobile grid panel
    val leftPanelContent = @Composable {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardWidth = (maxWidth - (HORIZONTAL_PADDING * 2) - 10.dp) / 2

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        top = (if (isDesktopPlatform) 16.dp else 10.dp) + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = bottomContentPadding + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    verticalItemSpacing = 10.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING - 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isDesktopPlatform) {
                                        IconButton(onClick = onToggleSidebar, modifier = Modifier.offset(x = (-8).dp)) {
                                            Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    BreadcrumbTrail(
                                        selectedFolderId = selectedFolderId,
                                        breadcrumbs = breadcrumbs,
                                        onNavigate = { viewModel.selectFolder(it) },
                                        modifier = Modifier.weight(1f).offset(x = if (isDesktopPlatform) (-6).dp else 0.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(44.dp))
                            }
                        }
                    }

                    if (selectedFolderId == null && !isSelectionMode) {
                        item {
                            Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Tasks", "$remindersCount left", onClick = { onNavigateToReminders() })
                            }
                        }
                        item {
                            Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Bookmarks", "$bookmarksCount saved", onClick = { onNavigateToBookmarks() })
                            }
                        }
                        item {
                            Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Images", "$imagesCount saved", onClick = { onNavigateToImages() })
                            }
                        }
                        item {
                            Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Documents", "$documentsCount attached", onClick = { onNavigateToDocuments() })
                            }
                        }
                    }

                    if (selectedFolderId == null && favoriteNotes.isNotEmpty() && !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).noRippleClickable { isFavoritesExpanded = !isFavoritesExpanded }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Favorites", fontFamily = PoppinsFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = if (isFavoritesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Toggle Favorites", modifier = Modifier.padding(start = 4.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        if (isFavoritesExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(state = favListState, modifier = Modifier.fillMaxWidth().mouseScrollable(favListState), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)) {
                                    items(favoriteNotes, key = { "fav_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(note = note, isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = { if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId) else onNavigateToEditor(note.noteId) },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (notes.isNotEmpty() || !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).noRippleClickable { isNotesExpanded = !isNotesExpanded }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Notes", fontFamily = PoppinsFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Toggle Notes", modifier = Modifier.padding(start = 4.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                }
                                if (!isSelectionMode) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box {
                                            Icon(painterResource(Res.drawable.arrow_up_down), "Sort", modifier = Modifier.size(20.dp).clip(CircleShape).noRippleClickable { showSortMenu = true }, tint = MaterialTheme.colorScheme.onSurface)
                                            if (isDesktopPlatform) {
                                                InlyDesktopMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                                    DesktopSortMenu(currentSortType = currentSortType, currentSortOrder = currentSortOrder, onDismiss = { showSortMenu = false }, onSortChanged = { type, order -> viewModel.updateSort(type, order); showSortMenu = false })
                                                }
                                            }
                                        }
                                        Box {
                                            Icon(painterResource(Res.drawable.file_plus_corner), "New Note", modifier = Modifier.size(20.dp).clip(CircleShape).noRippleClickable { if (isDesktopPlatform) { addNoteInput = ""; showAddNotePopup = true } else showAddNoteDialog = true }, tint = MaterialTheme.colorScheme.onSurface)
                                            if (isDesktopPlatform) {
                                                InlyDesktopMenu(expanded = showAddNotePopup, onDismissRequest = { showAddNotePopup = false }, modifier = Modifier.width(280.dp)) {
                                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                        Text("New Note", fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
                                                        InlyTextField(value = addNoteInput, onValueChange = { addNoteInput = it }, placeholder = "Note title...", modifier = Modifier.fillMaxWidth())
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            InlyButtonSecondary(text = "Cancel", onClick = { showAddNotePopup = false }, modifier = Modifier.weight(1f))
                                                            InlyButtonPrimary(text = "Create", onClick = { if (addNoteInput.isNotBlank()) { handleCreateNote(addNoteInput.trim()); showAddNotePopup = false } }, modifier = Modifier.weight(1f))
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
                        if (subFolders.isNotEmpty() || !isSelectionMode) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(state = folderListState, contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).mouseScrollable(folderListState)) {
                                    if (!isSelectionMode) {
                                        item {
                                            if (isDesktopPlatform) {
                                                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart).height(36.dp)) {
                                                    FolderPill(name = "New", isSelected = false, isNewButton = true, onClick = { addFolderInput = ""; showAddFolderPopup = true }, onLongClick = {})
                                                    InlyDesktopMenu(expanded = showAddFolderPopup, onDismissRequest = { showAddFolderPopup = false }, modifier = Modifier.width(280.dp)) {
                                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                            Text("New Folder", fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
                                                            InlyTextField(value = addFolderInput, onValueChange = { addFolderInput = it }, placeholder = "e.g. Personal, Work...", modifier = Modifier.fillMaxWidth())
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                InlyButtonSecondary(text = "Cancel", onClick = { showAddFolderPopup = false }, modifier = Modifier.weight(1f))
                                                                InlyButtonPrimary(text = "Create", onClick = { if (addFolderInput.isNotBlank()) { handleCreateFolder(addFolderInput.trim()); showAddFolderPopup = false } }, modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                FolderPill(name = "New", isSelected = false, isNewButton = true, onClick = { showAddFolderDialog = true }, onLongClick = {})
                                            }
                                        }
                                    }
                                    items(subFolders, key = { it.folderId }) { folder ->
                                        FolderPill(
                                            name = folder.name,
                                            isSelected = selectedFolderIds.contains(folder.folderId),
                                            onClick = {
                                                if (isSelectionMode) viewModel.toggleFolderSelection(
                                                    folder.folderId
                                                ) else viewModel.selectFolder(folder.folderId)
                                            },
                                            onLongClick = { viewModel.toggleFolderSelection(folder.folderId) })
                                    }
                                }
                            }
                        }

                        itemsIndexed(notes, key = { _, note -> note.noteId }) { index, note ->
                            val sidePad = if (index % 2 == 0) Modifier.padding(start = HORIZONTAL_PADDING) else Modifier.padding(end = HORIZONTAL_PADDING)
                            Box(modifier = sidePad) {
                                NoteCard(
                                    note = note, isSelected = selectedNoteIds.contains(note.noteId),
                                    onClick = {
                                        if (isSelectionMode) viewModel.toggleNoteSelection(
                                            note.noteId
                                        ) else onNavigateToEditor(note.noteId)
                                    },
                                    onLongClick = { viewModel.toggleNoteSelection(note.noteId) })
                            }
                        }
                    }

                    if (selectedFolderId == null && recentNotes.isNotEmpty() && !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).noRippleClickable { isRecentsExpanded = !isRecentsExpanded }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Recents", fontFamily = PoppinsFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = if (isRecentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Toggle Recents", modifier = Modifier.padding(start = 4.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        if (isRecentsExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(state = recentListState, modifier = Modifier.fillMaxWidth().mouseScrollable(recentListState), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)) {
                                    items(recentNotes, key = { "recent_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(note = note, isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = { if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId) else onNavigateToEditor(note.noteId) },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 16.dp, top = if (isDesktopPlatform) 18.dp else 12.dp)) {
                TopBarIconButton(icon = painterResource(Res.drawable.ellipsis), contentDescription = "Settings", bgColor = Color.Transparent, tint = MaterialTheme.colorScheme.onSurface, hazeState = hazeState, onClick = { showNotesMenu = true })
                UserSettings(
                    expanded = showNotesMenu, onDismiss = { showNotesMenu = false },
                    onNavigateToSettings = { onNavigateToSettings(); showNotesMenu = false },
                    onNavigateToTrash = { onNavigateToTrash(); showNotesMenu = false },
                    onShowPairingCode = { showNotesMenu = false; val currentIp = getLocalNetworkIp(); val newToken = generateSecureToken(); val newEncryptionKey = generateSecureToken() + generateSecureToken(); settingsManager.saveSyncAuthToken(newToken); settingsManager.saveSyncEncryptionKey(newEncryptionKey); activePairingData = SyncPairingData(ipAddress = currentIp, port = 8080, authToken = newToken, encryptionKey = newEncryptionKey); showPairingDialog = true },
                    onScanPairingCode = { showNotesMenu = false; showMobileScannerDialog = true },
                    onSyncNow = { showNotesMenu = false; syncViewModel.triggerManualSync() }
                )
            }
        }
    }

    // Scaffold
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues).haze(state = hazeState)) {

            leftPanelContent()

            NotesSelectionPill(isVisible = isSelectionMode, selectedCount = selectedNoteIds.size + selectedFolderIds.size, onClearSelection = { viewModel.clearSelection() }, onDelete = { viewModel.deleteSelectedItems() }, modifier = Modifier.align(Alignment.BottomCenter))

            if (!isDesktopPlatform) {
                AddFolderBottomSheet(expanded = showAddFolderDialog, onDismiss = { showAddFolderDialog = false }, onCreate = handleCreateFolder)
                AddNoteBottomSheet(expanded = showAddNoteDialog, onDismiss = { showAddNoteDialog = false }, onCreate = handleCreateNote)
                SortBottomSheet(expanded = showSortMenu, currentSortType = currentSortType, currentSortOrder = currentSortOrder, onDismiss = { showSortMenu = false }, onSortChanged = { type, order -> viewModel.updateSort(type, order); showSortMenu = false })
            }

            if (showPairingDialog && activePairingData != null) { SyncPairingDialog(pairingData = activePairingData, onDismiss = { showPairingDialog = false }) }
            if (showMobileScannerDialog) {
                SyncScannerDialog(onDismiss = { showMobileScannerDialog = false }, onScanned = { pairingData ->
                    showMobileScannerDialog = false
                    settingsManager.saveSyncIpAddress(pairingData.ipAddress); settingsManager.saveSyncPort(pairingData.port); settingsManager.saveSyncAuthToken(pairingData.authToken); settingsManager.saveSyncEncryptionKey(pairingData.encryptionKey)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Paired with ${pairingData.ipAddress}!") }
                })
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 66.dp)) { data ->
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = 6.dp, modifier = Modifier.padding(horizontal = 24.dp).wrapContentWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = data.visuals.message, fontFamily = PoppinsFont, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopSortMenu(currentSortType: SortType, currentSortOrder: SortOrder, onDismiss: () -> Unit, onSortChanged: (SortType, SortOrder) -> Unit) {
    Column(modifier = Modifier.width(200.dp).padding(vertical = 4.dp)) {
        DesktopSortOptionItem("Last Edited", currentSortType == SortType.LAST_EDITED) { onDismiss(); onSortChanged(SortType.LAST_EDITED, currentSortOrder) }
        DesktopSortOptionItem("Date Created", currentSortType == SortType.DATE_CREATED) { onDismiss(); onSortChanged(SortType.DATE_CREATED, currentSortOrder) }
        DesktopSortOptionItem("Name (A-Z)", currentSortType == SortType.NAME) { onDismiss(); onSortChanged(SortType.NAME, currentSortOrder) }
        DesktopSortOptionItem("Manual", currentSortType == SortType.MANUAL) {
            onDismiss(); onSortChanged(SortType.MANUAL, currentSortOrder)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        DesktopSortOptionItem("Ascending", currentSortOrder == SortOrder.ASCENDING) { onDismiss(); onSortChanged(currentSortType, SortOrder.ASCENDING) }
        DesktopSortOptionItem("Descending", currentSortOrder == SortOrder.DESCENDING) { onDismiss(); onSortChanged(currentSortType, SortOrder.DESCENDING) }
    }
}

@Composable
private fun DesktopSortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 1.dp).clip(RoundedCornerShape(6.dp)).noRippleClickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, fontFamily = PoppinsFont, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (isSelected) Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun OverviewCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(shape = DefaultCornerShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clip(DefaultCornerShape).noRippleClickable { onClick() }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontFamily = PoppinsFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontFamily = PoppinsFont, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun BreadcrumbTrail(selectedFolderId: String?, breadcrumbs: List<FolderEntity>, onNavigate: (String?) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
        item {
            val isRoot = selectedFolderId == null
            Text("Home", fontFamily = PoppinsFont, fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium, fontSize = 20.sp, color = if (isRoot) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface, modifier = Modifier.noRippleClickable { onNavigate(null) })
        }
        items(breadcrumbs) { folder ->
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.padding(horizontal = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
            val isLast = folder.folderId == selectedFolderId
            Text(folder.name, fontFamily = PoppinsFont, fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium, fontSize = 18.sp, color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface, modifier = Modifier.noRippleClickable { onNavigate(folder.folderId) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderPill(name: String, isSelected: Boolean, isNewButton: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bgColor = when { isNewButton -> MaterialTheme.colorScheme.primary; isSelected -> MaterialTheme.colorScheme.onSurface; isDesktopPlatform -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.surface }
    val textColor = when { isNewButton -> MaterialTheme.colorScheme.onPrimary; isSelected -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.onSurface }
    Surface(shape = RoundedCornerShape(8.dp), color = bgColor, contentColor = textColor, modifier = Modifier.height(36.dp).defaultMinSize(minWidth = 72.dp).clip(RoundedCornerShape(8.dp)).combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick, onLongClick = onLongClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(if (isNewButton) painterResource(Res.drawable.folder_plus) else painterResource(Res.drawable.folder), null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(name, fontFamily = PoppinsFont, fontSize = 14.sp)
            AnimatedVisibility(visible = isSelected && !isNewButton) { Row { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(14.dp)) } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(note: NoteMetadataEntity, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val mediaStorageHelper: com.ben.inly.domain.util.MediaStorageHelper = koinInject()
    val bgColor = when { isSelected -> MaterialTheme.colorScheme.onSurface; isDesktopPlatform -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.surface }
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    val coverHeight = 72.dp; val iconOverhang = 12.dp
    val hasCover = note.coverImagePath != null; val hasIcon = !note.icon.isNullOrEmpty(); val hasHeader = hasCover || hasIcon

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(DefaultCornerShape).background(bgColor).combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick, onLongClick = onLongClick)) {
        Column(Modifier.fillMaxSize()) {
            if (hasHeader) {
                Box(modifier = Modifier.fillMaxWidth().height(coverHeight)) {
                    if (note.coverImagePath != null) {
                        val absolutePath = mediaStorageHelper.getAbsoluteMediaPath(note.coverImagePath)
                        val context = coil3.compose.LocalPlatformContext.current
                        val request = remember(absolutePath) { coil3.request.ImageRequest.Builder(context).data(absolutePath).memoryCacheKey(absolutePath).diskCacheKey(absolutePath).build() }
                        AsyncImage(model = request, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 0.12f else 0.05f)))
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = if (note.isFavorite && !hasHeader) 26.dp else 12.dp, top = if (hasIcon) iconOverhang + 10.dp else 10.dp, bottom = 10.dp)) {
                Text(note.title.ifEmpty { "Untitled" }, fontFamily = PoppinsFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(text = note.snippet.takeIf { it.isNotBlank() } ?: "Empty note...", fontFamily = PoppinsFont, fontSize = 12.sp, color = mutedColor, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
            }
        }
        if (hasIcon) Text(text = note.icon!!, fontSize = 22.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp).offset(y = coverHeight - iconOverhang))
        if (note.isFavorite) Icon(Icons.Default.Star, "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp))
        if (isSelected) Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) }
    }
}

@Composable
fun NotesSelectionPill(isVisible: Boolean, selectedCount: Int, onClearSelection: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val isDark = LocalAppIsDark.current
    val pillColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val tint = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = modifier.padding(horizontal = 24.dp)) {
        Surface(shape = DefaultCornerShape, color = pillColor, modifier = Modifier.padding(bottom = 28.dp).shadow(6.dp, DefaultCornerShape, spotColor = Color.Black.copy(alpha = 0.2f))) {
            val pillScroll = rememberScrollState()
            Row(modifier = Modifier.horizontalScroll(pillScroll).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp).noRippleClickable { onClearSelection() }, tint = tint)
                Text("$selectedCount", fontFamily = PoppinsFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = tint)
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                Icon(Icons.Default.Delete, "Move to Trash", modifier = Modifier.size(18.dp).noRippleClickable { onDelete() }, tint = tint)
            }
        }
    }
}

@Composable
fun AddFolderBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "New Folder", subtitle = "Organize your notes with a new category.") { closeAnd ->
        InlyTextField(value = folderName, onValueChange = { folderName = it }, placeholder = "e.g. Personal, Work...", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InlyButtonSecondary(text = "Cancel", onClick = { closeAnd(onDismiss) }, modifier = Modifier.weight(1f))
            InlyButtonPrimary(text = "Create", onClick = { if (folderName.isNotBlank()) closeAnd { onCreate(folderName.trim()) } }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AddNoteBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var noteTitle by remember { mutableStateOf("") }
    InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "New Note", subtitle = "Give your note a title, or leave it blank.") { closeAnd ->
        InlyTextField(value = noteTitle, onValueChange = { noteTitle = it }, placeholder = "Note title...", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InlyButtonSecondary(text = "Cancel", onClick = { closeAnd(onDismiss) }, modifier = Modifier.weight(1f))
            InlyButtonPrimary(text = "Create", onClick = { closeAnd { onCreate(noteTitle.trim()) } }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SortBottomSheet(expanded: Boolean, currentSortType: SortType, currentSortOrder: SortOrder, onDismiss: () -> Unit, onSortChanged: (SortType, SortOrder) -> Unit) {
    InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Sort By") { closeAnd ->
        SortOptionItem("Last Edited", currentSortType == SortType.LAST_EDITED) { closeAnd { onSortChanged(SortType.LAST_EDITED, currentSortOrder) } }
        SortOptionItem("Date Created", currentSortType == SortType.DATE_CREATED) { closeAnd { onSortChanged(SortType.DATE_CREATED, currentSortOrder) } }
        SortOptionItem("Name (A-Z)", currentSortType == SortType.NAME) { closeAnd { onSortChanged(SortType.NAME, currentSortOrder) } }
        SortOptionItem("Manual", currentSortType == SortType.MANUAL) {
            closeAnd { onSortChanged(SortType.MANUAL, currentSortOrder) }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Text("Order", fontFamily = PoppinsFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        SortOptionItem("Ascending", currentSortOrder == SortOrder.ASCENDING) { closeAnd { onSortChanged(currentSortType, SortOrder.ASCENDING) } }
        SortOptionItem("Descending", currentSortOrder == SortOrder.DESCENDING) { closeAnd { onSortChanged(currentSortType, SortOrder.DESCENDING) } }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().noRippleClickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, fontFamily = PoppinsFont, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (isSelected) Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}