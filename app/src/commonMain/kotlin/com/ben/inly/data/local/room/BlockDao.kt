package com.ben.inly.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockDao {
    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId AND isDeleted = 0 ORDER BY displayOrder ASC")
    suspend fun getBlocksForNote(noteId: String): List<NoteBlockEntity>

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId")
    suspend fun getAllBlocksForNoteIncludingDeleted(noteId: String): List<NoteBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBlock(block: NoteBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBlocks(blocks: List<NoteBlockEntity>)

    @Query("UPDATE note_blocks SET isDeleted = 1, updatedAt = :timestamp WHERE blockId = :blockId")
    suspend fun markBlockDeleted(blockId: String, timestamp: Long)

    @Query("DELETE FROM note_blocks WHERE noteId = :noteId")
    suspend fun deleteAllBlocksForNote(noteId: String)
}