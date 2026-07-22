package com.ben.inly.sync

import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NoteContent

object NoteMergeHelper {

    fun mergeNoteContent(
        localContent: NoteContent?,
        localUpdatedAt: Long,
        remoteContent: NoteContent,
        remoteUpdatedAt: Long
    ): NoteContent {
        if (localContent == null) return remoteContent

        // Strictly greater, not >= - on an exact-millisecond tie (two devices editing within the
        // same debounce window), remote otherwise always won regardless of which edit actually
        // landed second, silently discarding a local edit that was just as valid.
        val remoteWins = remoteUpdatedAt > localUpdatedAt
        val baseContent  = if (remoteWins) remoteContent else localContent
        val otherContent = if (remoteWins) localContent  else remoteContent
        val baseIds = baseContent.blocks.mapTo(HashSet()) { it.id }
        val otherById = otherContent.blocks.associateBy { it.id }
        val mergedTree = rebuildTree(baseContent.blocks, otherById)
        val otherOnly = otherContent.blocks.filter { it.id !in baseIds }

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

    /**
     * Walks base's list. For each block:
     *  - Database: field-level merge with its other-side twin.
     *  - leaf: pure last-write-wins by updatedAt, deleted or not - a block moved back to a note
     *    it was previously tombstoned in produces a genuinely newer alive write that must win
     *    over the stale tombstone, so deletion is never given priority independent of recency.
     */
    private fun rebuildTree(
        baseBlocks: List<NoteBlock>,
        otherById: Map<String, NoteBlock>
    ): List<NoteBlock> = baseBlocks.map { baseBlock ->
        when (baseBlock) {
            is DatabaseBlock -> {
                val twin = otherById[baseBlock.id] as? DatabaseBlock
                mergeDatabase(twin, baseBlock)
            }
            else -> {
                val other = otherById[baseBlock.id]
                when {
                    other == null -> baseBlock
                    // Strictly greater, not >= - ties keep whatever base already resolved to,
                    // instead of always handing an exact-millisecond tie to the other side.
                    other.updatedAt > baseBlock.updatedAt -> other
                    else -> baseBlock
                }
            }
        }
    }

    // database merge (unchanged logic, kept intact)

    private fun mergeDatabase(
        localBlock: DatabaseBlock?,
        remoteBlock: DatabaseBlock
    ): DatabaseBlock {
        if (localBlock == null) return remoteBlock

        // Strictly greater throughout this function, not >= - see mergeNoteContent's tie comment.
        val remoteBlockWins = remoteBlock.updatedAt > localBlock.updatedAt

        val localColMap  = localBlock.columns.associateBy  { it.id }
        val remoteColMap = remoteBlock.columns.associateBy { it.id }
        val allColIds    = (localColMap.keys + remoteColMap.keys).distinct()

        val mergedColumns = allColIds.mapNotNull { id ->
            val localCol  = localColMap[id]
            val remoteCol = remoteColMap[id]
            when {
                localCol != null && remoteCol != null -> {
                    val winnerCol = if (remoteCol.updatedAt > localCol.updatedAt) remoteCol else localCol
                    val loserCol  = if (remoteCol.updatedAt > localCol.updatedAt) localCol else remoteCol
                    winnerCol.copy(
                        isDeleted       = localCol.isDeleted || remoteCol.isDeleted,
                        aggregationType = winnerCol.aggregationType ?: loserCol.aggregationType,
                        currencySymbol  = winnerCol.currencySymbol  ?: loserCol.currencySymbol,
                        isFormulaCurrency = winnerCol.isFormulaCurrency || loserCol.isFormulaCurrency
                    )
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
                    val winnerRow = if (remoteRow.updatedAt > localRow.updatedAt) remoteRow else localRow
                    val mergedCells = if (remoteRow.updatedAt > localRow.updatedAt) {
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