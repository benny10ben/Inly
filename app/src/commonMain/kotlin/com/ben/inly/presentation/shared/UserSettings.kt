package com.ben.inly.presentation.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import com.ben.inly.ui.theme.PoppinsFont
import inly.app.generated.resources.Res
import inly.app.generated.resources.cog
import inly.app.generated.resources.qr_code
import inly.app.generated.resources.refresh_cw
import inly.app.generated.resources.scan_line
import inly.app.generated.resources.trash_2
import org.jetbrains.compose.resources.painterResource

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
            icon = painterResource(Res.drawable.cog),
            text = "Settings",
            onClick = {
                onDismiss()
                onNavigateToSettings()
            }
        )

        DesktopMenuItem(
            icon = painterResource(Res.drawable.qr_code),
            text = "Pair Mobile Device",
            onClick = {
                onDismiss()
                onShowPairingCode()
            }
        )

        DesktopMenuItem(
            icon = painterResource(Res.drawable.trash_2),
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
    icon: Painter,
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

        BottomSheetItem("Settings", painterResource(Res.drawable.cog)) { closeAnd { onNavigateToSettings() } }

        BottomSheetItem("Pair with Desktop", painterResource(Res.drawable.scan_line)) { closeAnd { onScanPairingCode() } }

        BottomSheetItem("Sync Now", painterResource(Res.drawable.refresh_cw)) { closeAnd { onSyncNow() } }

        BottomSheetItem("Trash", painterResource(Res.drawable.trash_2)) { closeAnd { onNavigateToTrash() } }

        InlyButtonPrimary(
            text = "Close",
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun BottomSheetItem(text: String, icon: Painter, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}