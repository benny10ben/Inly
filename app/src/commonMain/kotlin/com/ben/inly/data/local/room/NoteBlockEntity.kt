package com.ben.inly.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_blocks",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadataEntity::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class NoteBlockEntity(
    @PrimaryKey val blockId: String,
    val noteId: String,
    val displayOrder: Int,
    val blockDataJson: String,
    val updatedAt: Long,
    val isDeleted: Boolean
)