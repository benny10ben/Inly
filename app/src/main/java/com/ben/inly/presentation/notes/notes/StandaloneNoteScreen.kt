package com.ben.inly.presentation.notes.notes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ben.inly.R
import com.ben.inly.presentation.shared.editor.AddBlockMenuPill
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.SelectionModeObserver
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.editor.EditorToolbar
import java.io.File
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

// Adjust this value to change the corner roundness for buttons and components on this screen
private val DefaultCornerShape = RoundedCornerShape(6.dp)

/**
 * The main screen for viewing and editing a single standalone note.
 * Handles the dynamic header (cover image, icon, title) and connects to the core block editor.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StandaloneNoteScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    viewModel: StandaloneEditorViewModel = hiltViewModel()
) {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val blocks by viewModel.visibleBlocks.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val noteIcon by viewModel.noteIcon.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val coverImagePath by viewModel.coverImagePath.collectAsState()

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val isKeyboardOpen = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val showToolbar = !isSelectionMode && isKeyboardOpen

    var showAddBlockMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val globalTags by viewModel.globalTags.collectAsState()

    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.handleCoverImagePicked(it) } }

    LaunchedEffect(isKeyboardOpen, isSelectionMode) {
        if (!isKeyboardOpen) showAddBlockMenu = false
        if (isSelectionMode) showAddBlockMenu = false
    }

    SelectionModeObserver(isSelectionMode || showAddBlockMenu, onSelectionModeChange)

    BackHandler(enabled = true) {
        when {
            showOptionsMenu -> showOptionsMenu = false
            showAddBlockMenu -> showAddBlockMenu = false
            isSelectionMode -> viewModel.clearSelection()
            isKeyboardOpen -> keyboardController?.hide()
            else -> onNavigateBack()
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80
        }
    }

    val editorActions = remember(viewModel) {
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
            override fun onImagePicked(id: String, uri: Uri) = viewModel.handleImagePicked(id, uri)
            override fun onDocumentPicked(id: String, uri: Uri) = viewModel.handleDocumentPicked(id, uri)
            override fun onAddBlankBlock() = viewModel.addBlankBlockBelowFocused()

            override fun onAddMenuClick() { showAddBlockMenu = !showAddBlockMenu }
            override fun onOutsideTap() { showAddBlockMenu = false }

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

            override fun onCreateGlobalTag(name: String, colorHex: String): String {
                return viewModel.createGlobalTag(name, colorHex)
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (coverImagePath != null) {
                                    val imageFile = File(context.filesDir, coverImagePath!!)
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageFile)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .height(if (noteIcon != null) 120.dp else 56.dp)
                                    )
                                }

                                if (noteIcon != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 24.dp)
                                            .offset(y = 32.dp)
                                            .size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = noteIcon ?: "",
                                            onValueChange = { newValue ->
                                                viewModel.updateIcon(
                                                    if (newValue.isNotEmpty()) newValue.take(2) else ""
                                                )
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 58.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxSize(),
                                            decorationBox = { inner ->
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) { inner() }
                                            }
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Spacer(
                                    modifier = Modifier.height(
                                        if (noteIcon != null) 48.dp else 16.dp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
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
                                        onValueChange = { viewModel.updateTitle(it) },
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
                        onAddMenuClick = { editorActions.onAddMenuClick() }
                    )
                }

                AnimatedTopBar(
                    isScrolled = isScrolled,
                    coverImagePath = coverImagePath,
                    onBackClick = {
                        if (isSelectionMode) viewModel.clearSelection()
                        else onNavigateBack()
                    },
                    onOptionsClick = { showOptionsMenu = true }
                )

                AddBlockMenuPill(
                    isVisible = showAddBlockMenu,
                    hazeState = hazeState,
                    onAddDatabase = { showAddBlockMenu = false; viewModel.insertNewMediaBlock("database") },
                    onAddBookmark = { showAddBlockMenu = false; viewModel.insertNewMediaBlock("bookmark") },
                    onAddImage = { showAddBlockMenu = false; viewModel.insertNewMediaBlock("image") },
                    onAddDocument = { showAddBlockMenu = false; viewModel.insertNewMediaBlock("document") },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
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
                )

                NoteOptionsBottomSheet(
                    expanded = showOptionsMenu,
                    isFavorite = isFavorite,
                    hasIcon = noteIcon != null,
                    hasCover = coverImagePath != null,
                    onDismiss = { showOptionsMenu = false },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onAddIcon = { viewModel.updateIcon("😀") },
                    onRemoveIcon = { viewModel.updateIcon(null) },
                    onAddCover = { coverImagePicker.launch("image/*") },
                    onRemoveCover = { viewModel.removeCoverImage() },
                    onMoveToTrash = { viewModel.moveToTrash { onNavigateBack() } },
                )
            }
        }
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
private fun BottomSheetOptionItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontFamily = BricolageFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun AnimatedTopBar(
    isScrolled: Boolean,
    coverImagePath: String?,
    onBackClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val iconBgColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.background else Color.Black.copy(alpha = 0.15f),
        label = "iconBg"
    )
    val iconTintColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.onBackground else if (coverImagePath != null) Color.White else MaterialTheme.colorScheme.onBackground,
        label = "iconTint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 18.dp, start = 18.dp, end = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(iconBgColor, CircleShape).clip(CircleShape).clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(R.drawable.chevron_left), "Back", tint = iconTintColor, modifier = Modifier.size(22.dp))
        }

        Box(
            modifier = Modifier.size(44.dp).background(iconBgColor, CircleShape).clip(CircleShape).clickable { onOptionsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(R.drawable.ellipsis), "Options", tint = iconTintColor, modifier = Modifier.size(22.dp))
        }
    }
}