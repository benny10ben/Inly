package com.ben.inly.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class NoteSyncEvent {
    // A note's persisted content changed (incoming remote sync, an import, or "global_pinned") -
    // listeners should reload it, but only if they don't have unsaved local edits in flight
    data class NoteChanged(val entityId: String) : NoteSyncEvent()

    // A specific block moved to a different daily note - safe to apply even mid-autosave since it
    // mutates only that one block id rather than reconciling a whole note's content
    data class BlockMoved(val blockId: String, val fromDateString: String?, val toDateString: String) : NoteSyncEvent()

    data class BlockRemoved(val blockId: String, val dateString: String) : NoteSyncEvent()
}

object SyncEventBus {
    private val _events = MutableSharedFlow<NoteSyncEvent>()
    val events: SharedFlow<NoteSyncEvent> = _events.asSharedFlow()

    suspend fun emitSyncCompleted(entityId: String) {
        _events.emit(NoteSyncEvent.NoteChanged(entityId))
    }

    suspend fun emitBlockMoved(blockId: String, fromDateString: String?, toDateString: String) {
        _events.emit(NoteSyncEvent.BlockMoved(blockId, fromDateString, toDateString))
    }

    suspend fun emitBlockRemoved(blockId: String, dateString: String) {
        _events.emit(NoteSyncEvent.BlockRemoved(blockId, dateString))
    }
}
