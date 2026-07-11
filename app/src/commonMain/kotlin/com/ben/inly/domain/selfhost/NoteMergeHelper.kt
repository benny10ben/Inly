package com.ben.inly.domain.selfhost

import com.ben.inly.data.local.room.NoteBlockEntity

object NoteMergeHelper {

    fun mergeBlocks(
        noteId: String,
        localBlocks: List<NoteBlockEntity>,
        remoteUpserts: List<NoteBlockEntity>,
        remoteDeletions: List<BlockTombstone>
    ): List<NoteBlockEntity> {
        val safeLocalBlocks = localBlocks.filterOwnedBy(noteId, "local")
        val safeRemoteUpserts = remoteUpserts.filterOwnedBy(noteId, "remote")

        val merged = LinkedHashMap<String, NoteBlockEntity>()
        safeLocalBlocks.forEach { merged[it.blockId] = it }

        safeRemoteUpserts.forEach { remoteBlock ->
            val localBlock = merged[remoteBlock.blockId]
            if (localBlock == null || remoteBlock.updatedAt >= localBlock.updatedAt) {
                merged[remoteBlock.blockId] = remoteBlock
            }
        }

        remoteDeletions.forEach { tombstone ->
            val localBlock = merged[tombstone.blockId]
            if (localBlock == null || tombstone.deletedAt >= localBlock.updatedAt) {
                merged[tombstone.blockId] = NoteBlockEntity(
                    blockId = tombstone.blockId,
                    noteId = noteId,
                    displayOrder = localBlock?.displayOrder ?: 0,
                    blockDataJson = localBlock?.blockDataJson.orEmpty(),
                    updatedAt = tombstone.deletedAt,
                    isDeleted = true
                )
            }
        }

        return merged.values.toList()
    }

    private fun List<NoteBlockEntity>.filterOwnedBy(noteId: String, source: String): List<NoteBlockEntity> {
        return filter { block ->
            val ownedByNote = block.noteId == noteId
            if (!ownedByNote) {
                SelfHostSyncLog.e(
                    "NoteMergeHelper: dropped $source block ${block.blockId} belonging to note " +
                        "${block.noteId} while merging note $noteId"
                )
            }
            ownedByNote
        }
    }
}
