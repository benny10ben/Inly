package com.ben.inly.presentation.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import com.ben.inly.ui.theme.PoppinsFont

/**
 * A platform-aware settings menu.
 * On desktop, it renders a DropdownMenu popup.
 * On Android/iOS, it renders a BottomSheet dialog.
 */
@Composable
fun UserSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit, // Added new parameter
    onNavigateToTrash: () -> Unit,
    onShowPairingCode: () -> Unit,
    onScanPairingCode: () -> Unit,
    onSyncNow: () -> Unit
) {
    if (isDesktopPlatform) {
        InlyDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            UserSettingsDesktopMenu(
                onDismiss = onDismiss,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTrash = onNavigateToTrash,
                onShowPairingCode = onShowPairingCode
            )
        }
    } else {
        UserSettingsBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToTrash = onNavigateToTrash,
            onScanPairingCode = onScanPairingCode,
            onSyncNow = onSyncNow
        )
    }
}

// Desktop Popup Menu
@Composable
private fun UserSettingsDesktopMenu(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onShowPairingCode: () -> Unit
) {
    Column(modifier = Modifier.width(220.dp).padding(vertical = 4.dp)) {

        DesktopMenuItem(
            icon = Icons.Default.Settings,
            text = "Settings",
            onClick = {
                onDismiss()
                onNavigateToSettings()
            }
        )

        DesktopMenuItem(
            icon = Icons.Default.QrCode,
            text = "Pair Mobile Device",
            onClick = {
                onDismiss()
                onShowPairingCode()
            }
        )

        DesktopMenuItem(
            icon = Icons.Default.DeleteSweep,
            text = "Trash",
            onClick = {
                onDismiss()
                onNavigateToTrash()
            }
        )
    }
}

@Composable
private fun DesktopMenuItem(
    icon: ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// Mobile Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSettingsBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onScanPairingCode: () -> Unit,
    onSyncNow: () -> Unit
) {
    InlyBottomSheet(expanded = expanded, onDismiss = onDismiss, title = null) { closeAnd ->

        BottomSheetItem("Settings", Icons.Default.Settings) { closeAnd { onNavigateToSettings() } }

        BottomSheetItem("Pair with Desktop", Icons.Default.QrCodeScanner) { closeAnd { onScanPairingCode() } }

        BottomSheetItem("Sync Now", Icons.Default.Sync) { closeAnd { onSyncNow() } }

        BottomSheetItem("Trash", Icons.Default.DeleteSweep) { closeAnd { onNavigateToTrash() } }

        InlyButtonPrimary(
            text = "Close",
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun BottomSheetItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}