package com.ben.inly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ben.inly.R

/**
 * Hooks up the app's custom font (Bricolage Grotesque) and maps the specific font weights from resources.
 */
val BricolageFont = FontFamily(
    Font(R.font.bricolage_grotesque_regular, FontWeight.Normal),
    Font(R.font.bricolage_grotesque_semibold, FontWeight.Medium),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold)
)

/**
 * Overrides the standard Material Design text styles to use the custom font globally.
 * Anytime a Text composable uses a standard style like `MaterialTheme.typography.titleLarge`, it automatically inherits these exact properties.
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