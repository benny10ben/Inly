package com.ben.inly.domain.sync

interface SyncRepository {
    // Collects all changes since the last successful sync timestamp
    suspend fun collectLocalChanges(): List<SyncEnvelope>

    // Applies incoming changes to the local Room database and file storage
    suspend fun applyRemoteChanges(changes: List<SyncEnvelope>)
}