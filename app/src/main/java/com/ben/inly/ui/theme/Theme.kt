package com.ben.inly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base color palette
val CharcoalNoir   = Color(0xFF131313)
val IroncladGrey   = Color(0xFF262626)
val IroncladGrey2  = Color(0xFF333333)
val UrbanFog       = Color(0xFF848484)
val MoonlitSilver  = Color(0xFFB3B3B3)
val CloudVeil      = Color(0xFFE0E0E0)
val Fog            = Color(0xFFCCCCCC)

/**
 * Sometimes the standard Material 3 color slots aren't quite enough.
 * This structure holds custom extended colors specific to the app's design components.
 */
data class InlyExtendedColors(
    val variant1: Color,
    val variant2: Color
)

private val LightExtendedColors = InlyExtendedColors(
    variant1 = CharcoalNoir,
    variant2 = CloudVeil
)

private val DarkExtendedColors = InlyExtendedColors(
    variant1 = IroncladGrey,
    variant2 = CloudVeil
)

val LocalInlyExtendedColors = staticCompositionLocalOf {
    InlyExtendedColors(
        variant1 = Color.Unspecified,
        variant2 = Color.Unspecified
    )
}

// Standard Material 3 color mappings
private val LightColorScheme = lightColorScheme(
    primary          = CharcoalNoir,
    onPrimary        = CloudVeil,
    background       = CloudVeil,
    onBackground     = CharcoalNoir,
    surface          = Fog,
    onSurface        = CharcoalNoir,
    surfaceVariant   = MoonlitSilver,
    onSurfaceVariant = IroncladGrey,
    outline          = UrbanFog,
)

private val DarkColorScheme = darkColorScheme(
    primary          = CloudVeil,
    onPrimary        = CharcoalNoir,
    background       = CharcoalNoir,
    onBackground     = CloudVeil,
    surface          = IroncladGrey,
    onSurface        = CloudVeil,
    surfaceVariant   = IroncladGrey,
    onSurfaceVariant = MoonlitSilver,
    outline          = UrbanFog,
)

val LocalAppIsDark = staticCompositionLocalOf { false }

/**
 * The main theme wrapper for the app.
 * It automatically switches between light and dark palettes based on system settings, applies the custom typography, and provides the extended colors down the component tree.
 */
@Composable
fun InlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    SetSystemBars(
        statusBarColor = Color.Transparent,
        darkIcons = !darkTheme
    )

    CompositionLocalProvider(
        LocalAppIsDark provides darkTheme,
        LocalInlyExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}