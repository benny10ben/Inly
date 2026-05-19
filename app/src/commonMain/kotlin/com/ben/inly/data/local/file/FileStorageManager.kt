package com.ben.inly.data.local.file

import com.ben.inly.domain.model.NoteContent

/**
 * Multiplatform contract for reading and writing encrypted note blocks.
 * Android handles this via EncryptedFile. Desktop will handle this via local encrypted IO.
 */
interface FileStorageManager {
    suspend fun saveNoteContent(fileName: String, content: NoteContent)
    suspend fun readNoteContent(fileName: String): NoteContent?
    suspend fun deleteNoteContent(fileName: String): Boolean
}