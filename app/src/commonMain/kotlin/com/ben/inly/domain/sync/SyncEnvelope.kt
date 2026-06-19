package com.ben.inly.domain.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncType {
    NOTE,
    DAILY_NOTE,
    TAG,
    FOLDER
}

@Serializable
data class SyncEnvelope(
    val entityId: String,
    val entityType: SyncType,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val metadataJson: String,
    val contentJson: String
)

@Serializable
data class SyncPayload(
    val changes: List<SyncEnvelope>
)