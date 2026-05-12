package com.ben.inly.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.ui.theme.BricolageFont
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.layout.ContentScale
import com.ben.inly.presentation.shared.userSettings
import com.ben.inly.ui.theme.LocalAppIsDark
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.material.icons.filled.Sort
import androidx.compose.ui.res.painterResource
import com.ben.inly.R
import com.ben.inly.presentation.shared.components.InlyBottomSheet

private val HORIZONTAL_PADDING = 22.dp

// Adjust this value to change the corner roundness for all cards, pills, and inputs on this screen
private val DefaultCornerShape = RoundedCornerShape(6.dp)

/**
 * The main dashboard for standalone notes.
 * Displays the folder hierarchy, pinned favorites, recent edits, and quick-access categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    searchQuery: String,
    viewModel: NotesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onNavigateToEditor: (String) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToImages: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onNavigateToTrash: () -> Unit,
    onNavigateToDocuments: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val hazeState = remember { HazeState() }

    val isLoading by viewModel.isLoading.collectAsState()
    val subFolders by viewModel.currentSubFolders.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()

    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()

    val remindersCount by viewModel.remindersCount.collectAsState()
    val bookmarksCount by viewModel.bookmarksCount.collectAsState()
    val imagesCount by viewModel.imagesCount.collectAsState()
    val documentsCount by viewModel.documentsCount.collectAsState()

    val currentSortType by viewModel.sortType.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()

    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showNotesMenu by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cardWidth = (screenWidth - (HORIZONTAL_PADDING * 2) - 12.dp) / 2

    val favoriteNotes by viewModel.favoriteNotes.collectAsState()

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChange(isSelectionMode)
    }

    BackHandler(enabled = true) {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else if (selectedFolderId != null) {
            viewModel.navigateUp()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = bottomContentPadding + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize().haze(state = hazeState)
                ) {
                    // BREADCRUMBS & MENU
                    if (!isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = HORIZONTAL_PADDING + 2.dp, end = HORIZONTAL_PADDING - 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BreadcrumbTrail(
                                    selectedFolderId = selectedFolderId,
                                    breadcrumbs = breadcrumbs,
                                    onNavigate = { viewModel.selectFolder(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showNotesMenu = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ellipsis),
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // OVERVIEW SECTION
                    if (selectedFolderId == null && !isSelectionMode && searchQuery.isBlank()) {
                        item {
                            Box(modifier = Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Reminders", "$remindersCount tasks left", onClick = onNavigateToReminders)
                            }
                        }
                        item {
                            Box(modifier = Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Bookmarks", "$bookmarksCount saved", onClick = onNavigateToBookmarks)
                            }
                        }
                        item {
                            Box(modifier = Modifier.padding(start = HORIZONTAL_PADDING)) {
                                OverviewCard("Images", "$imagesCount saved", onClick = onNavigateToImages)
                            }
                        }
                        item {
                            Box(modifier = Modifier.padding(end = HORIZONTAL_PADDING)) {
                                OverviewCard("Documents", "$documentsCount attached", onClick = onNavigateToDocuments)
                            }
                        }
                    }

                    // FAVORITES SECTION
                    if (selectedFolderId == null && favoriteNotes.isNotEmpty() && !isSelectionMode && searchQuery.isBlank()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Favorites",
                                fontFamily = BricolageFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    top = 16.dp, bottom = 8.dp,
                                    start = HORIZONTAL_PADDING + 3.dp
                                )
                            )
                        }

                        item(span = StaggeredGridItemSpan.FullLine) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING)
                            ) {
                                items(favoriteNotes, key = { "fav_${it.noteId}" }) { note ->
                                    val isSelected = selectedNoteIds.contains(note.noteId)
                                    Box(modifier = Modifier.width(cardWidth)) {
                                        NoteCard(
                                            note = note,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
                                                else onNavigateToEditor(note.noteId)
                                            },
                                            onLongClick = { viewModel.toggleNoteSelection(note.noteId) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // NOTES HEADER
                    if (notes.isNotEmpty() || !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = HORIZONTAL_PADDING + 3.dp,
                                        end = HORIZONTAL_PADDING + 3.dp,
                                        top = 16.dp,
                                        bottom = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Notes",
                                    fontFamily = BricolageFont,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!isSelectionMode) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_down_up),
                                            contentDescription = "Sort Notes",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .clickable { showSortBottomSheet = true },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.plus),
                                            contentDescription = "New Note",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .clickable { showAddNoteDialog = true },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // FOLDER PILLS
                    if (subFolders.isNotEmpty() || !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                if (!isSelectionMode) {
                                    item {
                                        FolderPill(
                                            name = "New",
                                            isSelected = false,
                                            isNewButton = true,
                                            onClick = { showAddFolderDialog = true },
                                            onLongClick = {}
                                        )
                                    }
                                }
                                items(subFolders, key = { it.folderId }) { folder ->
                                    val isSelected = selectedFolderIds.contains(folder.folderId)
                                    FolderPill(
                                        name = folder.name,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) viewModel.toggleFolderSelection(folder.folderId)
                                            else viewModel.selectFolder(folder.folderId)
                                        },
                                        onLongClick = { viewModel.toggleFolderSelection(folder.folderId) }
                                    )
                                }
                            }
                        }
                    }

                    // NOTE CARDS
                    itemsIndexed(notes, key = { _, note -> note.noteId }) { index, note ->
                        val isSelected = selectedNoteIds.contains(note.noteId)
                        val cardModifier = if (index % 2 == 0)
                            Modifier.padding(start = HORIZONTAL_PADDING)
                        else
                            Modifier.padding(end = HORIZONTAL_PADDING)

                        Box(modifier = cardModifier) {
                            NoteCard(
                                note = note,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
                                    else onNavigateToEditor(note.noteId)
                                },
                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) }
                            )
                        }
                    }

                    // RECENTS SECTION
                    if (selectedFolderId == null && recentNotes.isNotEmpty() && !isSelectionMode && searchQuery.isBlank()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Recents",
                                fontFamily = BricolageFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    top = 16.dp, bottom = 8.dp,
                                    start = HORIZONTAL_PADDING + 3.dp
                                )
                            )
                        }

                        item(span = StaggeredGridItemSpan.FullLine) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING)
                            ) {
                                items(recentNotes, key = { "recent_${it.noteId}" }) { note ->
                                    val isSelected = selectedNoteIds.contains(note.noteId)
                                    Box(modifier = Modifier.width(cardWidth)) {
                                        NoteCard(
                                            note = note,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (isSelectionMode) viewModel.toggleNoteSelection(note.noteId)
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
            }

            NotesSelectionPill(
                isVisible = isSelectionMode,
                selectedCount = selectedNoteIds.size + selectedFolderIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onDelete = { viewModel.deleteSelectedItems() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            userSettings(
                expanded = showNotesMenu,
                onDismiss = { showNotesMenu = false },
                onNavigateToTrash = onNavigateToTrash,
            )

            AddFolderBottomSheet(
                expanded = showAddFolderDialog,
                onDismiss = { showAddFolderDialog = false },
                onCreate = { name ->
                    viewModel.createNewFolder(name)
                    showAddFolderDialog = false
                }
            )

            AddNoteBottomSheet(
                expanded = showAddNoteDialog,
                onDismiss = { showAddNoteDialog = false },
                onCreate = { title ->
                    viewModel.createNewNote(title = title, forceHomeFolder = false) { newNoteId ->
                        onNavigateToEditor(newNoteId)
                    }
                    showAddNoteDialog = false
                }
            )

            SortBottomSheet(
                expanded = showSortBottomSheet,
                currentSortType = currentSortType,
                currentSortOrder = currentSortOrder,
                onDismiss = { showSortBottomSheet = false },
                onSortChanged = { type, order ->
                    viewModel.updateSort(type, order)
                    showSortBottomSheet = false
                },
            )
        }
    }
}

@Composable
fun OverviewCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = DefaultCornerShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultCornerShape)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontFamily = BricolageFont,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontFamily = BricolageFont,
                fontSize = 13.sp,
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
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        item {
            val isRoot = selectedFolderId == null
            Text(
                text = "Home",
                fontFamily = BricolageFont,
                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                fontSize = 20.sp,
                color = if (isRoot) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onNavigate(null) }
            )
        }
        items(breadcrumbs) { folder ->
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val isLast = folder.folderId == selectedFolderId
            Text(
                text = folder.name,
                fontFamily = BricolageFont,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                fontSize = 18.sp,
                color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onNavigate(folder.folderId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderPill(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isNewButton: Boolean = false
) {
    val bgColor = when {
        isNewButton -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isNewButton -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = DefaultCornerShape,
        color = bgColor,
        contentColor = textColor,
        modifier = Modifier
            .clip(DefaultCornerShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isNewButton) Icons.Default.Add else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = name,
                fontFamily = BricolageFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            AnimatedVisibility(visible = isSelected && !isNewButton) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(14.dp)
                    )
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
    val context = androidx.compose.ui.platform.LocalContext.current

    val bgColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(DefaultCornerShape)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (note.coverImagePath != null) {
                val imageFile = File(context.filesDir, note.coverImagePath!!)
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(imageFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
            }

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!note.icon.isNullOrEmpty()) {
                        Text(
                            text = note.icon!!,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (note.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.snippet.ifEmpty { "Empty note..." },
                    fontFamily = BricolageFont,
                    fontSize = 13.sp,
                    color = mutedColor,
                    maxLines = if (note.coverImagePath != null) 1 else if (!note.icon.isNullOrEmpty()) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
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
                .padding(bottom = 32.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = DefaultCornerShape,
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
        ) {
            val scrollState = rememberScrollState()
            val divider = @Composable {
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                val iconSize = 18.dp

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(iconSize).clickable { onClearSelection() },
                    tint = tint
                )

                Text(
                    text = "$selectedCount",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = tint
                )

                divider()

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Move to Trash",
                    modifier = Modifier.size(iconSize).clickable { onDelete() },
                    tint = tint
                )
            }
        }
    }
}

@Composable
fun AddFolderBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
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
                Text("e.g. Personal, Work...", fontFamily = BricolageFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = BricolageFont, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
            shape = DefaultCornerShape
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        closeAnd { onCreate(folderName.trim()) }
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = DefaultCornerShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Create", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AddNoteBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
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
                    text = "Note title...",
                    fontFamily = BricolageFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
            shape = DefaultCornerShape
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
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
                enabled = true,
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
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Sort By"
    ) { closeAnd ->
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
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Text(
            text = "Order",
            fontFamily = BricolageFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        SortOptionItem("Ascending", currentSortOrder == SortOrder.ASCENDING) {
            closeAnd { onSortChanged(currentSortType, SortOrder.ASCENDING) }
        }
        SortOptionItem("Descending", currentSortOrder == SortOrder.DESCENDING) {
            closeAnd { onSortChanged(currentSortType, SortOrder.DESCENDING) }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontFamily = BricolageFont,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}