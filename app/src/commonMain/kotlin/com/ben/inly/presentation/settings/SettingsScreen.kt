package com.ben.inly.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyButtonSecondary
import com.ben.inly.presentation.shared.components.TopBarIconButton
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onImportClick: () -> Unit = {},
    onExportReady: (String) -> Unit = {},
    onImportReady: (String) -> Unit = {},
    onRequestBackupFolder: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var showImportExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Backup States
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val backupDirectoryUri by viewModel.backupDirectoryUri.collectAsState()

    val backupTime by viewModel.backupTime.collectAsState()
    val backupDay by viewModel.backupDay.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsTopBar(onNavigateBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            item {
                SettingsGroup(title = "Data & Storage") {
                    SettingsActionRow(
                        icon = Icons.Default.ImportExport,
                        title = "Import / Export",
                        onClick = { showImportExportSheet = true }
                    )

                    if (!com.ben.inly.domain.util.isDesktopPlatform) {
                        SettingsToggleRow(
                            icon = Icons.Default.Backup,
                            title = "Automatic Backups",
                            isChecked = autoBackupEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (backupDirectoryUri == null) {
                                        onRequestBackupFolder()
                                    } else {
                                        viewModel.setAutoBackupEnabled(true)
                                    }
                                } else {
                                    viewModel.setAutoBackupEnabled(false)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = autoBackupEnabled,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                            ) {
                                SettingsDivider()

                                val frequencies = listOf("Hourly", "Daily", "Weekly")
                                frequencies.forEach { freq ->
                                    SettingsSelectionRow(
                                        title = freq,
                                        isSelected = backupFrequency == freq,
                                        onClick = {
                                            viewModel.saveBackupSchedule(
                                                freq,
                                                backupTime,
                                                backupDay
                                            )
                                        }
                                    )
                                }

                                if (backupFrequency != "Hourly") {
                                    SettingsDivider()

                                    if (backupFrequency == "Weekly") {
                                        SettingsActionRow(
                                            icon = Icons.Default.CalendarToday,
                                            title = "Backup Day",
                                            trailingLabel = backupDay,
                                            onClick = { showDayPicker = true }
                                        )
                                    }

                                    SettingsActionRow(
                                        icon = Icons.Default.Schedule,
                                        title = "Backup Time",
                                        trailingLabel = backupTime,
                                        onClick = { showTimePicker = true }
                                    )
                                }

                                SettingsDivider()
                                SettingsActionRow(
                                    icon = Icons.Default.FolderOpen,
                                    title = "Backup Location",
                                    trailingLabel = "Change",
                                    onClick = { onRequestBackupFolder() }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Appearance") {
                    SettingsActionRow(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        trailingLabel = "System",
                        onClick = {}
                    )
                }
            }

            item {
                SettingsGroup(title = "Need Help?") {
                    SettingsActionRow(
                        icon = Icons.Default.HelpOutline,
                        title = "FAQ",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = Icons.Default.NewReleases,
                        title = "What's New",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = {}
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = "About Inly",
                        trailingLabel = "v1.0.0",
                        onClick = {}
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
                        .clickable {}
                        .padding(horizontal = 14.dp, vertical = 15.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Clear All Data",
                            fontFamily = PoppinsFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showImportExportSheet) {
            InlyBottomSheet(
                expanded = showImportExportSheet,
                onDismiss = { showImportExportSheet = false },
                title = "Import / Export",
                subtitle = "Backup your data securely to a local file, or restore a previous backup."
            ) { closeAnd ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InlyButtonPrimary(
                        text = if (isExporting) "Preparing..." else "Export Backup (.inly)",
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val json = viewModel.getBackupJson()
                                        showImportExportSheet = false
                                        isExporting = false
                                        onExportReady(json)
                                    } catch (e: Exception) {
                                        isExporting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    InlyButtonSecondary(
                        text = "Import Backup",
                        onClick = {
                            showImportExportSheet = false
                            onImportClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        // Convert "HH:mm" into a dummy timestamp so the picker initializes at the correct time
        val timeParts = backupTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 2
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }

        com.ben.inly.presentation.shared.components.MinimalTimePickerDialog(
            expanded = showTimePicker,
            initialTimestamp = cal.timeInMillis,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                val formattedTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.saveBackupSchedule(backupFrequency, formattedTime, backupDay)
            }
        )
    }

    if (showDayPicker) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var selectedIndex by remember { mutableStateOf(days.indexOf(backupDay).coerceAtLeast(0)) }

        val wheelContent = @Composable {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.ben.inly.presentation.shared.components.WheelPicker(
                    items = days,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                    itemHeight = if (com.ben.inly.domain.util.isDesktopPlatform) 40.dp else 44.dp
                )
            }
        }

        if (com.ben.inly.domain.util.isDesktopPlatform) {
            com.ben.inly.presentation.shared.components.InlyDesktopMenu(
                expanded = showDayPicker,
                onDismissRequest = { showDayPicker = false }
            ) {
                Column(modifier = Modifier.width(280.dp).wrapContentHeight()) {
                    wheelContent()
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InlyButtonSecondary(text = "Cancel", onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                        InlyButtonPrimary(text = "Save", onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            InlyBottomSheet(
                expanded = showDayPicker,
                onDismiss = { showDayPicker = false },
                title = "Select Backup Day"
            ) {
                wheelContent()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InlyButtonSecondary(text = "Cancel", onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                    InlyButtonPrimary(text = "Save", onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SettingsSelectionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .padding(start = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            bgColor = MaterialTheme.colorScheme.surface,
            tint = MaterialTheme.colorScheme.onSurface,
            hazeState = null,
            onClick = onNavigateBack
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Settings",
            fontFamily = PoppinsFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            fontFamily = PoppinsFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontFamily = PoppinsFont,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                fontFamily = PoppinsFont,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontFamily = PoppinsFont,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}