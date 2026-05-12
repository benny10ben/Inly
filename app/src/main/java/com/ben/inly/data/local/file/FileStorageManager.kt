package com.ben.inly.data.local.file

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.ben.inly.domain.model.NoteContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles the secure, encrypted local storage of note content.
 */
class FileStorageManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val jsonFormat = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val fileLock = Any()

    /**
     * Encrypts and saves note data to the device.
     * The lock prevents crashes during rapid swiping.
     * Any existing file must be deleted first, as Android's EncryptedFile fails when trying to overwrite directly.
     */
    fun saveNoteContent(fileName: String, content: NoteContent) {
        synchronized(fileLock) {
            val file = File(context.filesDir, fileName)

            if (file.exists()) {
                file.delete()
            }

            try {
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    file,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()

                val jsonString = jsonFormat.encodeToString(content)

                encryptedFile.openFileOutput().use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Decrypts a saved note file back into a usable object.
     * Locked to prevent reading a file exactly while it's being updated.
     * Returns null if the file doesn't exist yet (e.g., on a brand new day).
     */
    fun readNoteContent(fileName: String): NoteContent? {
        synchronized(fileLock) {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null

            return try {
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    file,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()

                val jsonString = encryptedFile.openFileInput().use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }

                jsonFormat.decodeFromString<NoteContent>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}