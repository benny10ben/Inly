package com.ben.inly.presentation.mobile.home.overview.bookmarks

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.components.TopBarIconButton
import com.ben.inly.presentation.shared.components.customInlyShadow
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.FocusRequest
import com.ben.inly.presentation.shared.editor.blockViews.BookmarkBlockView
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

private val InputContainerShape = RoundedCornerShape(12.dp)
private val SelectionHighlightShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookmarksViewModel = koinViewModel()
) {
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val groupedBlocks by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds: Set<String> by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val clipboardManager = LocalClipboardManager.current
    val localFocusManager = LocalFocusManager.current

    val focusRequest: FocusRequest? by viewModel.focusRequest.collectAsState()
    val hazeState = remember { HazeState() }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var activeBlockId by remember { mutableStateOf<String?>(null) }

    var showAddUrlInput by remember { mutableStateOf(false) }
    var newUrlInput by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }

    KmpBackHandler(enabled = showAddUrlInput) {
        showAddUrlInput = false
        newUrlInput = ""
    }

    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllBookmarks()
    }

    LaunchedEffect(showAddUrlInput) {
        if (showAddUrlInput) {
            delay(100)
            try { inputFocusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            localFocusManager.clearFocus()
        }
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let { request ->
            val id = request.id
            activeBlockId = id
            var attempts = 0
            while (focusRequesters[id] == null && attempts < 50) {
                delay(20)
                attempts++
            }
            try { focusRequesters[id]?.requestFocus() } catch (_: Exception) {}
            viewModel.clearFocusRequest()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = if (isDesktopPlatform) 80.dp else 110.dp,
                    bottom = 120.dp
                ),
            ) {
                item {
                    Text(
                        text = "Bookmarks",
                        fontFamily = PoppinsFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (groupedBlocks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No saved links yet.",
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    items(groupedBlocks, key = { it.monthYear }) { group ->
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                            Text(
                                text = group.monthYear,
                                fontFamily = PoppinsFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp)
                                    .padding(bottom = 12.dp)
                            )
                            BookmarkGrid(
                                blocks = group.blocks,
                                selectedBlockIds = selectedBlockIds,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            BookmarksTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                isSelectionMode = isSelectionMode,
                hazeState = hazeState,
                onBackClick = {
                    if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                },
                onAddClick = { showAddUrlInput = true }
            )

            AnimatedVisibility(
                visible = showAddUrlInput,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val defaultBgColor = if (isDesktopPlatform) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                val defaultContentColor = MaterialTheme.colorScheme.onSurface
                val barSize = if (isDesktopPlatform) 46.dp else 52.dp

                Box(
                    modifier = Modifier
                        .then(if (isDesktopPlatform) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth())
                        .imePadding()
                        .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = InputContainerShape,
                        color = defaultBgColor,
                        contentColor = defaultContentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barSize)
                            .customInlyShadow(InputContainerShape)
                            .clip(InputContainerShape)
                            .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(state = hazeState))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Add Link",
                                modifier = Modifier.size(20.dp),
                                tint = defaultContentColor
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                textStyle = TextStyle(
                                    fontFamily = PoppinsFont,
                                    fontSize = 15.sp,
                                    color = defaultContentColor
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(defaultContentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    if (newUrlInput.isNotBlank()) {
                                        viewModel.insertBookmarkWithUrl(newUrlInput.trim())
                                        showAddUrlInput = false
                                        newUrlInput = ""
                                        localFocusManager.clearFocus()
                                    }
                                }),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (newUrlInput.isEmpty()) {
                                            Text(
                                                text = "Paste a link...",
                                                fontFamily = PoppinsFont,
                                                fontSize = 15.sp,
                                                color = defaultContentColor.copy(0.5f)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            IconButton(
                                onClick = {
                                    if (newUrlInput.isNotEmpty()) {
                                        newUrlInput = ""
                                    } else {
                                        showAddUrlInput = false
                                        localFocusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(18.dp),
                                    tint = defaultContentColor.copy(0.6f)
                                )
                            }
                        }
                    }
                }
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
                onAddBlockAbove = {},
                onAddBlockBelow = {},
                onDelete = { viewModel.deleteSelectedBlocks() },
                onTogglePin = {},
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
            )
        }
    }
}

@Composable
fun BookmarkGrid(
    blocks: List<BookmarkBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: BookmarksViewModel
) {
    val columns = if (isDesktopPlatform) 3 else 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val chunkedBlocks = blocks.chunked(columns)

        chunkedBlocks.forEach { rowBlocks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBlocks.forEach { block ->
                    val isSelected = selectedBlockIds.contains(block.id)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        BookmarkBlockView(
                            block = block,
                            inSelectionMode = isSelectionMode,
                            onToggleSelection = { viewModel.toggleSelection(block.id) },
                            onUrlSubmit = { url -> viewModel.handleUrlSubmit(block.id, url) }
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .border(3.dp, MaterialTheme.colorScheme.primary, SelectionHighlightShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            )
                        }
                    }
                }

                val emptySpaces = columns - rowBlocks.size
                repeat(emptySpaces) {
                    Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}

@Composable
private fun BookmarksTopBar(
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    hazeState: HazeState? = null,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val defaultBgColor = if (isDesktopPlatform) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.statusBarsPadding())
            .padding(top = if (isDesktopPlatform) 14.dp else 18.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            bgColor = defaultBgColor,
            tint = defaultContentColor,
            hazeState = hazeState,
            onClick = onBackClick
        )

        if (!isSelectionMode) {
            TopBarIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Bookmark",
                bgColor = defaultBgColor,
                tint = defaultContentColor,
                hazeState = hazeState,
                onClick = onAddClick
            )
        }
    }
}

