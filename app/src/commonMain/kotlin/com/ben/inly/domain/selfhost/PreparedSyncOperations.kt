package com.ben.inly.domain.selfhost

import com.ben.inly.data.local.room.NoteBlockEntity
import com.ben.inly.data.local.room.NoteMetadataEntity

data class PreparedSyncOperations(
    val metadataUpsert: NoteMetadataEntity,
    val blockUpserts: List<NoteBlockEntity>,
    val blockDeletions: List<BlockTombstone>
)
