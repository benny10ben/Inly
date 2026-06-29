package com.ben.inly.presentation.mobile.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.sidebarDragTracker(
    dragState: SidebarDragState,
    listState: LazyListState,
    rowKeys: List<String?>,
    payloadForKey: (String?) -> String?,
    isDropTarget: (key: String?, payload: String) -> Boolean,
    onDrop: (payload: String, targetKey: String, insertBefore: Boolean) -> Unit,
    rowHeightPx: Float,
    dragThresholdPx: Float = 8f
): Modifier {
    val currentRowKeys       by rememberUpdatedState(rowKeys)
    val currentPayloadForKey by rememberUpdatedState(payloadForKey)
    val currentIsDropTarget  by rememberUpdatedState(isDropTarget)
    val currentOnDrop        by rememberUpdatedState(onDrop)

    return this.pointerInput(dragState, listState) {
        awaitPointerEventScope {
            while (true) {
                val press = awaitPointerEvent()
                if (press.type != PointerEventType.Press) continue
                val pressPos = press.changes.firstOrNull()?.position ?: continue

                var pressedKey: String? = keyAtY(pressPos.y, listState, currentRowKeys)
                var dragStarted = false

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break

                    when (event.type) {
                        PointerEventType.Move -> {
                            val dist = (change.position - pressPos).getDistance()

                            if (!dragStarted && dist > dragThresholdPx && pressedKey != null) {
                                val payload = currentPayloadForKey(pressedKey)
                                if (payload != null) {
                                    dragState.startDrag(payload)
                                    dragStarted = true
                                }
                            }

                            if (dragStarted) {
                                change.consume()
                                val cursorY = change.position.y
                                dragState.cursorY = cursorY
                                val payload = dragState.payload ?: ""

                                resolveDropTarget(
                                    cursorY      = cursorY,
                                    listState    = listState,
                                    rowKeys      = currentRowKeys,
                                    payload      = payload,
                                    isDropTarget = currentIsDropTarget,
                                    dragState    = dragState
                                )
                            }
                        }

                        PointerEventType.Release -> {
                            if (dragStarted) {
                                val target  = dragState.dropTargetId
                                val payload = dragState.payload
                                if (target != null && payload != null) {
                                    currentOnDrop(
                                        payload,
                                        target,
                                        dragState.dropPosition == DropInsertPosition.BEFORE
                                    )
                                }
                            }
                            dragState.endDrag()
                            pressedKey  = null
                            dragStarted = false
                            break
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

private fun resolveDropTarget(
    cursorY: Float,
    listState: LazyListState,
    rowKeys: List<String?>,
    payload: String,
    isDropTarget: (key: String?, payload: String) -> Boolean,
    dragState: SidebarDragState
) {
    val layoutItems = listState.layoutInfo.visibleItemsInfo
    if (layoutItems.isEmpty()) return

    val rootLayoutItem = layoutItems.firstOrNull { item ->
        rowKeys.getOrNull(item.index) == DROP_KEY_ROOT
    }
    if (rootLayoutItem != null) {
        val top    = rootLayoutItem.offset.toFloat()
        val bottom = top + rootLayoutItem.size.toFloat()
        if (cursorY in top..bottom) {
            dragState.dropTargetId = DROP_KEY_ROOT
            dragState.dropPosition = DropInsertPosition.INTO
            return
        }
    }

    data class DragItem(
        val key: String,
        val top: Float,
        val bottom: Float,
        val isFolder: Boolean
    ) {
        val center get() = (top + bottom) / 2f
        val height get() = bottom - top
    }

    val draggable = layoutItems
        .mapNotNull { item ->
            val key = rowKeys.getOrNull(item.index) ?: return@mapNotNull null
            if (key == DROP_KEY_ROOT) return@mapNotNull null
            if (!isDropTarget(key, payload)) return@mapNotNull null
            val top    = item.offset.toFloat()
            val bottom = top + item.size.toFloat()
            DragItem(key, top, bottom, key.startsWith("sb_folder_"))
        }
        .sortedBy { it.top }

    if (draggable.isEmpty()) return
    if (cursorY < draggable.first().top) {
        dragState.dropTargetId = draggable.first().key
        dragState.dropPosition = DropInsertPosition.BEFORE
        return
    }
    if (cursorY >= draggable.last().bottom) {
        dragState.dropTargetId = draggable.last().key
        dragState.dropPosition = DropInsertPosition.AFTER
        return
    }

    val hit = draggable.firstOrNull { cursorY >= it.top && cursorY < it.bottom }

    if (hit != null) {
        if (hit.isFolder) {
            // BEFORE: top 10% of the row
            // INTO:   middle 80% of the row
            // AFTER:  bottom 10% of the row
            val edgeZone = hit.height * 0.10f
            dragState.dropTargetId = hit.key
            dragState.dropPosition = when {
                cursorY < hit.top    + edgeZone -> DropInsertPosition.BEFORE
                cursorY > hit.bottom - edgeZone -> DropInsertPosition.AFTER
                else                            -> DropInsertPosition.INTO
            }
        } else {
            dragState.dropTargetId = hit.key
            dragState.dropPosition = if (cursorY < hit.center) DropInsertPosition.BEFORE
            else                      DropInsertPosition.AFTER
        }
        return
    }

    var bestItem = draggable.first()
    var bestDist = Float.MAX_VALUE
    for (item in draggable) {
        val d1 = kotlin.math.abs(cursorY - item.top)
        val d2 = kotlin.math.abs(cursorY - item.bottom)
        if (d1 < bestDist) { bestDist = d1; bestItem = item }
        if (d2 < bestDist) { bestDist = d2; bestItem = item }
    }
    dragState.dropTargetId = bestItem.key
    dragState.dropPosition = if (cursorY < bestItem.center) DropInsertPosition.BEFORE
    else                           DropInsertPosition.AFTER
}

private fun keyAtY(y: Float, listState: LazyListState, rowKeys: List<String?>): String? {
    for (item in listState.layoutInfo.visibleItemsInfo) {
        val top    = item.offset.toFloat()
        val bottom = top + item.size.toFloat()
        if (y >= top && y < bottom) return rowKeys.getOrNull(item.index)
    }
    return null
}