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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.ben.inly.domain.util.AiEventBus
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.tabs.daily.DailyScreen
import com.ben.inly.presentation.navigation.Screen
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.presentation.trash.TrashScreen
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.ben.inly.presentation.splash.LoadingScreen
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import com.ben.inly.domain.repository.EmojiRepository
import com.ben.inly.domain.util.rememberMicrophonePermissionLauncher
import inly.app.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    HomeViewModel: com.ben.inly.presentation.tabs.home.HomeViewModel = koinViewModel(),
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
) {

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val bytes = Res.readBytes("files/data-by-group.json")
                EmojiRepository.initialize(bytes.decodeToString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isVoiceTaskListening by HomeViewModel.isVoiceTaskListening.collectAsState()
    val partialText by HomeViewModel.voiceTaskPartialText.collectAsState()

    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val requestMicPermission = rememberMicrophonePermissionLauncher { isGranted ->
        if (isGranted) {
            HomeViewModel.startVoiceTaskListening()
        } else {}
    }

    var activeTab by remember { mutableStateOf(Screen.Daily.route) }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Daily.route || currentRoute == Screen.Notes.route) {
            activeTab = currentRoute
        }
    }

    // AI chat ViewModel
    val ragViewModel: com.ben.inly.presentation.rag.RagViewModel = koinViewModel()

    var globalSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Controls the AI chat overlay
    var showRagChatOverlay by remember { mutableStateOf(false) }

    var isSelectionActive by remember { mutableStateOf(false) }
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    var showAddNoteDialog by remember { mutableStateOf(false) }

    KmpBackHandler(enabled = isSearchActive) {
        isSearchActive = false
        globalSearchQuery = ""
    }

    val isTopLevelScreen = currentRoute == Screen.Daily.route ||
            currentRoute == Screen.Notes.route ||
            currentRoute == Screen.Editor.route
    val isBottomBarVisible = isTopLevelScreen && !(isKeyboardOpen && !isSearchActive) && !isSelectionActive
    var bottomBarHeightDp by remember { mutableStateOf(0.dp) }

    var isSidebarVisible by remember { mutableStateOf(true) }

    var showAddNotePopup by remember { mutableStateOf(false) }
    var addNoteInput by remember { mutableStateOf("") }

    val openAiChat: () -> Unit = {
        ragViewModel.clearChat()
        AiEventBus.requestImmediateIndex()
        showRagChatOverlay = true
    }

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
            isIndexing = false,
            onSearchIconTap = openAiChat,
            onAiSearchTriggered = { query ->
                globalSearchQuery = query
                showRagChatOverlay = true
            },
            onAddNote = {
                if (isDesktopPlatform) {
                    addNoteInput = ""
                    showAddNotePopup = true
                } else {
                    showAddNoteDialog = true
                }
            },
            onMicClick = {
                if (isVoiceTaskListening) {
                    HomeViewModel.stopVoiceTaskListening()
                } else {
                    HomeViewModel.startVoiceTaskListening(
                        onPermissionNeeded = { requestMicPermission() }
                    )
                }
            },
            desktopAddNotePopupExpanded = showAddNotePopup,
            desktopAddNoteInput = addNoteInput,
            onDesktopAddNoteInputChange = { addNoteInput = it },
            onDesktopAddNotePopupDismiss = { showAddNotePopup = false },
            onDesktopAddNoteConfirm = {
                if (addNoteInput.isNotBlank()) {
                    HomeViewModel.createNewNote(title = addNoteInput.trim(), forceHomeFolder = true) { newNoteId ->
                        navController.navigate(Screen.Editor.createRoute(newNoteId))
                    }
                    showAddNotePopup = false
                }
            }
        )
    }

    val handleCreateNoteAction = { title: String ->
        HomeViewModel.createNewNote(title = title, forceHomeFolder = true) { newNoteId ->
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
                startDestination = if (isDesktopPlatform) Screen.Splash.route else Screen.Daily.route,
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .consumeWindowInsets(innerPadding)
                    .haze(state = hazeState),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                if (isDesktopPlatform) {
                    composable(Screen.Splash.route) {
                        LoadingScreen(
                            onLoadingComplete = {
                                navController.navigate(Screen.Daily.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable(Screen.Daily.route) {
                    DailyScreen(
                        bottomContentPadding = if (isBottomBarVisible && !isDesktopPlatform) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        onClearSearch = { globalSearchQuery = ""; isSearchActive = false },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        onNavigateToEditor = { noteId ->
                            navController.navigate(Screen.Editor.createRoute(noteId))
                        },
                        desktopBottomBar = desktopPanelBottomBar,
                        isSidebarVisible = isSidebarVisible,
                        sidebarWidth = DESKTOP_SIDEBAR_WIDTH,
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                    )
                }

                composable(Screen.Notes.route) {
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.HomeScreen(
                        bottomContentPadding = if (isBottomBarVisible && !isDesktopPlatform) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        onClearSearch = { globalSearchQuery = ""; isSearchActive = false },
                        onNavigateToEditor = { noteId ->
                            navController.navigate(
                                Screen.Editor.createRoute(
                                    noteId
                                )
                            )
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                        onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                        onNavigateToImages = { navController.navigate(Screen.Images.route) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onNavigateToTrash = { navController.navigate("trash_route") },
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        desktopBottomBar = desktopPanelBottomBar,
                        isSidebarVisible = isSidebarVisible,
                        sidebarWidth = DESKTOP_SIDEBAR_WIDTH,
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                        onSidebarWidthChange = { /* no-op */ },
                    )
                }

                composable(
                    route = "trash_route",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    TrashScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Screen.Editor.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.note.NoteScreen(
                        noteId = backStackEntry.savedStateHandle.get<String>("noteId") ?: "",
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditor = { subNoteId ->
                            navController.navigate(Screen.Editor.createRoute(subNoteId))
                        },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile
                    )
                }

                composable(
                    route = Screen.Reminders.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.reminders.RemindersScreen(
                        onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Screen.Bookmarks.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.bookmarks.BookmarksScreen(
                        onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Screen.Images.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    val imagesViewModel: com.ben.inly.presentation.tabs.home.overview.images.ImagesViewModel = koinViewModel()
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.images.ImagesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTriggerImagePicker = {
                            onPickImage { path -> imagesViewModel.createNewImageWithFile(path) }
                        },
                        viewModel = imagesViewModel
                    )
                }

                composable(
                    route = Screen.Documents.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    val documentsViewModel: com.ben.inly.presentation.tabs.home.overview.documents.DocumentsViewModel = koinViewModel()
                    _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.documents.DocumentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTriggerDocumentPicker = {
                            onPickDocument { path ->
                                documentsViewModel.createNewDocumentWithFile(
                                    path
                                )
                            }
                        },
                        onOpenFile = onOpenFile,
                        viewModel = documentsViewModel
                    )
                }
            }

            if (!isDesktopPlatform) {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
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
                        animationSpec = tween(durationMillis = 250, delayMillis = 100, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(durationMillis = 250, delayMillis = 100)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(durationMillis = 200)),
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
                        onSearchIconTap = openAiChat,
                        onAiSearchTriggered = { query ->
                            isSearchActive = false
                            showRagChatOverlay = true
                            ragViewModel.submitQuery(query)
                        },
                        onAddNote = { showAddNoteDialog = true },
                        onMicClick = {
                            if (isVoiceTaskListening) {
                                HomeViewModel.stopVoiceTaskListening()
                            } else {
                                HomeViewModel.startVoiceTaskListening(
                                    onPermissionNeeded = { requestMicPermission() }
                                )
                            }
                        }
                    )
                }

                _root_ide_package_.com.ben.inly.presentation.tabs.home.AddNoteBottomSheet(
                    expanded = showAddNoteDialog,
                    onDismiss = { showAddNoteDialog = false },
                    onCreate = handleCreateNoteAction
                )
            }

            // AI chat overlay
            com.ben.inly.presentation.rag.RagChatOverlay(
                isVisible = showRagChatOverlay,
                onDismiss = {
                    showRagChatOverlay = false
                    ragViewModel.clearChat()
                },
                viewModel = ragViewModel
            )
        }
    }
}

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
    onAiSearchTriggered: (String) -> Unit,
    onSearchIconTap: () -> Unit = {},
    isIndexing: Boolean = false,
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        onAiSearchTriggered(searchQuery)
                                    }
                                ),
                                textStyle = TextStyle(
                                    fontFamily = PoppinsFont,
                                    fontSize = 14.sp,
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
                                                fontFamily = PoppinsFont,
                                                fontSize = 14.sp,
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
                        Surface(
                            shape = CircleShape,
                            color = defaultBgColor,
                            contentColor = defaultContentColor,
                            modifier = Modifier
                                .size(barSize)
                                .customInlyShadow(CircleShape)
                                .clip(CircleShape)
                                .then(if (isDesktopPlatform) Modifier else Modifier.hazeChild(hazeState))
                                .clickable(enabled = !isIndexing) { onSearchIconTap() }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (isIndexing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = defaultContentColor
                                    )
                                } else {
                                    Icon(Icons.Default.Search, "Ask AI", modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = currentRoute != Screen.Editor.route,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(300)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.width(8.dp))

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
                                                        fontFamily = PoppinsFont,
                                                        fontSize = 14.sp,
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
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).width(280.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 12.dp
                                                    )
                                                ) {
                                                    Text(
                                                        "New Note",
                                                        fontFamily = PoppinsFont,
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
                                                                fontFamily = PoppinsFont,
                                                                fontSize = 14.sp
                                                            )
                                                        },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = DefaultCornerShape,
                                                        textStyle = TextStyle(
                                                            fontFamily = PoppinsFont,
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
                                                                .height(46.dp),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                            shape = DefaultCornerShape,
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.surface,
                                                                contentColor = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                        ) {
                                                            Text(
                                                                "Cancel",
                                                                fontFamily = PoppinsFont,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        Button(
                                                            onClick = onDesktopAddNoteConfirm,
                                                            enabled = desktopAddNoteInput.isNotBlank(),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(46.dp),
                                                            shape = DefaultCornerShape,
                                                        ) {
                                                            Text(
                                                                "Create",
                                                                fontFamily = PoppinsFont,
                                                                fontSize = 14.sp
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