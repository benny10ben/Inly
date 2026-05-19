package com.ben.inly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Phase 5 KMP Migration: Swapped to default SansSerif to break the Android `R.font` dependency.
 * In Phase 6, we will load Bricolage Grotesque across all platforms using Compose Multiplatform's `Res.font` system.
 */
val BricolageFont = FontFamily.SansSerif

/**
 * Overrides the standard Material Design text styles to use the custom font globally.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BricolageFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)