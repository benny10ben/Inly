package com.ben.inly.presentation.notes.overview.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.FocusRequest
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * A global task manager screen.
 * It pulls every checkbox block from across the entire app and displays them here in one place.
 * Users can check things off, edit the text, or add new reminders directly to their Inbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onNavigateBack: () -> Unit,
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    viewModel: RemindersViewModel = koinViewModel(),
) {
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val blocks: List<NoteBlock> by viewModel.visibleBlocks.collectAsState()
    val isShowingCompleted: Boolean by viewModel.isShowingCompleted.collectAsState()

    val selectedBlockIds: Set<String> by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val clipboardManager = LocalClipboardManager.current
    val focusRequest: FocusRequest? by viewModel.focusRequest.collectAsState()

    KmpBackHandler(enabled = isSelectionMode || isShowingCompleted) {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else if (isShowingCompleted) {
            viewModel.toggleCompletedView()
        }
    }

    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        viewModel.loadAllTasks()
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .haze(state = hazeState)
                    .padding(top = if (isDesktopPlatform) 80.dp else 110.dp)
            ) {
                Text(
                    text = if (isShowingCompleted) "Completed" else "Reminders",
                    fontFamily = PoppinsFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (blocks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isShowingCompleted) "No completed tasks yet." else "All caught up!",
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val editorActions = remember(viewModel, onOpenFile) {
                            object : EditorActions {
                                override fun onClearFocusRequest() = viewModel.clearFocusRequest()
                                override fun onUpdateText(id: String, text: String) = viewModel.updateBlockText(id, text)
                                override fun onToggleCheckbox(id: String, checked: Boolean) = viewModel.toggleCheckbox(id, checked)
                                override fun onFocusBlock(id: String) = viewModel.setFocusedBlock(id)
                                override fun onEnterPressed(id: String, before: String, after: String) = viewModel.handleEnter(id, before, after)
                                override fun onBackspaceOnEmpty(id: String) = viewModel.handleBackspaceOnEmpty(id)
                                override fun onToggleSelection(id: String) = viewModel.toggleSelection(id)
                                override fun onUpdateReminder(id: String, timestamp: Long?) = viewModel.updateReminder(id, timestamp)
                                override fun onOpenFile(filePath: String, mimeType: String) { onOpenFile(filePath, mimeType) }

                                override fun onChangeBlockType(type: String) {}
                                override fun onToggleFormat(format: String) {}
                                override fun onAdjustIndentation(increase: Boolean) {}
                                override fun onToggleExpand(id: String) {}
                                override fun onUrlSubmit(id: String, url: String) {}
                                override fun onImagePicked(id: String, uri: String) {}
                                override fun onDocumentPicked(id: String, uri: String) {}
                                override fun onAddBlankBlock() {}
                                override fun onInsertMediaBlock(type: String) {}
                                override fun onOutsideTap() {}
                                override fun onUpdateDbTitle(id: String, title: String) {}
                                override fun onAddDbRow(id: String) {}
                                override fun onAddDbColumn(id: String) {}
                                override fun onUpdateDbCell(blockId: String, rowId: String, colId: String, value: String) {}
                                override fun onUpdateDbColumn(blockId: String, colId: String, name: String, type: ColumnType) {}
                                override fun onUpdateDbSort(blockId: String, colId: String, isAscending: Boolean?) {}
                                override fun onAddDbFilter(blockId: String, colId: String, operator: String, value: String) {}
                                override fun onRemoveDbFilter(blockId: String, config: FilterConfig) {}
                                override fun onReorderDbColumns(blockId: String, from: Int, to: Int) {}
                                override fun onUpdateDbFormula(blockId: String, colId: String, expression: String) {}
                                override fun onDeleteDbColumn(blockId: String, colId: String) {}
                                override fun onDeleteDbRow(blockId: String, rowId: String) {}
                                override fun onAddDbRowAt(blockId: String, index: Int) {}
                                override fun onAddDbColumnAt(blockId: String, index: Int) {}
                                override fun onUpdateDbColumnWidth(blockId: String, colId: String, width: Int) {}
                                override fun onVoiceRecorded(id: String, filePath: String, duration: Int) {}
                                override fun onRemoveVoice(id: String) {}
                                override fun onStartRecording() {}
                                override fun onStopRecording(blockId: String, cancel: Boolean) {}
                                override fun onPlayAudio(filePath: String, onComplete: () -> Unit) {}
                                override fun onStopAudio() {}
                                override fun onDeleteImageBlock(id: String) {}
                                override fun onCreateGlobalTag(name: String, colorHex: String): String = ""
                                override fun onRequestImagePicker(blockId: String) {}
                                override fun onRequestDocumentPicker(blockId: String) {}

                                override fun onRequestDbFilePicker(blockId: String, rowId: String, colId: String, isAudio: Boolean) {}
                                override fun onStopDbAudioRecording(blockId: String, rowId: String, colId: String, cancel: Boolean) {}

                                override fun onUndo() {}
                                override fun onRedo() {}
                            }
                        }

                        EditorScreen(
                            blocks = blocks,
                            globalTags = emptyList(),
                            actions = editorActions,
                            focusRequest = focusRequest,
                            selectedBlockIds = selectedBlockIds,
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            RemindersTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                isSelectionMode = isSelectionMode,
                isShowingCompleted = isShowingCompleted,
                onBackClick = {
                    if (isSelectionMode) viewModel.clearSelection()
                    else if (isShowingCompleted) viewModel.toggleCompletedView()
                    else onNavigateBack()
                },
                onToggleCompleted = { viewModel.toggleCompletedView() },
                onAddClick = { viewModel.insertNewReminder() }
            )

            BlockSelectionPill(
                isVisible = isSelectionMode,
                selectedCount = selectedBlockIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAllBlocks() },
                hazeState = hazeState,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(viewModel.getSelectedText()))
                    viewModel.clearSelection()
                },
                onCut = { clipboardManager.setText(AnnotatedString(viewModel.cutSelectedBlocks())) },
                onAddBlockAbove = {},
                onAddBlockBelow = {},
                onDelete = { viewModel.deleteSelectedBlocks() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
            )
        }
    }
}

@Composable
private fun RemindersTopBar(
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    isShowingCompleted: Boolean,
    onBackClick: () -> Unit,
    onToggleCompleted: () -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBgColor, CircleShape)
                        .clip(CircleShape)
                        .clickable { onToggleCompleted() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Completed Tasks",
                        tint = if (isShowingCompleted) MaterialTheme.colorScheme.primary else iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

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
                        contentDescription = "Add Task",
                        tint = iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}