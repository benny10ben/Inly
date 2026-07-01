package com.ben.inly.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.presentation.desktop.DesktopMainScreen

@Composable
actual fun DesktopMainScreenWrapper(
    isSidebarVisible: Boolean,
    sidebarWidth: Dp,
    onToggleSidebar: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit,
    onPickImage: ((String) -> Unit) -> Unit,
    onTakePhoto: ((String) -> Unit) -> Unit,
    onPickDocument: ((String) -> Unit) -> Unit,
    onOpenFile: (String, String) -> Unit,
    onExportMarkdown: (String, String) -> Unit,
    onExportPdf: (String, String, List<NoteBlock>) -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackupClick: () -> Unit
) {
    DesktopMainScreen(
        isSidebarVisible = isSidebarVisible,
        sidebarWidth = sidebarWidth,
        onToggleSidebar = onToggleSidebar,
        onSelectionModeChange = onSelectionModeChange,
        onPickImage = onPickImage,
        onTakePhoto = onTakePhoto,
        onPickDocument = onPickDocument,
        onOpenFile = onOpenFile,
        onExportMarkdown = onExportMarkdown,
        onExportPdf = onExportPdf,
        onExportBackup = onExportBackup,
        onImportBackupClick = onImportBackupClick
    )
}