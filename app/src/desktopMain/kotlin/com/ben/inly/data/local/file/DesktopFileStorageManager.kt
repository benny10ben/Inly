package com.ben.inly.data.local.file

import com.ben.inly.domain.model.NoteContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DesktopFileStorageManager(
    private val encryptionKey: ByteArray
) : FileStorageManager {

    private val storageDir = File(System.getProperty("user.home"), ".inly/notes").apply { mkdirs() }

    private val jsonFormat = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val fileLock = Any()

    override suspend fun saveNoteContent(fileName: String, content: NoteContent) {
        synchronized(fileLock) {
            try {
                val file = File(storageDir, fileName)
                val jsonString = jsonFormat.encodeToString(content)
                val payload = jsonString.toByteArray(Charsets.UTF_8)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = ByteArray(12)
                SecureRandom().nextBytes(iv)
                val keySpec = SecretKeySpec(encryptionKey, "AES")
                val gcmSpec = GCMParameterSpec(128, iv)

                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
                val encryptedData = cipher.doFinal(payload)

                val path = file.toOkioPath()
                FileSystem.SYSTEM.sink(path).buffer().use { sink ->
                    sink.write(iv)
                    sink.write(encryptedData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun readNoteContent(fileName: String): NoteContent? {
        synchronized(fileLock) {
            val file = File(storageDir, fileName)
            if (!file.exists()) return null

            return try {
                val path = file.toOkioPath()
                val fileBytes = FileSystem.SYSTEM.source(path).buffer().use { source ->
                    source.readByteArray()
                }

                if (fileBytes.size < 12) return null

                val iv = fileBytes.copyOfRange(0, 12)
                val encryptedData = fileBytes.copyOfRange(12, fileBytes.size)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keySpec = SecretKeySpec(encryptionKey, "AES")
                val gcmSpec = GCMParameterSpec(128, iv)

                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
                val decryptedBytes = cipher.doFinal(encryptedData)

                val jsonString = String(decryptedBytes, Charsets.UTF_8)
                jsonFormat.decodeFromString<NoteContent>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun deleteNoteContent(fileName: String): Boolean {
        val file = File(storageDir, fileName)
        return if (file.exists()) file.delete() else true
    }
}