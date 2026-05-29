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
val CharcoalNoir   = Color(0xFF000000)
val IroncladGrey   = Color(0xFF262626)
val IroncladGrey2  = Color(0xFF333333)
val UrbanFog       = Color(0xFF848484)
val MoonlitSilver  = Color(0xFFB3B3B3)
val CloudVeil      = Color(0xFFE0E0E0)
val Fog            = Color(0xFFCCCCCC)

val White            = Color(0xFFffffff)

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

private val LightColorScheme = lightColorScheme(
    primary          = CharcoalNoir,
    onPrimary        = CloudVeil,
    background       = White,
    onBackground     = CharcoalNoir,
    surface          = CloudVeil,
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
@Composable
fun InlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Only override the 'background' color for Desktop Dark Mode.
    val colorScheme = if (darkTheme && isDesktopPlatform) {
        baseColorScheme.copy(background = Color.Black)
    } else {
        baseColorScheme
    }

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