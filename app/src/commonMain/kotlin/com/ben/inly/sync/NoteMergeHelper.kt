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
                    mergeDatabase(localBlock as? DatabaseBlock, remoteBlock)
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

    private fun mergeDatabase(localBlock: DatabaseBlock?, remoteBlock: DatabaseBlock): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        val localColMap = localBlock.columns.associateBy { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }

        val mergedColumns = remoteBlock.columns.map { remoteCol ->
            val localCol = localColMap[remoteCol.id]
            if (localCol != null) remoteCol.copy(isDeleted = localCol.isDeleted || remoteCol.isDeleted)
            else remoteCol
        }.toMutableList()

        localBlock.columns
            .filter { it.id !in remoteColMap }
            .forEach { mergedColumns.add(it) }

        val localRowMap = localBlock.rows.associateBy { it.id }
        val remoteRowMap = remoteBlock.rows.associateBy { it.id }
        val allRowIds = (localRowMap.keys + remoteRowMap.keys).distinct()

        val mergedRows = allRowIds.mapNotNull { id ->
            val localRow = localRowMap[id]
            val remoteRow = remoteRowMap[id]

            when {
                localRow != null && remoteRow != null -> {
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