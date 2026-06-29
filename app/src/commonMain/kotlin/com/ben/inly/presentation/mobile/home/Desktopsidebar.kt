package com.ben.inly.presentation.mobile.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ben.inly.data.local.room.FolderEntity
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.ui.theme.PoppinsFont

enum class DropInsertPosition { BEFORE, INTO, AFTER }

private val INDENT_STEP          = 16.dp
private val SIDEBAR_BASE_START   = 8.dp
private val CHEVRON_SLOT         = 26.dp
private val ROW_ICON_SLOT        = 24.dp
private val ROW_ICON_SIZE        = 22.dp
private val ROW_MIN_HEIGHT       = 42.dp
private val ROW_FONT_SIZE        = 16.sp
private val ROW_VERTICAL_PADDING = 2.dp

private val RowColorSpec = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
private val RowFloatSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
private val ChevronSpec  = spring<Float>(stiffness = Spring.StiffnessMediumLow)

internal const val DRAG_PREFIX_NOTE   = "note:"
internal const val DRAG_PREFIX_FOLDER = "folder:"
internal const val DROP_KEY_ROOT      = "drop_root"

class SidebarDragState {
    var dragging     by mutableStateOf(false)
    var payload      by mutableStateOf<String?>(null)
    var dropTargetId by mutableStateOf<String?>(null)
    var dropPosition by mutableStateOf(DropInsertPosition.BEFORE)
    var cursorY      by mutableStateOf(0f)

    fun startDrag(p: String) { payload = p; dragging = true }
    fun endDrag()            { dragging = false; payload = null; dropTargetId = null; cursorY = 0f; dropPosition = DropInsertPosition.BEFORE }
}

@Composable
fun rememberSidebarDragState() = remember { SidebarDragState() }

@Composable
fun Modifier.sidebarNoRippleClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(onClick) {
        detectTapGestures(onTap = { onClick() })
    }

// Tree data

sealed interface SidebarTreeRow {
    val key: String
    val level: Int

    data class Folder(val folder: FolderEntity, override val level: Int) : SidebarTreeRow {
        override val key: String get() = "sb_folder_${folder.folderId}"
    }

    data class Note(val note: NoteMetadataEntity, override val level: Int) : SidebarTreeRow {
        override val key: String get() = "sb_note_${note.noteId}"
    }
}

fun flattenFolderTree(
    parentId: String?,
    level: Int,
    foldersByParent: Map<String?, List<FolderEntity>>,
    notesByFolder: Map<String?, List<NoteMetadataEntity>>,
    expandedFolderIds: Set<String>,
    isManualSort: Boolean = false
): List<SidebarTreeRow> {
    val out     = mutableListOf<SidebarTreeRow>()
    val folders = foldersByParent[parentId].orEmpty()
    val notes   = notesByFolder[parentId].orEmpty()

    val combined: List<SidebarTreeRow> = if (isManualSort) {
        val folderRows = folders.map { SidebarTreeRow.Folder(it, level) }
        val noteRows   = notes.map   { SidebarTreeRow.Note(it, level) }
        (folderRows + noteRows).sortedWith(compareBy(
            { row: SidebarTreeRow ->
                when (row) {
                    is SidebarTreeRow.Folder -> if (row.folder.sortOrder == 0) Int.MAX_VALUE else row.folder.sortOrder
                    is SidebarTreeRow.Note   -> if (row.note.sortOrder == 0)   Int.MAX_VALUE else row.note.sortOrder
                }
            },
            { row: SidebarTreeRow ->
                when (row) { is SidebarTreeRow.Folder -> 0; is SidebarTreeRow.Note -> 1 }
            }
        ))
    } else {
        folders.map { SidebarTreeRow.Folder(it, level) } +
                notes.map   { SidebarTreeRow.Note(it, level) }
    }

    combined.forEach { row ->
        out += row
        if (row is SidebarTreeRow.Folder && row.folder.folderId in expandedFolderIds) {
            out += flattenFolderTree(
                row.folder.folderId, level + 1,
                foldersByParent, notesByFolder, expandedFolderIds, isManualSort
            )
        }
    }
    return out
}

// Drag chip

@Composable
fun SidebarDragChip(
    dragState: SidebarDragState,
    labelForPayload: (String) -> String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = dragState.dragging,
        enter = fadeIn(tween(120)) + expandVertically(tween(120)),
        exit  = fadeOut(tween(80)) + shrinkVertically(tween(80)),
        modifier = modifier.zIndex(100f)
    ) {
        val density     = LocalDensity.current
        val label       = dragState.payload?.let { labelForPayload(it) } ?: ""
        val isFolder    = dragState.payload?.startsWith(DRAG_PREFIX_FOLDER) == true
        val chipOffsetY = with(density) { dragState.cursorY.toDp() - 16.dp }

        Row(
            modifier = Modifier
                .offset(y = chipOffsetY.coerceAtLeast(0.dp))
                .padding(start = 12.dp)
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isFolder) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontFamily = PoppinsFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Row composables

@Composable
fun SidebarFolderRow(
    folder: FolderEntity,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    dragState: SidebarDragState,
    onClick: () -> Unit,
    onAddNote: () -> Unit,
    onAddSubfolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowId = "sb_folder_${folder.folderId}"

    val isInsertBefore = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.BEFORE

    val isInsertAfter = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.AFTER

    val isIntoTarget = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.INTO &&
            dragState.payload != "$DRAG_PREFIX_FOLDER${folder.folderId}"

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgTarget: Color = when {
        isIntoTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isSelected   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        isHovered    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        else         -> Color.Transparent
    }
    val bgColor by animateColorAsState(bgTarget, RowColorSpec, label = "fbg_${folder.folderId}")

    val borderAlpha by animateFloatAsState(if (isIntoTarget) 1f else 0f, RowFloatSpec, label = "fborder_${folder.folderId}")
    val beforeAlpha by animateFloatAsState(if (isInsertBefore) 1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "fbefore_${folder.folderId}")
    val afterAlpha  by animateFloatAsState(if (isInsertAfter)  1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "fafter_${folder.folderId}")

    val chevronRotation by animateFloatAsState(if (isExpanded) 90f else 0f, ChevronSpec, label = "chevron_${folder.folderId}")

    val shape = RoundedCornerShape(10.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        // Insert line above row
        if (beforeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beforeAlpha))
                    .scale(scaleX = beforeAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SIDEBAR_BASE_START + INDENT_STEP * level,
                    end = 8.dp,
                    top = ROW_VERTICAL_PADDING,
                    bottom = ROW_VERTICAL_PADDING
                )
                .clip(shape)
                .background(bgColor)
                .then(
                    if (borderAlpha > 0f) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                        shape
                    ) else Modifier
                )
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.size(CHEVRON_SLOT).rotate(chevronRotation)
            )
            Spacer(Modifier.width(4.dp))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isIntoTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(ROW_ICON_SIZE + 6.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = folder.name,
                fontFamily = PoppinsFont,
                fontSize = ROW_FONT_SIZE,
                fontWeight = FontWeight.Medium,
                color = if (isIntoTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when {
                isSelected -> SidebarTrailingCheck()
                isHovered && !dragState.dragging -> {
                    SidebarHoverAction(Icons.Default.Add, "New note here", onAddNote)
                    Spacer(Modifier.width(4.dp))
                    SidebarHoverAction(Icons.Default.CreateNewFolder, "New subfolder", onAddSubfolder)
                    Spacer(Modifier.width(2.dp))
                }
            }
        }

        // Insert line below row (last item)
        if (afterAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = afterAlpha))
                    .scale(scaleX = afterAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
fun SidebarNoteRow(
    note: NoteMetadataEntity,
    level: Int,
    isActive: Boolean,
    isSelected: Boolean,
    dragState: SidebarDragState,
    onClick: () -> Unit,
    rowKey: String = "sb_note_${note.noteId}",
    modifier: Modifier = Modifier
) {
    val rowId = "sb_note_${note.noteId}"

    val isInsertBefore = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.BEFORE

    val isInsertAfter = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.AFTER

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgTarget: Color = when {
        isActive   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        isSelected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        isHovered  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        else       -> Color.Transparent
    }
    val bgColor by animateColorAsState(bgTarget, RowColorSpec, label = "nbg_${note.noteId}")

    val beforeAlpha by animateFloatAsState(if (isInsertBefore) 1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "nbefore_${note.noteId}")
    val afterAlpha  by animateFloatAsState(if (isInsertAfter)  1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "nafter_${note.noteId}")

    Box(modifier = modifier.fillMaxWidth()) {
        if (beforeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beforeAlpha))
                    .scale(scaleX = beforeAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SIDEBAR_BASE_START + INDENT_STEP * level,
                    end = 8.dp,
                    top = ROW_VERTICAL_PADDING,
                    bottom = ROW_VERTICAL_PADDING
                )
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                if (!note.icon.isNullOrEmpty()) {
                    Text(text = note.icon!!, fontSize = 18.sp, textAlign = TextAlign.Center)
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                        modifier = Modifier.size(ROW_ICON_SIZE)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = note.title.ifEmpty { "Untitled" },
                fontFamily = PoppinsFont,
                fontSize = ROW_FONT_SIZE,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when {
                isSelected -> SidebarTrailingCheck()
                note.isFavorite -> {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp).size(14.dp)
                    )
                }
            }
        }

        if (afterAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = afterAlpha))
                    .scale(scaleX = afterAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
fun SidebarRootDropZone(dragState: SidebarDragState) {
    val isDropTarget = dragState.dragging && dragState.dropTargetId == DROP_KEY_ROOT
    val shape = RoundedCornerShape(10.dp)

    val borderAlpha by animateFloatAsState(
        targetValue = if (isDropTarget) 1f else 0f,
        animationSpec = RowFloatSpec,
        label = "root_border"
    )

    val bgTarget: Color = if (isDropTarget)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val bgColor by animateColorAsState(bgTarget, RowColorSpec, label = "root_bg")

    AnimatedVisibility(
        visible = dragState.dragging,
        enter = fadeIn(tween(150)) + expandVertically(tween(200, easing = FastOutSlowInEasing)),
        exit  = fadeOut(tween(120)) + shrinkVertically(tween(150, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clip(shape)
                .background(bgColor)
                .then(
                    if (borderAlpha > 0f) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                        shape
                    ) else Modifier
                )
                .heightIn(min = 36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = if (isDropTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Move to root",
                fontFamily = PoppinsFont,
                fontSize = 13.sp,
                color = if (isDropTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SidebarSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .sidebarNoRippleClickable { onToggle() }
                .padding(vertical = 2.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle $title",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 2.dp).size(22.dp)
            )
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                content = trailing
            )
        }
    }
}

@Composable
private fun SidebarHoverAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Transparent)
            .sidebarNoRippleClickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SidebarTrailingCheck() {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(20.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
    }
}