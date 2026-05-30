package com.ben.inly.presentation.notes.overview.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.DocumentBlockView
import androidx.compose.ui.text.AnnotatedString
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private val SelectionHighlightShape = RoundedCornerShape(12.dp)

/**
 * The shared multiplatform screen for browsing all attached documents across the app.
 * Displays files grouped by month and handles selection, addition, and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateBack: () -> Unit,
    onTriggerDocumentPicker: () -> Unit,
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    viewModel: DocumentsViewModel = koinViewModel()
) {
    val clipboardManager = LocalClipboardManager.current

    val isLoading by viewModel.isLoading.collectAsState()
    val groupedBlocks by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusRequest by viewModel.focusRequest.collectAsState()

    KmpBackHandler(enabled = true) {
        if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
    }

    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        viewModel.loadAllDocuments()
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let {
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
                    .background(MaterialTheme.colorScheme.background)
                    .haze(state = hazeState),
                contentPadding = PaddingValues(
                    top = if (isDesktopPlatform) 80.dp else 110.dp,
                    bottom = 120.dp
                ),
            ) {
                item {
                    Text(
                        text = "Documents",
                        fontFamily = PoppinsFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
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
                                "No documents attached yet.",
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp)
                            )

                            DocumentGrid(
                                blocks = group.blocks,
                                selectedBlockIds = selectedBlockIds,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel,
                                onOpenFile = onOpenFile
                            )
                        }
                    }
                }
            }

            DocumentsTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                isSelectionMode = isSelectionMode,
                onBackClick = {
                    if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                },
                onAddClick = onTriggerDocumentPicker
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
                onAddBlockAbove = {},
                onAddBlockBelow = {},
                onDelete = { viewModel.deleteSelectedBlocks() },
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
fun DocumentGrid(
    blocks: List<DocumentBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: DocumentsViewModel,
    onOpenFile: (filePath: String, mimeType: String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        val minItemWidth = if (isDesktopPlatform) 280f else 150f
        val spacing = 12f
        val columns = maxOf(2, ((maxWidth.value + spacing) / (minItemWidth + spacing)).toInt())

        Column(
            modifier = Modifier.fillMaxWidth(),
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
                            DocumentBlockView(
                                block = block,
                                inSelectionMode = isSelectionMode,
                                onToggleSelection = { viewModel.toggleSelection(block.id) },
                                onRequestPicker = {},
                                onOpenFile = onOpenFile
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
}

@Composable
private fun DocumentsTopBar(
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val iconBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val iconTintColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.statusBarsPadding())
            .padding(top = if (isDesktopPlatform) 14.dp else 18.dp, start = 16.dp, end = 16.dp),
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Document",
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}