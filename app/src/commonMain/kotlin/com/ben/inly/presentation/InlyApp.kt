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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import com.ben.inly.presentation.mobile.daily.DailyScreen
import com.ben.inly.presentation.navigation.Screen
import com.ben.inly.presentation.trash.TrashScreen
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.ben.inly.presentation.splash.LoadingScreen
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.repository.EmojiRepository
import com.ben.inly.domain.util.rememberMicrophonePermissionLauncher
import inly.app.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import com.ben.inly.presentation.mobile.home.HomeScreen
import kotlinx.coroutines.withContext

private val DESKTOP_SIDEBAR_WIDTH = 340.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlyApp(
    HomeViewModel: com.ben.inly.presentation.mobile.home.HomeViewModel = koinViewModel(),
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportBackup: (jsonContent: String) -> Unit = {},
    onImportBackupClick: () -> Unit = {},
    onRequestBackupFolder: () -> Unit,
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> }
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

    val requestMicPermission = rememberMicrophonePermissionLauncher { isGranted ->
        if (isGranted) {
            HomeViewModel.startVoiceTaskListening()
        } else {}
    }

    var activeTab by remember { mutableStateOf(Screen.Daily.route) }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Daily.route || currentRoute == Screen.Home.route) {
            activeTab = currentRoute
        }
    }

    // AI chat ViewModel
    val ragViewModel: com.ben.inly.presentation.rag.RagViewModel = koinViewModel()

    // Controls the AI chat overlay
    var showRagChatOverlay by remember { mutableStateOf(false) }

    var isSelectionActive by remember { mutableStateOf(false) }
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    var showAddNoteDialog by remember { mutableStateOf(false) }

    val isTopLevelScreen = currentRoute == Screen.Daily.route ||
            currentRoute == Screen.Home.route ||
            currentRoute == Screen.Note.route
    val isBottomBarVisible = isTopLevelScreen && !isKeyboardOpen && !isSelectionActive
    var bottomBarHeightDp by remember { mutableStateOf(0.dp) }

    var isSidebarVisible by remember { mutableStateOf(true) }

    val openAiChat: () -> Unit = {
        ragViewModel.clearChat()
        AiEventBus.requestImmediateIndex()
        showRagChatOverlay = true
    }

    val handleCreateNoteAction = { title: String ->
        HomeViewModel.createNewNote(title = title, forceHomeFolder = true) { newNoteId ->
            navController.navigate(Screen.Note.createRoute(newNoteId))
        }
        showAddNoteDialog = false
    }

    if (isDesktopPlatform) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            com.ben.inly.presentation.desktop.DesktopMainScreen(
                isSidebarVisible = isSidebarVisible,
                sidebarWidth = DESKTOP_SIDEBAR_WIDTH,
                onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                onPickImage = onPickImage,
                onTakePhoto = onTakePhoto,
                onPickDocument = onPickDocument,
                onOpenFile = onOpenFile,
                onExportMarkdown = onExportMarkdown,
                onExportPdf = onExportPdf,
                onExportBackup = onExportBackup,
                onImportBackupClick = onImportBackupClick
            )

            com.ben.inly.presentation.rag.RagChatOverlay(
                isVisible = showRagChatOverlay,
                onDismiss = {
                    showRagChatOverlay = false
                    ragViewModel.clearChat()
                },
                viewModel = ragViewModel
            )
        }
        return
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
                        bottomContentPadding = if (isBottomBarVisible) bottomBarHeightDp else 0.dp,
                        showAddNoteDialog = showAddNoteDialog,
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        onNavigateToEditor = { noteId ->
                            navController.navigate(Screen.Note.createRoute(noteId))
                        },
                        isSidebarVisible = isSidebarVisible,
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        bottomContentPadding = if (isBottomBarVisible) bottomBarHeightDp else 0.dp,
                        onNavigateToEditor = { noteId ->
                            navController.navigate(
                                Screen.Note.createRoute(
                                    noteId
                                )
                            )
                        },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                        onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                        onNavigateToImages = { navController.navigate(Screen.Images.route) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onNavigateToTrash = { navController.navigate("trash_route") },
                        onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
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
                    route = Screen.Note.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) { backStackEntry ->
                    _root_ide_package_.com.ben.inly.presentation.mobile.home.note.NoteScreen(
                        noteId = backStackEntry.savedStateHandle.get<String>("noteId") ?: "",
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditor = { subNoteId ->
                            navController.navigate(Screen.Note.createRoute(subNoteId))
                        },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        onExportMarkdown = onExportMarkdown,
                        onExportPdf = onExportPdf
                    )
                }

                composable(
                    route = Screen.Reminders.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.reminders.RemindersScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditor = { noteId -> navController.navigate(Screen.Note.createRoute(noteId)) }
                    )
                }
                composable(
                    route = Screen.Bookmarks.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.bookmarks.BookmarksScreen(
                        onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Screen.Images.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    val imagesViewModel: com.ben.inly.presentation.mobile.home.overview.images.ImagesViewModel = koinViewModel()
                    _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.images.ImagesScreen(
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
                    val documentsViewModel: com.ben.inly.presentation.mobile.home.overview.documents.DocumentsViewModel = koinViewModel()
                    _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.documents.DocumentsScreen(
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

                composable(
                    route = Screen.Settings.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
                ) {
                    com.ben.inly.presentation.settings.SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onExportReady = onExportBackup,
                        onImportClick = onImportBackupClick,
                        onRequestBackupFolder = onRequestBackupFolder
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
                        onAiIconTap = openAiChat,
                        onAddNote = { showAddNoteDialog = true },
                        onMicClick = {
                            if (isVoiceTaskListening) {
                                HomeViewModel.stopVoiceTaskListening()
                            } else {
                                HomeViewModel.startVoiceTaskListening(
                                    onPermissionNeeded = { requestMicPermission() }
                                )
                            }
                        },
                        isListening = isVoiceTaskListening,
                        partialText = partialText
                    )
                }

                _root_ide_package_.com.ben.inly.presentation.mobile.home.AddNoteBottomSheet(
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