package com.ben.inly.domain.selfhost.media

import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.VoiceBlock

object MediaReferenceScanner {

    fun extractMediaFileNames(blocks: List<NoteBlock>): Set<String> {
        val fileNames = mutableSetOf<String>()
        blocks.forEach { block ->
            when (block) {
                is ImageBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
                is DocumentBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
                is VoiceBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
                else -> Unit
            }
        }
        return fileNames
    }
}