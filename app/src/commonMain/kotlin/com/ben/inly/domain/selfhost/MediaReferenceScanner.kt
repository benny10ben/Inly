package com.ben.inly.domain.selfhost

import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.RowContainerBlock
import com.ben.inly.domain.model.VoiceBlock

object MediaReferenceScanner {

    fun extractMediaFileNames(blocks: List<NoteBlock>): Set<String> {
        val fileNames = mutableSetOf<String>()
        scan(blocks, fileNames)
        return fileNames
    }

    private fun scan(blocks: List<NoteBlock>, into: MutableSet<String>) {
        blocks.forEach { block ->
            when (block) {
                is ImageBlock -> block.localFilePath?.substringAfterLast("/")?.let { into.add(it) }
                is DocumentBlock -> block.localFilePath?.substringAfterLast("/")?.let { into.add(it) }
                is VoiceBlock -> block.localFilePath?.substringAfterLast("/")?.let { into.add(it) }
                is RowContainerBlock -> block.columns.forEach { column -> scan(column.blocks, into) }
                else -> Unit
            }
        }
    }
}
