package com.ben.inly.domain.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Helps safely copy external files (like gallery images or PDFs) directly into the app's internal storage.
 */
class MediaStorageHelper(private val context: Context) {

    /**
     * Takes a file the user picked via the OS picker, extracts its name and size,
     * and copies the actual file data into a secure local directory.
     * This ensures the app's notes don't break if the user later deletes the original file from their phone.
     */
    fun copyUriToInternalStorage(uri: Uri): MediaInfo? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "Unknown_File"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = displayName.substringAfterLast('.', "")
            val localFileName = "media_${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"

            val file = File(context.filesDir, localFileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            MediaInfo(
                localFileName = localFileName,
                originalName = displayName,
                mimeType = mimeType,
                sizeBytes = size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * A simple wrapper to hold the file's details, including a handy property
 * to instantly format the file size into a readable string (like '2.4 MB').
 */
data class MediaInfo(
    val localFileName: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}