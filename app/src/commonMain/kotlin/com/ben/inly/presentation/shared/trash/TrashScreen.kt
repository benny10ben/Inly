package com.ben.inly.presentation.shared.trash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.notes.NoteCard
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val DefaultCornerShape = RoundedCornerShape(12.dp)
private val HORIZONTAL_PADDING = 16.dp

/**
 * The shared multiplatform screen for viewing and managing deleted notes.
 * Notes here can be permanently deleted or restored to their original location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel()
) {
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    var selectedNoteToManage by remember { mutableStateOf<NoteMetadataEntity?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    KmpBackHandler(enabled = true) {
        onNavigateBack()
    }

    Scaffold(
        containerColor = if (isDesktopPlatform) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Trash", fontFamily = PoppinsFont, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty Trash", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDesktopPlatform) Color.Transparent else MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (trashedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Trash is empty",
                    fontFamily = PoppinsFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + if (isDesktopPlatform) 20.dp else 16.dp,
                    bottom = 80.dp,
                    start = HORIZONTAL_PADDING,
                    end = HORIZONTAL_PADDING
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashedNotes, key = { it.noteId }) { note ->
                    NoteCard(
                        note = note,
                        isSelected = false,
                        onClick = { selectedNoteToManage = note },
                        onLongClick = { selectedNoteToManage = note }
                    )
                }
            }
        }

        ManageNoteBottomSheet(
            expanded = selectedNoteToManage != null,
            onDismiss = { selectedNoteToManage = null },
            onRestore = {
                val noteId = selectedNoteToManage?.noteId ?: return@ManageNoteBottomSheet
                selectedNoteToManage = null
                viewModel.restoreNote(noteId)
            },
            onPermanentlyDelete = {
                val note = selectedNoteToManage ?: return@ManageNoteBottomSheet
                selectedNoteToManage = null
                viewModel.permanentlyDelete(note.noteId, note.filePath)
            }
        )

        EmptyTrashBottomSheet(
            expanded = showEmptyTrashConfirm,
            onDismiss = { showEmptyTrashConfirm = false },
            onConfirmEmpty = {
                showEmptyTrashConfirm = false
                viewModel.emptyTrash()
            }
        )
    }
}

@Composable
fun ManageNoteBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Manage Note",
        subtitle = "Notes in trash are automatically deleted after 30 days."
    ) { closeAnd ->
        BottomSheetActionItem(Icons.Default.Restore, "Restore Note") {
            closeAnd {
                scope.launch { delay(250); onRestore() }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        BottomSheetActionItem(Icons.Default.DeleteForever, "Delete Permanently", isDestructive = true) {
            closeAnd {
                scope.launch { delay(250); onPermanentlyDelete() }
            }
        }

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
            shape = DefaultCornerShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Cancel", fontFamily = PoppinsFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun EmptyTrashBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onConfirmEmpty: () -> Unit
) {
    val scope = rememberCoroutineScope()

    InlyBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Empty Trash?",
        subtitle = "This will permanently delete all notes currently in the trash. This action cannot be undone."
    ) { closeAnd ->
        BottomSheetActionItem(Icons.Default.DeleteSweep, "Empty Trash", isDestructive = true) {
            closeAnd {
                scope.launch { delay(250); onConfirmEmpty() }
            }
        }

        Button(
            onClick = { closeAnd(onDismiss) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
            shape = DefaultCornerShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Cancel", fontFamily = PoppinsFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun BottomSheetActionItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontFamily = PoppinsFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}