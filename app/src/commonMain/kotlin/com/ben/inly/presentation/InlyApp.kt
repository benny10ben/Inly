package com.ben.inly.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.daily.DailyScreen
import com.ben.inly.presentation.navigation.Screen
import com.ben.inly.presentation.notes.NotesScreen
import com.ben.inly.presentation.notes.NotesViewModel
import com.ben.inly.presentation.notes.AddNoteBottomSheet
import com.ben.inly.presentation.notes.notes.StandaloneNoteScreen
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.presentation.notes.overview.bookmarks.BookmarksScreen
import com.ben.inly.presentation.notes.overview.documents.DocumentsScreen
import com.ben.inly.presentation.notes.overview.documents.DocumentsViewModel
import com.ben.inly.presentation.notes.overview.images.ImagesScreen
import com.ben.inly.presentation.notes.overview.images.ImagesViewModel
import com.ben.inly.presentation.notes.overview.reminders.RemindersScreen
import com.ben.inly.presentation.shared.trash.TrashScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val DESKTOP_SIDEBAR_WIDTH = 340.dp

private fun Modifier.customInlyShadow(shape: Shape): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.35f),
    ambientColor = Color.Black.copy(alpha = 0.20f)
)

private val DefaultCornerShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlyApp(
    notesViewModel: NotesViewModel = koinViewModel(),
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isVoiceTaskListening by notesViewModel.isVoiceTaskListening.collectAsState()
    val partialText by notesViewModel.voiceTaskPartialText.collectAsState()

    val hazeState = remember { HazeState() }
    val density = LocalDensity.current

    var activeTab by remember { mutableStateOf(Screen.Daily.route) }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Daily.route || currentRoute == Screen.Notes.route) {
            activeTab = currentRoute
        }
    }

    var globalSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSelectionActive by remember { mutableStateOf(false) }
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // Mobile-only bottom sheet flag
    var showAddNoteDialog by remember { mutableStateOf(false) }

    KmpBackHandler(enabled = isSearchActive) {
        isSearchActive = false
        globalSearchQuery = ""
    }

    val isTopLevelScreen = currentRoute == Screen.Daily.route || currentRoute == Screen.Notes.route
    val isBottomBarVisible = isTopLevelScreen && !(isKeyboardOpen && !isSearchActive) && !isSelectionActive
    var bottomBarHeightDp by remember { mutableStateOf(0.dp) }

    var isSidebarVisible by remember { mutableStateOf(true) }

    // Desktop: popup state lives here so InlyBottomBar can own the anchor Box
    var showAddNotePopup by remember { mutableStateOf(false) }
    var addNoteInput by remember { mutableStateOf("") }

    val desktopPanelBottomBar = @Composable {
        InlyBottomBar(
            navController = navController,
            hazeState = hazeState,
            currentRoute = currentRoute,
            activeTab = activeTab,
            searchQuery = globalSearchQuery,
            isSearchActive = isSearchActive,
            isListening = isVoiceTaskListening,
            partialText = partialText,
            onSearchQueryChange = { globalSearchQuery = it },
            onSearchActiveChange = { isSearchActive = it },
            onAddNote = {
                if (isDesktopPlatform) {
                    addNoteInput = ""
                    showAddNotePopup = true
                } else {
                    showAddNoteDialog = true
                }
            },
            onMicClick = {
                if (isVoiceTaskListening) notesViewModel.stopVoiceTaskListening()
                else notesViewModel.startVoiceTaskListening()
            },
            desktopAddNotePopupExpanded = showAddNotePopup,
            desktopAddNoteInput = addNoteInput,
            onDesktopAddNoteInputChange = { addNoteInput = it },
            onDesktopAddNotePopupDismiss = { showAddNotePopup = false },
            onDesktopAddNoteConfirm = {
                if (addNoteInput.isNotBlank()) {
                    notesViewModel.createNewNote(title = addNoteInput.trim(), forceHomeFolder = true) { newNoteId ->
                        navController.navigate(Screen.Editor.createRoute(newNoteId))
                    }
                    showAddNotePopup = false
                }
            }
        )
    }

    val handleCreateNoteAction = { title: String ->
        notesViewModel.createNewNote(title = title, forceHomeFolder = true) { newNoteId ->
            navController.navigate(Screen.Editor.createRoute(newNoteId))
        }
        showAddNoteDialog = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Daily.route,
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .consumeWindowInsets(innerPadding)
                    .haze(state = hazeState),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Daily.route) {
                    DailyScreen(
                        bottomContentPadding = if (isBottomBarVisible && !isDesktopPlatform) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        onClearSearch = { globalSearchQuery = ""; isSearchActive = false },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        desktopBottomBar = desktopPanelBottomBar,
                        isSidebarVisible = isSidebarVisible,
                        sidebarWidth = DESKTOP_SIDEBAR_WIDTH,
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                        onSidebarWidthChange = { /* no-op */ }
                    )
                }

                composable(Screen.Notes.route) {
                    NotesScreen(
                        bottomContentPadding = if (isBottomBarVisible && !isDesktopPlatform) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        onClearSearch = { globalSearchQuery = ""; isSearchActive = false },
                        onNavigateToEditor = { noteId -> navController.navigate(Screen.Editor.createRoute(noteId)) },
                        onNavigateBack = { navController.popBackStack() },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                        onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                        onNavigateToImages = { navController.navigate(Screen.Images.route) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onNavigateToTrash = { navController.navigate("trash_route") },
                        onPickImage = onPickImage,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        desktopBottomBar = desktopPanelBottomBar,
                        isSidebarVisible = isSidebarVisible,
                        sidebarWidth = DESKTOP_SIDEBAR_WIDTH,
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                        onSidebarWidthChange = { /* no-op */ }
                    )
                }

                composable("trash_route") {
                    TrashScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Screen.Editor.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { backStackEntry ->
                    StandaloneNoteScreen(
                        noteId = backStackEntry.savedStateHandle.get<String>("noteId") ?: "",
                        onNavigateBack = { navController.popBackStack() },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile
                    )
                }

                composable(Screen.Reminders.route) {
                    RemindersScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.Bookmarks.route) {
                    BookmarksScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.Images.route) {
                    val imagesViewModel: ImagesViewModel = koinViewModel()
                    ImagesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTriggerImagePicker = {
                            onPickImage { path -> imagesViewModel.createNewImageWithFile(path) }
                        },
                        viewModel = imagesViewModel
                    )
                }

                composable(Screen.Documents.route) {
                    val documentsViewModel: DocumentsViewModel = koinViewModel()
                    DocumentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTriggerDocumentPicker = {
                            onPickDocument { path -> documentsViewModel.createNewDocumentWithFile(path) }
                        },
                        onOpenFile = onOpenFile,
                        viewModel = documentsViewModel
                    )
                }
            }

            // --- MOBILE BOTTOM BAR ---
            if (!isDesktopPlatform) {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val scrimColor = MaterialTheme.colorScheme.background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(300)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coords ->
                            bottomBarHeightDp = with(density) { coords.size.height.toDp() }
                        }
                ) {
                    InlyBottomBar(
                        navController = navController,
                        hazeState = hazeState,
                        currentRoute = currentRoute,
                        activeTab = activeTab,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        isListening = isVoiceTaskListening,
                        partialText = partialText,
                        onSearchQueryChange = { globalSearchQuery = it },
                        onSearchActiveChange = { isSearchActive = it },
                        onAddNote = { showAddNoteDialog = true },
                        onMicClick = {
                            if (isVoiceTaskListening) notesViewModel.stopVoiceTaskListening()
                            else notesViewModel.startVoiceTaskListening()
                        }
                    )
                }

                // Mobile-only bottom sheet
                AddNoteBottomSheet(
                    expanded = showAddNoteDialog,
                    onDismiss = { showAddNoteDialog = false },
                    onCreate = handleCreateNoteAction
                )
            }
        }
    }
}

// Bottom bar shared component (Mobile overlay & Desktop Left Panel layout)
@Composable
fun InlyBottomBar(
    navController: NavHostController,
    hazeState: HazeState,
    currentRoute: String?,
    activeTab: String,
    searchQuery: String,
    isSearchActive: Boolean,
    isListening: Boolean,
    partialText: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onAddNote: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    desktopAddNotePopupExpanded: Boolean = false,
    desktopAddNoteInput: String = "",
    onDesktopAddNoteInputChange: (String) -> Unit = {},
    onDesktopAddNotePopupDismiss: () -> Unit = {},
    onDesktopAddNoteConfirm: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val defaultBgColor = if (isDesktopPlatform) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface

    val barSize = if (isDesktopPlatform) 46.dp else 52.dp
    val navItemHeight = if (isDesktopPlatform) 34.dp else 40.dp
    val searchShape = RoundedCornerShape(12.dp)

    LaunchedEffect(currentRoute) {
        onSearchActiveChange(false)
        onSearchQueryChange("")
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            focusManager.clearFocus()
        }
    }

    val desktopOverlayModifier = if (isDesktopPlatform) {
        Modifier.height(0.dp).wrapContentHeight(Alignment.Bottom, unbounded = true)
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(desktopOverlayModifier),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                (slideInVertically(tween(350)) { it } + fadeIn(tween(250))) togetherWith
                        (slideOutVertically(tween(350)) { it } + fadeOut(tween(250)))
            },
            label = "BottomBarTransition"
        ) { searchActive ->
            if (searchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = searchShape,
                        color = defaultBgColor,
                        contentColor = defaultContentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barSize)
                            .customInlyShadow(searchShape)
                            .clip(searchShape)
                            .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(hazeState))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = defaultContentColor
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    fontFamily = BricolageFont,
                                    fontSize = 15.sp,
                                    color = defaultContentColor
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(defaultContentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search...",
                                                fontFamily = BricolageFont,
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
                                    onSearchQueryChange("")
                                    onSearchActiveChange(false)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close Search",
                                    modifier = Modifier.size(18.dp),
                                    tint = defaultContentColor.copy(0.6f)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                        .padding(bottom = 6.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        // Search button
                        Surface(
                            shape = CircleShape,
                            color = defaultBgColor,
                            contentColor = defaultContentColor,
                            modifier = Modifier
                                .size(barSize)
                                .customInlyShadow(CircleShape)
                                .clip(CircleShape)
                                .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(hazeState))
                                .clickable { onSearchActiveChange(true) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Search, "Search", modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Nav pill
                        Surface(
                            shape = CircleShape,
                            color = defaultBgColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(barSize)
                                .customInlyShadow(CircleShape)
                                .clip(CircleShape)
                                .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(hazeState))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomNavItem(
                                    icon = Icons.Default.CalendarMonth,
                                    isSelected = activeTab == Screen.Daily.route,
                                    modifier = Modifier.weight(1f).height(navItemHeight)
                                ) {
                                    if (currentRoute != Screen.Daily.route) navController.navigate(Screen.Daily.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                BottomNavItem(
                                    icon = Icons.Default.Home,
                                    isSelected = activeTab == Screen.Notes.route,
                                    modifier = Modifier.weight(1f).height(navItemHeight)
                                ) {
                                    if (currentRoute != Screen.Notes.route) navController.navigate(Screen.Notes.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mic — mobile only
                            if (!isDesktopPlatform) {
                                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(barSize)) {
                                    this@Column.AnimatedVisibility(
                                        visible = isListening || partialText.isNotEmpty(),
                                        enter = fadeIn(tween(200)) + expandHorizontally(
                                            expandFrom = Alignment.End,
                                            animationSpec = tween(200)
                                        ),
                                        exit = fadeOut(tween(200)) + shrinkHorizontally(
                                            shrinkTowards = Alignment.End,
                                            animationSpec = tween(200)
                                        ),
                                        modifier = Modifier
                                            .offset(x = (-12).dp)
                                            .padding(end = (barSize + 10.dp))
                                            .wrapContentWidth(unbounded = true, align = Alignment.End)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(100f),
                                            color = defaultBgColor,
                                            contentColor = defaultContentColor,
                                            modifier = Modifier
                                                .widthIn(max = 240.dp)
                                                .customInlyShadow(RoundedCornerShape(100f))
                                                .clip(RoundedCornerShape(100f))
                                                .hazeChild(hazeState)
                                        ) {
                                            Text(
                                                text = partialText.ifBlank { "Listening..." },
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                fontFamily = BricolageFont,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isListening) defaultContentColor else defaultBgColor,
                                        contentColor = if (isListening) MaterialTheme.colorScheme.background else defaultContentColor,
                                        modifier = Modifier
                                            .size(barSize)
                                            .customInlyShadow(CircleShape)
                                            .clip(CircleShape)
                                            .hazeChild(hazeState)
                                            .clickable { onMicClick() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.Mic, "Mic", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }

                            // ── Add Note button — anchored popup on desktop ──
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    shape = CircleShape,
                                    color = defaultBgColor,
                                    contentColor = defaultContentColor,
                                    modifier = Modifier
                                        .size(barSize)
                                        .customInlyShadow(CircleShape)
                                        .clip(CircleShape)
                                        .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(hazeState))
                                        .clickable { onAddNote() }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Edit, "Add", modifier = Modifier.size(20.dp))
                                    }
                                }

                                if (isDesktopPlatform) {
                                    DropdownMenu(
                                        expanded = desktopAddNotePopupExpanded,
                                        onDismissRequest = onDesktopAddNotePopupDismiss,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                            .width(280.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 12.dp
                                            )
                                        ) {
                                            Text(
                                                "New Note",
                                                fontFamily = BricolageFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                            OutlinedTextField(
                                                value = desktopAddNoteInput,
                                                onValueChange = onDesktopAddNoteInputChange,
                                                placeholder = {
                                                    Text(
                                                        "Note title...",
                                                        fontFamily = BricolageFont,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = DefaultCornerShape,
                                                textStyle = TextStyle(
                                                    fontFamily = BricolageFont,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = onDesktopAddNotePopupDismiss,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp),
                                                    shape = DefaultCornerShape,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                                ) {
                                                    Text(
                                                        "Cancel",
                                                        fontFamily = BricolageFont,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Button(
                                                    onClick = onDesktopAddNoteConfirm,
                                                    enabled = desktopAddNoteInput.isNotBlank(),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp),
                                                    shape = DefaultCornerShape,
                                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                                ) {
                                                    Text(
                                                        "Create",
                                                        fontFamily = BricolageFont,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected && isDesktopPlatform -> MaterialTheme.colorScheme.surface
        isSelected -> MaterialTheme.colorScheme.background
        else -> Color.Transparent
    }
    val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.6f)

    Surface(
        shape = CircleShape,
        color = bgColor,
        contentColor = iconColor,
        border = BorderStroke(1.dp, Color.Transparent),
        modifier = modifier
            .clip(CircleShape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
    }
}