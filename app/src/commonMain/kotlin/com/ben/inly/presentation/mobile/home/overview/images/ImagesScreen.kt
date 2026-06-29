package com.ben.inly.presentation.mobile.home.overview.images

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.blockViews.ImageBlockView
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.ben.inly.presentation.shared.components.TopBarIconButton

private val SelectionHighlightShape = RoundedCornerShape(12.dp)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen(
    onNavigateBack: () -> Unit,
    onTriggerImagePicker: () -> Unit,
    viewModel: ImagesViewModel = koinViewModel()
) {
    val clipboardManager = LocalClipboardManager.current

    val isLoading by viewModel.isLoading.collectAsState()
    val groupedBlocks by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusRequest by viewModel.focusRequest.collectAsState()

    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        viewModel.loadAllImages()
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
                    .haze(state = hazeState)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = if (isDesktopPlatform) 80.dp else 110.dp,
                    bottom = 120.dp
                )
            ) {
                item {
                    Text(
                        text = "Images",
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
                                "No images saved yet.",
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    items(groupedBlocks, key = { it.monthYear }) { group ->
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .padding(bottom = 36.dp)
                        ) {
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

                            ImageGrid(
                                blocks = group.blocks,
                                selectedBlockIds = selectedBlockIds,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            ImagesTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                hazeState = hazeState,
                isSelectionMode = isSelectionMode,
                onBackClick = {
                    if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                },
                onAddClick = onTriggerImagePicker
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
fun ImageGrid(
    blocks: List<ImageBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: ImagesViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        val minItemWidth = 120f
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
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            ImageBlockView(
                                block = block,
                                inSelectionMode = isSelectionMode,
                                onToggleSelection = { viewModel.toggleSelection(block.id) },
                                onRequestPicker = {},
                                onRequestCamera = {},
                                onDelete = { viewModel.deleteImageBlock(block.id) }
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagesTopBar(
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
                contentDescription = "Add Image",
                bgColor = defaultBgColor,
                tint = defaultContentColor,
                hazeState = hazeState,
                onClick = onAddClick
            )
        }
    }
}