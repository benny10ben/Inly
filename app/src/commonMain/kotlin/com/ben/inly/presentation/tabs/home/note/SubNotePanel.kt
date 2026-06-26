package com.ben.inly.presentation.tabs.home.note

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.presentation.shared.components.TopBarIconButton

/**
 * Notion-style slide-in panel for database row notes (desktop only).
 *
 * MUST be placed directly inside the right-panel Box (after clipping).
 * fillMaxSize() then refers only to the right panel — the left sidebar
 * is never touched, even when isExpanded = true (widthFraction = 1f).
 *
 * The expand icon sits immediately to the right of NoteTopBar's back arrow,
 * styled identically (44dp container, 22dp icon, same bg/tint).
 */
@Composable
fun SubNotePanel(
    noteId: String,
    onClose: () -> Unit,
    onExpand: (String) -> Unit,
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> },
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(noteId) { visible = true }

    var isExpanded by remember(noteId) { mutableStateOf(false) }

    val widthFraction by animateFloatAsState(
        targetValue = when {
            !visible   -> 0f
            isExpanded -> 1f
            else       -> 0.5f
        },
        animationSpec = tween(durationMillis = 280),
        label = "panelWidth"
    )

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible && !isExpanded) 0.3f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "scrimAlpha"
    )

    val dismiss: () -> Unit = {
        visible = false
        onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Scrim — tap to close.
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismiss
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(if (isExpanded) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
        ) {
            var showInnerPanel by remember { mutableStateOf(false) }
            var innerPanelNoteId by remember { mutableStateOf<String?>(null) }

            val panelColorScheme = MaterialTheme.colorScheme.copy(
                background = if (isExpanded) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
            )
            MaterialTheme(colorScheme = panelColorScheme) {
                NoteScreen(
                    noteId = noteId,
                    onNavigateBack = dismiss,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto,
                    onPickDocument = onPickDocument,
                    onOpenFile = onOpenFile,
                    onExportMarkdown = onExportMarkdown,
                    onExportPdf = onExportPdf,
                    onNavigateToEditor = { nestedId ->
                        innerPanelNoteId = nestedId
                        showInnerPanel = true
                    }
                )

                if (showInnerPanel && innerPanelNoteId != null) {
                    SubNotePanel(
                        noteId = innerPanelNoteId!!,
                        onClose = { showInnerPanel = false; innerPanelNoteId = null },
                        onExpand = onExpand,
                        onPickImage = onPickImage,
                        onTakePhoto = onTakePhoto,
                        onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile,
                        onExportMarkdown = onExportMarkdown,
                        onExportPdf = onExportPdf,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp, start = 64.dp)
            ) {
                TopBarIconButton(
                    icon = if (isExpanded) Icons.Default.KeyboardArrowLeft else Icons.Default.OpenInFull,
                    contentDescription = if (isExpanded) "Collapse panel" else "Expand panel",
                    bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = { isExpanded = !isExpanded }
                )
            }
        }
    }
}