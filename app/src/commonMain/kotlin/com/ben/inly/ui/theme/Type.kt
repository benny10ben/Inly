package com.ben.inly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import inly.app.generated.resources.Res
import inly.app.generated.resources.poppins_bold
import inly.app.generated.resources.poppins_medium
import inly.app.generated.resources.poppins_regular
import org.jetbrains.compose.resources.Font

/**
 * Phase 6 KMP Migration: Loading Poppins across all platforms using Compose Multiplatform's Res API.
 */
val PoppinsFont: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_bold, FontWeight.Bold)
    )

/**
 * Overrides the standard Material Design text styles to use Poppins globally.
 */
val AppTypography: Typography
    @Composable
    get() {
        val poppins = PoppinsFont

        return Typography(
            bodyLarge = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
            titleLarge = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            labelSmall = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
    }