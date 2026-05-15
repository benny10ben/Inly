package com.ben.inly.presentation.daily

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.FilterConfig
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.presentation.shared.editor.AddBlockMenuPill
import com.ben.inly.presentation.shared.editor.BlockSelectionPill
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.EditorScreen
import com.ben.inly.presentation.shared.editor.SelectionModeObserver
import com.ben.inly.ui.theme.BricolageFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import com.ben.inly.presentation.shared.editor.EditorToolbar
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// Change these values here to adjust the corner shapes across the entire Daily Screen
private val CardShape = RoundedCornerShape(6.dp)
private val ButtonShape = RoundedCornerShape(16.dp)

/**
 * The main UI for the Daily Notes feature.
 * Combines the scrolling calendar header, the block editor, and the search overlay.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyScreen(
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onClearSearch: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    viewModel: DailyEditorViewModel = hiltViewModel()
) {
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    val hazeState = remember { HazeState() }

    val searchResults by viewModel.dailySearchResults.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val blocks by viewModel.visibleBlocks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedBlockIds by viewModel.selectedBlockIds.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    val loadedDateString by viewModel.loadedDateString.collectAsState()

    val initialDate = remember { LocalDate.now() }
    val initialPage = remember { Int.MAX_VALUE / 2 }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val isKeyboardOpen = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val showToolbar = !isSelectionMode && isKeyboardOpen && !isSearchActive

    var showAddBlockMenu by remember { mutableStateOf(false) }

    val globalTags by viewModel.globalTags.collectAsState()

    LaunchedEffect(isKeyboardOpen, isSelectionMode, isSearchActive) {
        if (!isKeyboardOpen) showAddBlockMenu = false
        if (isSelectionMode || isSearchActive) showAddBlockMenu = false
    }

    SelectionModeObserver(isSelectionMode || showAddBlockMenu, onSelectionModeChange)

    BackHandler(enabled = isSelectionMode || isKeyboardOpen || isSearchActive || showAddBlockMenu) {
        if (showAddBlockMenu) {
            showAddBlockMenu = false
        } else if (isSelectionMode) {
            viewModel.clearSelection()
        } else if (isKeyboardOpen) {
            keyboardController?.hide()
        } else if (isSearchActive) {
            onClearSearch()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val newDate = initialDate.plusDays((page - initialPage).toLong())
                viewModel.selectDate(newDate)
            }
    }

    LaunchedEffect(selectedDate) {
        val dayOffset = ChronoUnit.DAYS.between(initialDate, selectedDate).toInt()
        val targetPage = initialPage + dayOffset
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            if (abs(pagerState.currentPage - targetPage) > 3) pagerState.scrollToPage(targetPage)
            else pagerState.animateScrollToPage(targetPage)
        }
    }

    // Modal Date Picker for jumping to specific days
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).padding(horizontal = 16.dp)) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.selectDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Date", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .statusBarsPadding()
                    .haze(state = hazeState)
            ) {
                StaticDateHeader(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onCalendarIconClick = { showBottomSheet = true }
                )

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val pageDate = initialDate.plusDays((page - initialPage).toLong())
                        var fetchedBlocks by remember { mutableStateOf<List<NoteBlock>>(emptyList()) }

                        LaunchedEffect(pageDate) {
                            fetchedBlocks = viewModel.fetchBlocksForPreview(pageDate.toString())
                        }

                        val isCurrentActivePage = pageDate == selectedDate && loadedDateString == pageDate.toString()

                        LaunchedEffect(isCurrentActivePage, blocks) {
                            if (isCurrentActivePage) {
                                fetchedBlocks = blocks
                            }
                        }

                        val displayBlocks = if (isCurrentActivePage) blocks else fetchedBlocks
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

                        EditorScreen(
                            blocks = displayBlocks,
                            globalTags = globalTags,
                            actions = editorActions,
                            focusRequest = if (isCurrentActivePage) focusRequest else null,
                            selectedBlockIds = selectedBlockIds,
                            hazeState = hazeState,
                            bottomContentPadding = bottomContentPadding
                        )
                    }

                    // Search Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchQuery.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults, key = { it.noteId }) { meta ->
                                DailySearchResultCard(
                                    note = meta,
                                    onClick = {
                                        meta.dateString?.let { viewModel.selectDate(LocalDate.parse(it)) }
                                        onClearSearch()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Keyboard Toolbar Overlay
            androidx.compose.animation.AnimatedVisibility(
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
                    onChangeBlockType = { viewModel.changeFocusedBlockType(it) },
                    onToggleFormat = { viewModel.toggleFormat(it) },
                    onAdjustIndentation = { viewModel.adjustIndentation(it) },
                    onAddMenuClick = { showAddBlockMenu = !showAddBlockMenu }
                )
            }

            AddBlockMenuPill(
                isVisible = showAddBlockMenu,
                hazeState = hazeState,
                onAddDatabase = {
                    showAddBlockMenu = false
                    viewModel.insertNewMediaBlock("database")
                },
                onAddBookmark = {
                    showAddBlockMenu = false
                    viewModel.insertNewMediaBlock("bookmark")
                },
                onAddImage = {
                    showAddBlockMenu = false
                    viewModel.insertNewMediaBlock("image")
                },
                onAddDocument = {
                    showAddBlockMenu = false
                    viewModel.insertNewMediaBlock("document")
                },
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun DailySearchResultCard(note: NoteMetadataEntity, onClick: () -> Unit) {
    val formattedDate = remember(note.dateString) {
        try {
            val date = LocalDate.parse(note.dateString)
            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        } catch (e: Exception) { note.dateString ?: "Unknown Date" }
    }
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(formattedDate, fontFamily = BricolageFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(note.snippet.ifEmpty { "Empty note..." }, fontFamily = BricolageFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StaticDateHeader(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onCalendarIconClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val day = selectedDate.dayOfMonth
            val suffix = when { day in 11..13 -> "th"; day % 10 == 1 -> "st"; day % 10 == 2 -> "nd"; day % 10 == 3 -> "rd"; else -> "th" }
            val headerText = if (selectedDate == LocalDate.now()) "Today" else "${selectedDate.format(DateTimeFormatter.ofPattern("MMMM"))} $day$suffix"
            Text(headerText, fontFamily = BricolageFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        CalendarStrip(selectedDate = selectedDate, onDateSelected = onDateSelected)
    }
}