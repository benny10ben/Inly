package com.ben.inly.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SyncEventBus {
    private val _syncCompletedEvent = MutableSharedFlow<String>()
    val syncCompletedEvent: SharedFlow<String> = _syncCompletedEvent.asSharedFlow()

    suspend fun emitSyncCompleted(entityId: String) {
        _syncCompletedEvent.emit(entityId)
    }
}