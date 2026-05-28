package com.ben.inly.sync

import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent

object NoteMergeHelper {

    /**
     * Three-way merge of note content.
     *
     * Rules:
     * - DatabaseBlock: always structural merge (union rows+cols, soft-delete flags OR'd)
     * - All other blocks that exist in BOTH local and remote: last-write-wins by timestamp
     * - Blocks only in remote: include them (remote added a block we don't have)
     * - Blocks only in local: include them (local added a block remote doesn't have yet),
     *   preserving their original position rather than appending to the end
     */
    fun mergeNoteContent(
        localContent: NoteContent?,
        localUpdatedAt: Long,
        remoteContent: NoteContent,
        remoteUpdatedAt: Long
    ): NoteContent {
        if (localContent == null) return remoteContent

        val localById = localContent.blocks.associateBy { it.id }
        val remoteById = remoteContent.blocks.associateBy { it.id }

        val remoteBlockIds = remoteById.keys

        // Build the merged block list starting from remote ordering
        val mergedBlocks = remoteContent.blocks.map { remoteBlock ->
            val localBlock = localById[remoteBlock.id]

            when {
                remoteBlock is DatabaseBlock -> {
                    mergeDatabase(localBlock as? DatabaseBlock, remoteBlock)
                }
                localBlock == null -> {
                    // New block from remote — take it
                    remoteBlock
                }
                remoteUpdatedAt >= localUpdatedAt -> {
                    // Remote is same age or newer — trust remote for this block's content
                    remoteBlock
                }
                else -> {
                    // Local is newer — keep local version of this block
                    localBlock
                }
            }
        }.toMutableList()

        // Interleave local-only blocks back into their original positions
        // rather than appending blindly to the end.
        val localOnlyBlocks = localContent.blocks.filter { it.id !in remoteBlockIds }
        if (localOnlyBlocks.isNotEmpty()) {
            val localOriginalOrder = localContent.blocks.map { it.id }

            localOnlyBlocks.forEach { localBlock ->
                val localIndex = localOriginalOrder.indexOf(localBlock.id)

                // Find the nearest preceding block (by local order) that exists in merged list
                val precedingId = localOriginalOrder
                    .subList(0, localIndex)
                    .lastOrNull { id -> mergedBlocks.any { it.id == id } }

                val insertAfter = if (precedingId != null) {
                    mergedBlocks.indexOfFirst { it.id == precedingId }
                } else {
                    -1 // insert at start
                }

                mergedBlocks.add(insertAfter + 1, localBlock)
            }
        }

        return NoteContent(blocks = mergedBlocks)
    }

    private fun mergeDatabase(localBlock: DatabaseBlock?, remoteBlock: DatabaseBlock): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        // Columns: union, with soft-delete OR'd for shared columns
        val localColMap = localBlock.columns.associateBy { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }

        val mergedColumns = remoteBlock.columns.map { remoteCol ->
            val localCol = localColMap[remoteCol.id]
            if (localCol != null) remoteCol.copy(isDeleted = localCol.isDeleted || remoteCol.isDeleted)
            else remoteCol
        }.toMutableList()

        // Local-only columns appended (they were added locally, remote hasn't seen them yet)
        localBlock.columns
            .filter { it.id !in remoteColMap }
            .forEach { mergedColumns.add(it) }

        // Rows: union, with soft-delete OR'd and cells merged (remote wins on conflict)
        val localRowMap = localBlock.rows.associateBy { it.id }
        val remoteRowMap = remoteBlock.rows.associateBy { it.id }
        val allRowIds = (localRowMap.keys + remoteRowMap.keys).distinct()

        val mergedRows = allRowIds.mapNotNull { id ->
            val localRow = localRowMap[id]
            val remoteRow = remoteRowMap[id]

            when {
                localRow != null && remoteRow != null -> {
                    // Merge cells: local first, remote overwrites on key conflict
                    val mergedCells = localRow.cells + remoteRow.cells
                    remoteRow.copy(
                        cells = mergedCells,
                        isDeleted = localRow.isDeleted || remoteRow.isDeleted
                    )
                }
                else -> localRow ?: remoteRow
            }
        }

        return remoteBlock.copy(columns = mergedColumns, rows = mergedRows)
    }
}