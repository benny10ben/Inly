package com.ben.inly.sync

import com.ben.inly.domain.model.ColumnBlock
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.model.RowContainerBlock
import com.ben.inly.domain.model.markDeleted

object NoteMergeHelper {

    fun mergeNoteContent(
        localContent: NoteContent?,
        localUpdatedAt: Long,
        remoteContent: NoteContent,
        remoteUpdatedAt: Long
    ): NoteContent {
        if (localContent == null) return remoteContent

        val remoteWins = remoteUpdatedAt >= localUpdatedAt
        val baseContent  = if (remoteWins) remoteContent else localContent
        val otherContent = if (remoteWins) localContent  else remoteContent
        val baseFlat  = flattenById(baseContent.blocks)
        val otherFlat  = flattenById(otherContent.blocks)
        val mergedTree = rebuildTree(baseContent.blocks, otherFlat, remoteWins)
        val mergedIds = collectIds(mergedTree)
        val otherOnly = otherContent.blocks.filter { it.id !in baseFlat && it.id !in mergedIds }

        val result = mergedTree.toMutableList()
        if (otherOnly.isNotEmpty()) {
            val otherOrder = otherContent.blocks.map { it.id }
            otherOnly.forEach { block ->
                val idx = otherOrder.indexOf(block.id)
                val precedingId = otherOrder.subList(0, idx)
                    .lastOrNull { id -> result.any { it.id == id } }
                val insertAfter = if (precedingId != null) {
                    result.indexOfFirst { it.id == precedingId }
                } else -1
                result.add(insertAfter + 1, block)
            }
        }

        return NoteContent(blocks = result.distinctBy { it.id })
    }

    // tree helpers
    /** Flattens the whole tree (descending into row columns) into id -> block. */
    private fun flattenById(blocks: List<NoteBlock>): Map<String, NoteBlock> {
        val out = HashMap<String, NoteBlock>()
        fun walk(list: List<NoteBlock>) {
            for (b in list) {
                out[b.id] = b
                if (b is RowContainerBlock) b.columns.forEach { walk(it.blocks) }
            }
        }
        walk(blocks)
        return out
    }

    /** All ids present anywhere in the tree. */
    private fun collectIds(blocks: List<NoteBlock>): Set<String> {
        val out = HashSet<String>()
        fun walk(list: List<NoteBlock>) {
            for (b in list) {
                out.add(b.id)
                if (b is RowContainerBlock) b.columns.forEach { walk(it.blocks) }
            }
        }
        walk(blocks)
        return out
    }

    /**
     * Walks base's tree. For each block:
     *  - RowContainer: recurse into its columns (structure preserved from base).
     *  - Database: field-level merge with its other-side twin.
     *  - leaf: pick the newer of {base, other} by updatedAt; honor tombstones.
     */
    private fun rebuildTree(
        baseBlocks: List<NoteBlock>,
        otherFlat: Map<String, NoteBlock>,
        remoteWins: Boolean
    ): List<NoteBlock> = baseBlocks.map { baseBlock ->
        when (baseBlock) {
            is RowContainerBlock -> {
                val newCols = baseBlock.columns.map { col ->
                    col.copy(blocks = rebuildTree(col.blocks, otherFlat, remoteWins))
                }
                baseBlock.copy(columns = newCols)
            }
            is DatabaseBlock -> {
                val twin = otherFlat[baseBlock.id] as? DatabaseBlock
                mergeDatabase(twin, baseBlock)
            }
            else -> {
                val other = otherFlat[baseBlock.id]
                val deletedInEither = baseBlock.isDeleted || (other?.isDeleted == true)
                val winner = when {
                    other == null -> baseBlock
                    baseBlock.updatedAt >= other.updatedAt -> baseBlock
                    else -> other
                }
                if (deletedInEither && !winner.isDeleted) winner.markDeleted() else winner
            }
        }
    }

    // database merge (unchanged logic, kept intact)

    private fun mergeDatabase(
        localBlock: DatabaseBlock?,
        remoteBlock: DatabaseBlock
    ): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        val remoteBlockWins = remoteBlock.updatedAt >= localBlock.updatedAt

        val localColMap  = localBlock.columns.associateBy  { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }
        val allColIds    = (localColMap.keys + remoteColMap.keys).distinct()

        val mergedColumns = allColIds.mapNotNull { id ->
            val localCol  = localColMap[id]
            val remoteCol = remoteColMap[id]
            when {
                localCol != null && remoteCol != null -> {
                    val winnerCol = if (remoteCol.updatedAt >= localCol.updatedAt) remoteCol else localCol
                    winnerCol.copy(isDeleted = localCol.isDeleted || remoteCol.isDeleted)
                }
                else -> localCol ?: remoteCol
            }
        }.toMutableList()

        val localRowMap  = localBlock.rows.associateBy  { it.id }
        val remoteRowMap = remoteBlock.rows.associateBy { it.id }
        val allRowIds    = (localRowMap.keys + remoteRowMap.keys).distinct()

        val mergedRows = allRowIds.mapNotNull { id ->
            val localRow  = localRowMap[id]
            val remoteRow = remoteRowMap[id]
            when {
                localRow != null && remoteRow != null -> {
                    val winnerRow = if (remoteRow.updatedAt >= localRow.updatedAt) remoteRow else localRow
                    val mergedCells = if (remoteRow.updatedAt >= localRow.updatedAt) {
                        localRow.cells + remoteRow.cells
                    } else {
                        remoteRow.cells + localRow.cells
                    }
                    winnerRow.copy(
                        cells    = mergedCells,
                        isDeleted = localRow.isDeleted || remoteRow.isDeleted
                    )
                }
                else -> localRow ?: remoteRow
            }
        }

        val winnerBlock = if (remoteBlockWins) remoteBlock else localBlock
        return winnerBlock.copy(
            columns   = mergedColumns,
            rows      = mergedRows,
            updatedAt = maxOf(localBlock.updatedAt, remoteBlock.updatedAt)
        )
    }
}