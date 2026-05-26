package com.ben.inly.sync

import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.NoteContent

object NoteMergeHelper {

    fun mergeNoteContent(localContent: NoteContent?, remoteContent: NoteContent): NoteContent {
        if (localContent == null) return remoteContent

        val mergedBlocks = remoteContent.blocks.map { remoteBlock ->
            if (remoteBlock is DatabaseBlock) {
                val localBlock = localContent.blocks.find { it.id == remoteBlock.id } as? DatabaseBlock

                if (localBlock != null) {

                    // --- 1. COLUMN MERGE ---
                    val localColMap = localBlock.columns.associateBy { it.id }
                    val remoteColMap = remoteBlock.columns.associateBy { it.id }

                    // Base the new order on the incoming remote blocks
                    val mergedColumns = remoteBlock.columns.map { remoteCol ->
                        val localCol = localColMap[remoteCol.id]
                        if (localCol != null) {
                            // Deletion wins. For name/type edits, Remote wins.
                            remoteCol.copy(isDeleted = localCol.isDeleted || remoteCol.isDeleted)
                        } else {
                            remoteCol
                        }
                    }.toMutableList()

                    // Append any local columns that the remote note doesn't know about yet
                    val missingLocalCols = localBlock.columns.filter { it.id !in remoteColMap }
                    mergedColumns.addAll(missingLocalCols)


                    // --- 2. ROW MERGE ---
                    val localRowMap = localBlock.rows.associateBy { it.id }
                    val remoteRowMap = remoteBlock.rows.associateBy { it.id }

                    val allRowIds = localRowMap.keys + remoteRowMap.keys

                    val mergedRows = allRowIds.mapNotNull { id ->
                        val localRow = localRowMap[id]
                        val remoteRow = remoteRowMap[id]

                        if (localRow != null && remoteRow != null) {
                            val isDeleted = localRow.isDeleted || remoteRow.isDeleted
                            val mergedCells = localRow.cells + remoteRow.cells
                            remoteRow.copy(cells = mergedCells, isDeleted = isDeleted)
                        } else {
                            localRow ?: remoteRow
                        }
                    }

                    // Return the fully merged database
                    remoteBlock.copy(columns = mergedColumns, rows = mergedRows)
                } else {
                    remoteBlock
                }
            } else {
                remoteBlock
            }
        }

        return NoteContent(blocks = mergedBlocks)
    }
}