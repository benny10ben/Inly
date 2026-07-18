package com.ben.inly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import inly.app.generated.resources.Res
import inly.app.generated.resources.poppins_bold
import inly.app.generated.resources.poppins_medium
import inly.app.generated.resources.poppins_regular
import org.jetbrains.compose.resources.Font

// --- Font Size Definitions ---

data class InlyFontSizes(
    val bodyLarge: TextUnit,
    val titleLarge: TextUnit,
    val labelSmall: TextUnit
)

// Mobile Profiles
val MobileFontSizesSmall = InlyFontSizes(bodyLarge = 14.sp, titleLarge = 20.sp, labelSmall = 12.sp)
val MobileFontSizesDefault = InlyFontSizes(bodyLarge = 15.sp, titleLarge = 21.sp, labelSmall = 12.sp)
val MobileFontSizesLarge = InlyFontSizes(bodyLarge = 16.sp, titleLarge = 22.sp, labelSmall = 13.sp)

// Desktop Profiles (Scaled up slightly)
val DesktopFontSizesSmall = InlyFontSizes(bodyLarge = 15.sp, titleLarge = 21.sp, labelSmall = 13.sp)
val DesktopFontSizesDefault = InlyFontSizes(bodyLarge = 16.sp, titleLarge = 22.sp, labelSmall = 13.sp)
val DesktopFontSizesLarge = InlyFontSizes(bodyLarge = 17.sp, titleLarge = 23.sp, labelSmall = 14.sp)

val LocalInlyFontSizes = staticCompositionLocalOf { MobileFontSizesDefault }

// --- Typography ---

val PoppinsFont: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_bold, FontWeight.Bold)
    )

val AppTypography: Typography
    @Composable
    get() {
        val poppins = PoppinsFont
        val sizes = LocalInlyFontSizes.current

        return Typography(
            bodyLarge = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Normal,
                fontSize = sizes.bodyLarge,
                lineHeight = (sizes.bodyLarge.value * 1.5f).sp,
                letterSpacing = 0.5.sp
            ),
            titleLarge = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Bold,
                fontSize = sizes.titleLarge,
                lineHeight = (sizes.titleLarge.value * 1.25f).sp,
                letterSpacing = 0.sp
            ),
            labelSmall = TextStyle(
                fontFamily = poppins,
                fontWeight = FontWeight.Medium,
                fontSize = sizes.labelSmall,
                lineHeight = (sizes.labelSmall.value * 1.5f).sp,
                letterSpacing = 0.5.sp
            )
        )
    }