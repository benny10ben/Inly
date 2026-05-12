package com.ben.inly.presentation.notes.overview.documents

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ben.inly.R
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.DocumentBlockView
import com.ben.inly.ui.theme.BricolageFont
import androidx.compose.ui.text.AnnotatedString
import com.ben.inly.presentation.shared.editor.FocusRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.abs

// Adjust this to change how rounded the selection border is around documents
private val SelectionHighlightShape = RoundedCornerShape(6.dp)

/**
 * The main screen for browsing all attached documents across the app.
 * Displays files grouped by month and handles selection, addition, and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DocumentsViewModel = hiltViewModel()
) {
    val clipboardManager = LocalClipboardManager.current

    val isLoading by viewModel.isLoading.collectAsState()
    val groupedBlocks by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusRequest by viewModel.focusRequest.collectAsState()

    val hazeState = remember { HazeState() }

    // Opens the native Android file picker for any file type
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.createNewDocumentWithFile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllDocuments()
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let {
            viewModel.clearFocusRequest()
        }
    }

    BackHandler(enabled = true) {
        if (isSelectionMode) {
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

                DocumentsTopBar(
                    isSelectionMode = isSelectionMode,
                    onBackClick = {
                        if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                    },
                    onAddClick = { documentPickerLauncher.launch("*/*") }
                )

                Text(
                    text = "Documents",
                    fontFamily = BricolageFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (groupedBlocks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No documents attached yet.", fontFamily = BricolageFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
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

                                    CenteredDocumentCarousel(
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
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }
    }
}

/**
 * Custom horizontal scroller for documents.
 * Modifies the fling behavior to allow swiping past multiple documents smoothly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenteredDocumentCarousel(
    blocks: List<DocumentBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: DocumentsViewModel
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
            DocumentBlockView(
                block = block,
                inSelectionMode = isSelectionMode,
                onToggleSelection = { viewModel.toggleSelection(block.id) },
                onFilePicked = { uri -> viewModel.handleDocumentPicked(block.id, uri) }
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
private fun DocumentsTopBar(
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
                    contentDescription = "Add Document",
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}