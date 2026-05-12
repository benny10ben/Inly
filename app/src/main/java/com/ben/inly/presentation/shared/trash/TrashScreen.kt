package com.ben.inly.presentation.shared.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.presentation.notes.NoteCard
import com.ben.inly.ui.theme.BricolageFont
import kotlinx.coroutines.launch

// Centralized shapes for easy UI tweaking
private val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val ButtonShape = RoundedCornerShape(6.dp)

/**
 * The main screen for viewing and managing deleted notes.
 * Notes here can be permanently deleted or restored to their original location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    var selectedNoteToManage by remember { mutableStateOf<NoteMetadataEntity?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Trash", fontFamily = BricolageFont, fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (trashedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Trash is empty",
                    fontFamily = BricolageFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 16.dp, bottom = 80.dp, start = 22.dp, end = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
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

        if (selectedNoteToManage != null) {
            ManageNoteBottomSheet(
                onDismiss = { selectedNoteToManage = null },
                onRestore = {
                    viewModel.restoreNote(selectedNoteToManage!!.noteId)
                    selectedNoteToManage = null
                },
                onPermanentlyDelete = {
                    viewModel.permanentlyDelete(selectedNoteToManage!!.noteId, selectedNoteToManage!!.filePath)
                    selectedNoteToManage = null
                }
            )
        }

        if (showEmptyTrashConfirm) {
            EmptyTrashBottomSheet(
                onDismiss = { showEmptyTrashConfirm = false },
                onConfirmEmpty = {
                    viewModel.emptyTrash()
                    showEmptyTrashConfirm = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageNoteBottomSheet(
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeAnd(action: () -> Unit) {
        coroutineScope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)) }
    ) {
        BackHandler { closeAnd(onDismiss) }

        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                text = "Manage Note",
                fontFamily = BricolageFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Text(
                text = "Notes in trash are automatically deleted after 30 days.",
                fontFamily = BricolageFont,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )

            BottomSheetActionItem(Icons.Default.Restore, "Restore Note") { closeAnd { onRestore() } }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            BottomSheetActionItem(Icons.Default.DeleteForever, "Delete Permanently", isDestructive = true) { closeAnd { onPermanentlyDelete() } }

            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyTrashBottomSheet(
    onDismiss: () -> Unit,
    onConfirmEmpty: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeAnd(action: () -> Unit) {
        coroutineScope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)) }
    ) {
        BackHandler { closeAnd(onDismiss) }

        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                text = "Empty Trash?",
                fontFamily = BricolageFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Text(
                text = "This will permanently delete all notes currently in the trash. This action cannot be undone.",
                fontFamily = BricolageFont,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )

            BottomSheetActionItem(Icons.Default.DeleteSweep, "Empty Trash", isDestructive = true) { closeAnd { onConfirmEmpty() } }

            Button(
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Cancel", fontFamily = BricolageFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
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
        Text(text, fontFamily = BricolageFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}