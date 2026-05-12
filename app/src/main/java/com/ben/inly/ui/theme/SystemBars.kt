package com.ben.inly.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Handles the appearance of the Android system bars (status and navigation bars).
 * This ensures the app content can draw behind the bars, and flips the system icons
 * between dark and light so they stay visible depending on the current theme.
 */
@Composable
fun SetSystemBars(
    statusBarColor: Color,
    darkIcons: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as Activity

    SideEffect {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = darkIcons
    }
}