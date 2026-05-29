package com.ben.inly.presentation.notes.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.editor.EditorToolbar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import coil3.compose.AsyncImage
import com.ben.inly.presentation.shared.components.KmpBackHandler
import okio.Path.Companion.toPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DefaultCornerShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StandaloneNoteScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    viewModel: StandaloneEditorViewModel = koinViewModel()
) {

    val hazeState = remember { HazeState() }
    val clipboardManager = LocalClipboardManager.current
    val blocks by viewModel.visibleBlocks.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val noteIcon by viewModel.noteIcon.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val coverImagePath by viewModel.coverImagePath.collectAsState()

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    var showIconPicker by remember { mutableStateOf(false) }

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val isKeyboardOpen = WindowInsets.ime.getBottom(
        androidx.compose.ui.platform.LocalDensity.current
    ) > 0

    val showToolbar = !isSelectionMode && (isKeyboardOpen || isDesktopPlatform)
    var showOptionsMenu by remember { mutableStateOf(false) }
    val globalTags by viewModel.globalTags.collectAsState()

    SelectionModeObserver(isSelectionMode, onSelectionModeChange)

    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val isLoading by viewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80
        }
    }

    val scope = rememberCoroutineScope()

    // --- Unified Actions for Both Menus ---
    val handleToggleFavorite: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); viewModel.toggleFavorite() }
    }
    val handleAddIcon: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); showIconPicker = true }
    }
    val handleRemoveIcon: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); viewModel.updateIcon(null) }
    }
    val handleAddCover: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); onPickImage { path -> viewModel.handleCoverImagePicked(path) } }
    }
    val handleRemoveCover: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); viewModel.removeCoverImage() }
    }
    val handleMoveToTrash: () -> Unit = {
        showOptionsMenu = false
        scope.launch { if (!isDesktopPlatform) delay(250); viewModel.moveToTrash { onNavigateBack() } }
    }

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                EditorScreen(
                    blocks = blocks,
                    actions = editorActions,
                    listState = listState,
                    focusRequest = focusRequest,
                    selectedBlockIds = selectedBlockIds,
                    hazeState = hazeState,
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
                    enter = fadeIn(tween(120)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(80)) + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    EditorToolbar(
                        hazeState = hazeState,
                        onChangeBlockType = { editorActions.onChangeBlockType(it) },
                        onToggleFormat = { editorActions.onToggleFormat(it) },
                        onAdjustIndentation = { editorActions.onAdjustIndentation(it) },
                        onInsertMediaBlock = { editorActions.onInsertMediaBlock(it) }
                    )
                }

                NoteTopBar(
                    isScrolled = isScrolled,
                    coverImagePath = coverImagePath,
                    showOptionsMenu = showOptionsMenu,
                    onDismissOptionsMenu = { showOptionsMenu = false },
                    onBackClick = {
                        if (isSelectionMode) viewModel.clearSelection()
                        else onNavigateBack()
                    },
                    onOptionsClick = { showOptionsMenu = true },
                    desktopMenuContent = {
                        NoteOptionsDesktopMenu(
                            isFavorite = isFavorite,
                            hasIcon = noteIcon != null,
                            hasCover = coverImagePath != null,
                            onDismiss = { showOptionsMenu = false },
                            onToggleFavorite = handleToggleFavorite,
                            onAddIcon = handleAddIcon,
                            onRemoveIcon = handleRemoveIcon,
                            onAddCover = handleAddCover,
                            onRemoveCover = handleRemoveCover,
                            onMoveToTrash = handleMoveToTrash
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
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter).imePadding()
                )

                // Render BottomSheet on mobile ONLY
                if (!isDesktopPlatform) {
                    NoteOptionsBottomSheet(
                        expanded = showOptionsMenu,
                        isFavorite = isFavorite,
                        hasIcon = noteIcon != null,
                        hasCover = coverImagePath != null,
                        onDismiss = { showOptionsMenu = false },
                        onToggleFavorite = handleToggleFavorite,
                        onAddIcon = handleAddIcon,
                        onRemoveIcon = handleRemoveIcon,
                        onAddCover = handleAddCover,
                        onRemoveCover = handleRemoveCover,
                        onMoveToTrash = handleMoveToTrash
                    )
                }

                // Render Icon Picker BottomSheet on mobile ONLY
                if (!isDesktopPlatform) {
                    InlyBottomSheet(
                        expanded = showIconPicker,
                        onDismiss = { showIconPicker = false },
                        title = "Choose Icon"
                    ) { closeAnd ->
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val icons = listOf(
                                "😀", "🔥", "🚀", "📚", "📝",
                                "💡", "🎵", "📸", "❤️", "⭐",
                                "🌙", "☕", "🎯", "🧠", "📌"
                            )
                            icons.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 32.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            closeAnd { viewModel.updateIcon(emoji) }
                                        }
                                        .padding(8.dp)
                                )
                            }
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
    val fileStorageManager: com.ben.inly.data.local.file.FileStorageManager = org.koin.compose.koinInject()

    val topPadding by animateDpAsState(
        targetValue = if (noteIcon != null) 48.dp else 16.dp,
        label = "TopPadding"
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.fillMaxWidth()) {

            if (coverImagePath != null || noteIcon != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (coverImagePath != null) {
                        val absolutePath = fileStorageManager.getAbsoluteMediaPath(coverImagePath)
                        AsyncImage(
                            model = java.io.File(absolutePath),
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
                                .padding(start = 16.dp)
                                .graphicsLayer {
                                    translationY = 36.dp.toPx()
                                }
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onIconClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = noteIcon,
                                style = TextStyle(fontSize = 58.sp, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)
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
                DropdownMenu(
                    expanded = showIconPicker,
                    onDismissRequest = onDismissIconPicker,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(280.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Choose Icon",
                            fontFamily = BricolageFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val icons = listOf(
                                "😀", "🔥", "🚀", "📚", "📝",
                                "💡", "🎵", "📸", "❤️", "⭐",
                                "🌙", "☕", "🎯", "🧠", "📌"
                            )
                            icons.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            onIconChange(emoji)
                                            onDismissIconPicker()
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                if (noteTitle.isEmpty()) {
                    Text(
                        text = "Untitled",
                        fontFamily = BricolageFont,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                }
                BasicTextField(
                    value = noteTitle,
                    onValueChange = { onTitleChange(it) },
                    textStyle = TextStyle(
                        fontFamily = BricolageFont,
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

// Top bar (back + options)
@Composable
private fun NoteTopBar(
    isScrolled: Boolean,
    coverImagePath: String?,
    onBackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    showOptionsMenu: Boolean = false,
    onDismissOptionsMenu: () -> Unit = {},
    desktopMenuContent: @Composable () -> Unit = {}
) {
    val iconBgColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.background
        else Color.Black.copy(alpha = 0.15f),
        label = "iconBg"
    )
    val iconTintColor by animateColorAsState(
        targetValue = when {
            isScrolled           -> MaterialTheme.colorScheme.onBackground
            coverImagePath != null -> Color.White
            else                 -> MaterialTheme.colorScheme.onBackground
        },
        label = "iconTint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 14.dp, start = 14.dp, end = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TopBarIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            bgColor = iconBgColor,
            tint = iconTintColor,
            onClick = onBackClick
        )

        Box {
            TopBarIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "Options",
                bgColor = iconBgColor,
                tint = iconTintColor,
                onClick = onOptionsClick
            )

            // Desktop Dropdown Anchor
            if (isDesktopPlatform) {
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = onDismissOptionsMenu,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    desktopMenuContent()
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    bgColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bgColor, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Options menus (BottomSheet & Desktop Popup)
@Composable
fun NoteOptionsDesktopMenu(
    isFavorite: Boolean,
    hasIcon: Boolean,
    hasCover: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddIcon: () -> Unit,
    onRemoveIcon: () -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    Column(modifier = Modifier.width(240.dp).padding(vertical = 4.dp)) {
        if (hasIcon) {
            DesktopMenuItem(Icons.Default.EmojiEmotions, "Remove Icon") { onDismiss(); onRemoveIcon() }
        } else {
            DesktopMenuItem(Icons.Default.EmojiEmotions, "Add Icon") { onDismiss(); onAddIcon() }
        }

        if (hasCover) {
            DesktopMenuItem(Icons.Default.Image, "Change Cover") { onDismiss(); onAddCover() }
            DesktopMenuItem(Icons.Default.Image, "Remove Cover") { onDismiss(); onRemoveCover() }
        } else {
            DesktopMenuItem(Icons.Default.Image, "Add Cover") { onDismiss(); onAddCover() }
        }

        val favIcon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder
        val favText = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
        DesktopMenuItem(favIcon, favText) { onDismiss(); onToggleFavorite() }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        DesktopMenuItem(Icons.Default.Delete, "Move to Trash", isDestructive = true) {
            onDismiss(); onMoveToTrash()
        }
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
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            fontFamily = BricolageFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
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
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddIcon: () -> Unit,
    onRemoveIcon: () -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Note Options"
    ) { closeAnd ->
        if (hasIcon) {
            BottomSheetOptionItem(Icons.Default.EmojiEmotions, "Remove Icon") { closeAnd { onRemoveIcon() } }
        } else {
            BottomSheetOptionItem(Icons.Default.EmojiEmotions, "Add Icon") { closeAnd { onAddIcon() } }
        }

        if (hasCover) {
            BottomSheetOptionItem(Icons.Default.Image, "Change Cover") { closeAnd { onAddCover() } }
            BottomSheetOptionItem(Icons.Default.Image, "Remove Cover") { closeAnd { onRemoveCover() } }
        } else {
            BottomSheetOptionItem(Icons.Default.Image, "Add Cover") { closeAnd { onAddCover() } }
        }

        val favIcon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder
        val favText = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
        BottomSheetOptionItem(favIcon, favText) { closeAnd { onToggleFavorite() } }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        BottomSheetOptionItem(Icons.Default.Delete, "Move to Trash", isDestructive = true) {
            closeAnd { onMoveToTrash() }
        }

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(48.dp),
            shape = DefaultCornerShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                "Close",
                fontFamily = BricolageFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
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
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            fontFamily = BricolageFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}