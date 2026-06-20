package com.ben.inly.presentation.shared.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ben.inly.domain.util.isDesktopPlatform
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

fun Modifier.customInlyShadow(shape: Shape): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.35f),
    ambientColor = Color.Black.copy(alpha = 0.20f)
)

@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    bgColor: Color,
    tint: Color,
    hazeState: HazeState? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        contentColor = tint,
        modifier = Modifier
            .size(44.dp)
            .customInlyShadow(CircleShape)
            .clip(CircleShape)
            .then(if (isDesktopPlatform || hazeState == null) Modifier else Modifier.hazeChild(hazeState))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}