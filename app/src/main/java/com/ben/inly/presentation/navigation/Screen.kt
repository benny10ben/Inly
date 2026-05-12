package com.ben.inly.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines all the navigation routes used across the app.
 * Keeping them centralized in a sealed class prevents typos when jumping between screens.
 */
sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {

    // Main bottom-bar tabs
    object Daily : Screen("daily_screen", "Daily", Icons.Default.CalendarToday)
    object Notes : Screen("notes_screen", "Notes", Icons.AutoMirrored.Filled.Notes)

    // Sub-screens for organizing specific types of blocks
    object Reminders : Screen("reminders")
    object Bookmarks : Screen("bookmarks")
    object Images : Screen("images")
    object Documents : Screen("documents")

    /**
     * Route for opening a specific standalone note.
     * The helper function makes it easy to pass the note ID without messing up the string format.
     */
    object Editor : Screen("editor_screen/{noteId}") {
        fun createRoute(noteId: String) = "editor_screen/$noteId"
    }
}