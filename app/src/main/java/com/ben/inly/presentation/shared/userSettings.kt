package com.ben.inly.presentation.shared

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.ui.theme.BricolageFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

// Adjust this value to change the corner roundness for buttons inside the settings menu
private val DefaultButtonShape = RoundedCornerShape(6.dp)

/**
 * The main settings menu for the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun userSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Options"
    ) { closeAnd ->

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { closeAnd { onNavigateToTrash() } }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Trash",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Trash",
                fontFamily = BricolageFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(48.dp),
            shape = DefaultButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Close",
                fontFamily = BricolageFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}