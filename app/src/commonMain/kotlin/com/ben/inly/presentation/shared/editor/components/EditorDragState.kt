package com.ben.inly.presentation.shared.editor.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import kotlin.collections.iterator

enum class DropTargetZone {
    TOP, BOTTOM, LEFT, RIGHT, NONE
}

data class DragDropState(
    val isDragging: Boolean = false,
    val draggedBlockId: String? = null,
    val hoveredBlockId: String? = null,
    val activeDropZone: DropTargetZone = DropTargetZone.NONE,
    val pointerPositionInWindow: Offset = Offset.Zero,
    val grabOffsetInBlock: Offset = Offset.Zero,
    val draggedBlockSize: IntSize = IntSize.Zero
) {
    val isValidDrop: Boolean
        get() = isDragging &&
                draggedBlockId != null &&
                hoveredBlockId != null &&
                hoveredBlockId != draggedBlockId &&
                activeDropZone != DropTargetZone.NONE
}
val LocalDragDropState = compositionLocalOf { mutableStateOf(DragDropState()) }
class BlockBoundsRegistry {
    private val bounds = HashMap<String, Rect>()

    fun update(id: String, rect: Rect) { bounds[id] = rect }
    fun remove(id: String) { bounds.remove(id) }
    fun size(): Int = bounds.size

    fun hitTest(point: Offset): Pair<String, Rect>? {
        var best: Pair<String, Rect>? = null
        var bestArea = Float.MAX_VALUE
        for ((id, rect) in bounds) {
            if (rect.contains(point)) {
                val area = rect.width * rect.height
                if (area < bestArea) {
                    bestArea = area
                    best = id to rect
                }
            }
        }
        return best
    }
}

val LocalBlockBoundsRegistry = staticCompositionLocalOf { BlockBoundsRegistry() }