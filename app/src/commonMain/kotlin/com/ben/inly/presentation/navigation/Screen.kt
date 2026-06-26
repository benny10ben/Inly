package com.ben.inly.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines all the navigation routes used across the app.
 */
sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {

    // Main bottom-bar tabs
    object Daily : Screen("daily_screen", "Daily", Icons.Default.CalendarToday)
    object Home : Screen("home_screen", "Home", Icons.AutoMirrored.Filled.Notes)

    // Sub-screens for organizing specific types of blocks
    object Reminders : Screen("reminders")
    object Bookmarks : Screen("bookmarks")
    object Images : Screen("images")
    object Documents : Screen("documents")

    object Settings : Screen("settings")

    object Splash : Screen("splash_screen")

    /**
     * Route for opening a specific note.
     */
    object Note : Screen("editor_screen/{noteId}") {
        fun createRoute(noteId: String) = "editor_screen/$noteId"
    }
}