package com.ben.inly.presentation.notes.overview.bookmarks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ben.inly.R
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.BookmarkBlockView
import com.ben.inly.presentation.shared.editor.FocusRequest
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.ui.theme.LocalInlyExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlin.math.abs

// Centralized shapes for easy UI tweaking across the screen
private val InputContainerShape = RoundedCornerShape(6.dp)
private val SelectionHighlightShape = RoundedCornerShape(12.dp)

/**
 * The main screen for browsing all saved links and bookmarks.
 * Displays links grouped by month and handles adding new URLs via a sliding input bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val groupedBlocks: List<BookmarkGroup> by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds: Set<String> by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val clipboardManager = LocalClipboardManager.current
    val localFocusManager = LocalFocusManager.current

    val focusRequest: FocusRequest? by viewModel.focusRequest.collectAsState()
    val hazeState = remember { HazeState() }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var activeBlockId by remember { mutableStateOf<String?>(null) }

    // Input Bar State
    var showAddUrlInput by remember { mutableStateOf(false) }
    var newUrlInput by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }

    fun Modifier.softShadow(cornerRadius: Float = 8f) = this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        25f, 0f, 4f,
                        android.graphics.Color.argb(40, 0, 0, 0)
                    )
                }
            }
            canvas.drawRoundRect(0f, 0f, size.width, size.height, cornerRadius, cornerRadius, paint)
        }
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

    BackHandler(enabled = isSelectionMode || showAddUrlInput) {
        if (showAddUrlInput) {
            showAddUrlInput = false
            newUrlInput = ""
        } else if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            onNavigateBack()
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
            Column(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {

                BookmarksTopBar(
                    isSelectionMode = isSelectionMode,
                    onBackClick = {
                        if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                    },
                    onAddClick = { showAddUrlInput = true }
                )

                Text(
                    text = "Bookmarks",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (groupedBlocks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No saved links yet.", fontFamily = BricolageFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            items(groupedBlocks, key = { it.monthYear }) { group ->
                                Column {
                                    Text(
                                        text = group.monthYear,
                                        fontFamily = BricolageFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)
                                    )

                                    CenteredBookmarkCarousel(
                                        blocks = group.blocks,
                                        selectedBlockIds = selectedBlockIds,
                                        isSelectionMode = isSelectionMode,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input overlay for adding a new URL
            AnimatedVisibility(
                visible = showAddUrlInput,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val defaultBgColor = LocalInlyExtendedColors.current.variant1.copy(alpha = 0.45f)
                val defaultContentColor = LocalInlyExtendedColors.current.variant2

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .softShadow(cornerRadius = 24f)
                            .clip(InputContainerShape)
                            .hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = MaterialTheme.colorScheme.background,
                                    tint = HazeTint(defaultBgColor),
                                    blurRadius = 20.dp
                                )
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Add Link", modifier = Modifier.size(22.dp), tint = defaultContentColor)
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 16.sp, color = defaultContentColor),
                                singleLine = true,
                                cursorBrush = SolidColor(defaultContentColor),
                                modifier = Modifier.weight(1f).focusRequester(inputFocusRequester),
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
                                    if (newUrlInput.isEmpty()) {
                                        Text(text = "Paste a link...", fontFamily = BricolageFont, fontSize = 16.sp, color = defaultContentColor.copy(alpha = 0.5f))
                                    }
                                    inner()
                                }
                            )
                            if (newUrlInput.isNotEmpty()) {
                                IconButton(onClick = { newUrlInput = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp), tint = defaultContentColor.copy(alpha = 0.6f))
                                }
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
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    }
}

/**
 * Custom horizontal scroller for bookmarks.
 * Adjusts the fling behavior to allow swiping past multiple items smoothly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenteredBookmarkCarousel(
    blocks: List<BookmarkBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: BookmarksViewModel
) {
    val pagerState = rememberPagerState(pageCount = { blocks.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(10)
        )
    ) { page ->
        val block = blocks[page]
        val isSelected = selectedBlockIds.contains(block.id)

        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val absOffset = abs(pageOffset)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f - absOffset)
                .graphicsLayer {
                    val scale = 1f - (absOffset * 0.15f).coerceAtMost(0.15f)
                    scaleX = scale
                    scaleY = scale

                    val sign = if (pageOffset > 0) 1f else -1f
                    translationX = sign * (absOffset * 40.dp.toPx())

                    alpha = 1f - (absOffset * 0.4f).coerceAtMost(0.4f)
                }
        ) {
            BookmarkBlockView(
                block = block,
                inSelectionMode = isSelectionMode,
                onToggleSelection = { viewModel.toggleSelection(block.id) },
                onSubmit = { url -> viewModel.handleUrlSubmit(block.id, url) }
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(vertical = 4.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, SelectionHighlightShape)
                )
            }
        }
    }
}

@Composable
private fun BookmarksTopBar(
    isSelectionMode: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val iconBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val iconTintColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 18.dp, start = 18.dp, end = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBgColor, CircleShape)
                .clip(CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.chevron_left),
                contentDescription = "Back",
                tint = iconTintColor,
                modifier = Modifier.size(22.dp)
            )
        }

        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBgColor, CircleShape)
                    .clip(CircleShape)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.plus),
                    contentDescription = "Add Bookmark",
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}