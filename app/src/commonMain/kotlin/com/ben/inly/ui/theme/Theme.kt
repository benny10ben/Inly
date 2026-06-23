package com.ben.inly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ben.inly.domain.util.isDesktopPlatform

// Base color palette

val CharcoalNoir   = Color(0xFF0d0d0d)
val IroncladGrey   = Color(0xFF202020)
val UrbanFog       = Color(0xFF848484)
val MoonlitSilver  = Color(0xFFB3B3B3)
val CloudVeil      = Color(0xFFE6E6E6)

private val LightColorScheme = lightColorScheme(
    primary          = CharcoalNoir,
    onPrimary        = CloudVeil,
    background       = Color.White,
    onBackground     = Color.Black,
    surface          = CloudVeil,
    onSurface        = CharcoalNoir,
    outline          = UrbanFog,
)

private val DarkColorScheme = darkColorScheme(
    primary          = CloudVeil,
    onPrimary        = CharcoalNoir,
    background       = Color.Black,
    onBackground     = Color.White,
    surface          = IroncladGrey,
    onSurface        = CloudVeil,
    outline          = UrbanFog,
)

val LocalAppIsDark = staticCompositionLocalOf { false }
@Composable
fun InlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (darkTheme && isDesktopPlatform) {
        baseColorScheme.copy(background = Color.Black)
    } else {
        baseColorScheme
    }


    SetSystemBars(
        statusBarColor = Color.Transparent,
        darkIcons = !darkTheme
    )

    CompositionLocalProvider(
        LocalAppIsDark provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}