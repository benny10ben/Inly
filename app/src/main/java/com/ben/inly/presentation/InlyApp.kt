package com.ben.inly.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ben.inly.presentation.daily.DailyScreen
import com.ben.inly.presentation.navigation.Screen
import com.ben.inly.presentation.notes.NotesScreen
import com.ben.inly.presentation.notes.NotesViewModel
import com.ben.inly.presentation.notes.notes.StandaloneNoteScreen
import com.ben.inly.ui.theme.BricolageFont
import com.ben.inly.R
import com.ben.inly.presentation.notes.overview.bookmarks.BookmarksScreen
import com.ben.inly.presentation.notes.overview.documents.DocumentsScreen
import com.ben.inly.presentation.notes.overview.images.ImagesScreen
import com.ben.inly.presentation.notes.overview.reminders.RemindersScreen
import com.ben.inly.presentation.shared.trash.TrashScreen
import com.ben.inly.ui.theme.LocalInlyExtendedColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlyApp(notesViewModel: NotesViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = androidx.compose.ui.platform.LocalContext.current
    val isVoiceTaskListening by notesViewModel.isVoiceTaskListening.collectAsState()
    val partialText by notesViewModel.voiceTaskPartialText.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                notesViewModel.startVoiceTaskListening(context)
            }
        }
    )

    val hazeState = remember { HazeState() }

    var activeTab by remember { mutableStateOf(Screen.Daily.route) }
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Daily.route || currentRoute == Screen.Notes.route) {
            activeTab = currentRoute
        }
    }

    var globalSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var isSelectionActive by remember { mutableStateOf(false) }
    val isKeyboardOpen = WindowInsets.isImeVisible

    var showAddNoteDialog by remember { mutableStateOf(false) }

    val isTopLevelScreen = currentRoute == Screen.Daily.route || currentRoute == Screen.Notes.route
    val isBottomBarVisible = isTopLevelScreen &&
            !(isKeyboardOpen && !isSearchActive) &&
            !isSelectionActive

    var bottomBarHeightDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

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
                        bottomContentPadding = if (isBottomBarVisible) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        isSearchActive = isSearchActive,
                        onClearSearch = { globalSearchQuery = ""; isSearchActive = false },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive }
                    )
                }
                composable(Screen.Notes.route) {
                    NotesScreen(
                        bottomContentPadding = if (isBottomBarVisible) bottomBarHeightDp else 0.dp,
                        searchQuery = globalSearchQuery,
                        onNavigateToEditor = { noteId -> navController.navigate(Screen.Editor.createRoute(noteId)) },
                        onNavigateBack = { navController.popBackStack() },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive },
                        onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                        onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                        onNavigateToImages = { navController.navigate(Screen.Images.route) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onNavigateToTrash = { navController.navigate("trash_route") }
                    )
                }
                composable("trash_route") { TrashScreen(onNavigateBack = { navController.popBackStack() }) }
                composable(
                    route = Screen.Editor.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { backStackEntry ->
                    StandaloneNoteScreen(
                        noteId = backStackEntry.arguments?.getString("noteId") ?: "",
                        onNavigateBack = { navController.popBackStack() },
                        onSelectionModeChange = { isActive -> isSelectionActive = isActive }
                    )
                }
                composable(Screen.Reminders.route) { RemindersScreen(onNavigateBack = { navController.popBackStack() }) }
                composable(Screen.Bookmarks.route) { BookmarksScreen(onNavigateBack = { navController.popBackStack() }) }
                composable(Screen.Images.route) { ImagesScreen(onNavigateBack = { navController.popBackStack() }) }
                composable(Screen.Documents.route) { DocumentsScreen(onNavigateBack = { navController.popBackStack() }) }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, scrimColor.copy(alpha = 0f), scrimColor.copy(alpha = 0f))))
                    )
                }

                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coords -> bottomBarHeightDp = with(density) { coords.size.height.toDp() } }
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
                            if (isVoiceTaskListening) {
                                notesViewModel.stopVoiceTaskListening()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    notesViewModel.startVoiceTaskListening(context)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    )
                }
            }

            com.ben.inly.presentation.notes.AddNoteBottomSheet(
                expanded = showAddNoteDialog,
                onDismiss = { showAddNoteDialog = false },
                onCreate = { title ->
                    notesViewModel.createNewNote(title = title, forceHomeFolder = true) { newNoteId ->
                        navController.navigate(Screen.Editor.createRoute(newNoteId))
                    }
                    showAddNoteDialog = false
                }
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
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val defaultBgColor = LocalInlyExtendedColors.current.variant1.copy(alpha = 0.45f)
    val defaultContentColor = LocalInlyExtendedColors.current.variant2

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

    if (isSearchActive) {
        BackHandler {
            onSearchActiveChange(false)
            onSearchQueryChange("")
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                val animationSpec = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
                val alphaSpec = tween<Float>(durationMillis = 250)
                (slideInVertically(animationSpec) { it } + fadeIn(alphaSpec)) togetherWith
                        (slideOutVertically(animationSpec) { it } + fadeOut(alphaSpec))
            },
            label = "BottomBarTransition"
        ) { searchActive ->
            if (searchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = defaultBgColor,
                        contentColor = defaultContentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .softShadow(cornerRadius = 24f)
                            .clip(RoundedCornerShape(6.dp))
                            .hazeChild(state = hazeState)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(painter = painterResource(R.drawable.search), contentDescription = "Open Search", modifier = Modifier.size(22.dp), tint = defaultContentColor)
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(fontFamily = BricolageFont, fontSize = 16.sp, color = defaultContentColor),
                                singleLine = true,
                                cursorBrush = SolidColor(defaultContentColor),
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(text = if (activeTab == Screen.Daily.route) "Search daily notes..." else "Search library...", fontFamily = BricolageFont, fontSize = 16.sp, color = defaultContentColor.copy(alpha = 0.5f))
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp), tint = defaultContentColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp, start = 22.dp, end = 22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = defaultBgColor,
                            contentColor = defaultContentColor,
                            modifier = Modifier
                                .size(52.dp)
                                .softShadow(cornerRadius = 100f)
                                .clip(CircleShape)
                                .hazeChild(state = hazeState)
                                .clickable { onSearchActiveChange(true) }
                        ){
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(painter = painterResource(R.drawable.search), contentDescription = "Open Search", modifier = Modifier.size(22.dp), tint = defaultContentColor)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = CircleShape,
                            color = defaultBgColor,
                            modifier = Modifier
                                .weight(1f)
                                .softShadow(cornerRadius = 100f)
                                .clip(CircleShape)
                                .hazeChild(state = hazeState)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomNavItem(
                                    painter = painterResource(R.drawable.calendar_fold),
                                    isSelected = activeTab == Screen.Daily.route,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (currentRoute != Screen.Daily.route) navController.navigate(Screen.Daily.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                )
                                BottomNavItem(
                                    painter = painterResource(R.drawable.house),
                                    isSelected = activeTab == Screen.Notes.route,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (currentRoute != Screen.Notes.route) navController.navigate(Screen.Notes.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.width(52.dp)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isListening || partialText.isNotEmpty(),
                                    enter = fadeIn(tween(200)) + expandHorizontally(expandFrom = Alignment.End, animationSpec = tween(200)),
                                    exit = fadeOut(tween(200)) + shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(200)),
                                    modifier = Modifier
                                        .offset(x = (-14).dp)
                                        .padding(end = 62.dp)
                                        .wrapContentWidth(unbounded = true, align = Alignment.End)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100f),
                                        color = defaultBgColor,
                                        contentColor = defaultContentColor,
                                        modifier = Modifier
                                            .widthIn(max = 240.dp)
                                            .softShadow(cornerRadius = 100f)
                                            .clip(RoundedCornerShape(100f))
                                            .hazeChild(state = hazeState)
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
                                        .size(52.dp)
                                        .softShadow(cornerRadius = 100f)
                                        .clip(CircleShape)
                                        .hazeChild(state = hazeState)
                                        .clickable { onMicClick() }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(painter = painterResource(R.drawable.mic), contentDescription = "Voice Task", modifier = Modifier.size(21.dp))
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = defaultBgColor,
                                contentColor = defaultContentColor,
                                modifier = Modifier
                                    .size(52.dp)
                                    .softShadow(cornerRadius = 100f)
                                    .clip(CircleShape)
                                    .hazeChild(state = hazeState)
                                    .clickable { onAddNote() }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(painter = painterResource(R.drawable.square_pen), contentDescription = null, modifier = Modifier.size(21.dp), tint = defaultContentColor)
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
    painter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent
    val iconColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.6f) else LocalInlyExtendedColors.current.variant2.copy(0.4f)
    val borderColor = if (isSelected) Color.Transparent else Color.Transparent

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CircleShape,
        color = bgColor,
        contentColor = iconColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource, null, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}