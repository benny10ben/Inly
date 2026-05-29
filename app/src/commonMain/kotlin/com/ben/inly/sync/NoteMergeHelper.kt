package com.ben.inly.sync

import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.markDeleted

object NoteMergeHelper {
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

        val mergedBlocks = remoteContent.blocks.map { remoteBlock ->
            val localBlock = localById[remoteBlock.id]

            val isDeletedInEither = (localBlock?.isDeleted == true) || remoteBlock.isDeleted

            val winner = when {
                remoteBlock is DatabaseBlock -> {
                    mergeDatabase(localBlock as? DatabaseBlock, remoteBlock, localUpdatedAt, remoteUpdatedAt)
                }
                localBlock == null -> {
                    remoteBlock
                }
                remoteUpdatedAt >= localUpdatedAt -> {
                    remoteBlock
                }
                else -> {
                    localBlock
                }
            }

            if (isDeletedInEither && !winner.isDeleted) winner.markDeleted() else winner

        }.toMutableList()

        val localOnlyBlocks = localContent.blocks.filter { it.id !in remoteBlockIds }
        if (localOnlyBlocks.isNotEmpty()) {
            val localOriginalOrder = localContent.blocks.map { it.id }

            localOnlyBlocks.forEach { localBlock ->
                val localIndex = localOriginalOrder.indexOf(localBlock.id)

                val precedingId = localOriginalOrder
                    .subList(0, localIndex)
                    .lastOrNull { id -> mergedBlocks.any { it.id == id } }

                val insertAfter = if (precedingId != null) {
                    mergedBlocks.indexOfFirst { it.id == precedingId }
                } else {
                    -1
                }

                mergedBlocks.add(insertAfter + 1, localBlock)
            }
        }

        return NoteContent(blocks = mergedBlocks)
    }

    private fun mergeDatabase(
        localBlock: DatabaseBlock?,
        remoteBlock: DatabaseBlock,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long
    ): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        val remoteWins = remoteUpdatedAt >= localUpdatedAt

        // Merge Columns
        val localColMap = localBlock.columns.associateBy { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }
        val allColIds = (localColMap.keys + remoteColMap.keys).distinct()

        val mergedColumns = allColIds.mapNotNull { id ->
            val localCol = localColMap[id]
            val remoteCol = remoteColMap[id]

            when {
                localCol != null && remoteCol != null -> {
                    val winnerCol = if (remoteWins) remoteCol else localCol
                    winnerCol.copy(isDeleted = localCol.isDeleted || remoteCol.isDeleted)
                }
                else -> localCol ?: remoteCol
            }
        }.toMutableList()

        // Merge Rows
        val localRowMap = localBlock.rows.associateBy { it.id }
        val remoteRowMap = remoteBlock.rows.associateBy { it.id }
        val allRowIds = (localRowMap.keys + remoteRowMap.keys).distinct()

        val mergedRows = allRowIds.mapNotNull { id ->
            val localRow = localRowMap[id]
            val remoteRow = remoteRowMap[id]

            when {
                localRow != null && remoteRow != null -> {
                    val mergedCells = if (remoteWins) {
                        localRow.cells + remoteRow.cells
                    } else {
                        remoteRow.cells + localRow.cells
                    }

                    val winnerRow = if (remoteWins) remoteRow else localRow
                    winnerRow.copy(
                        cells = mergedCells,
                        isDeleted = localRow.isDeleted || remoteRow.isDeleted
                    )
                }
                else -> localRow ?: remoteRow
            }
        }

        val winnerBlock = if (remoteWins) remoteBlock else localBlock
        return winnerBlock.copy(columns = mergedColumns, rows = mergedRows)
    }
}