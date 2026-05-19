package com.ben.inly.domain.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncType {
    STANDALONE_NOTE,
    DAILY_NOTE
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