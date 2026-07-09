package com.ben.inly.domain.util

import kotlinx.coroutines.sync.Mutex

// Serializes any multi-step local note write against the sync engine's push/pull cycle, so a sync pass
// can never read a note mid-move - e.g. after a block has been removed from its old note but before
// it has landed in its new one.
object SyncCoordinator {
    val mutex = Mutex()
}
