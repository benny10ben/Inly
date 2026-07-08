package com.ben.inly.domain.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncType {
    NOTE,
    DAILY_NOTE,
    TAG,
    FOLDER,
    CATEGORY
}

@Serializable
data class SyncEnvelope(
    val entityId: String,
    // Defaulted so Json { coerceInputValues = true } can substitute this instead of throwing
    // when a peer running older code sends an entityType this build doesn't recognize (e.g. a
    // future SyncType case) - that envelope then simply fails to decode as the wrong entity type
    // downstream (caught per-envelope in SyncRepositoryImpl) instead of corrupting the whole batch.
    val entityType: SyncType = SyncType.NOTE,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val metadataJson: String,
    val contentJson: String
)

@Serializable
data class SyncPayload(
    val changes: List<SyncEnvelope>
)