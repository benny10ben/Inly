package com.ben.inly.domain.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AndroidMediaStorageHelper(private val context: Context) : MediaStorageHelper {

    override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = Uri.parse(uriString)
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

    override fun getAbsoluteMediaPath(fileName: String): String {
        return File(context.filesDir, fileName).absolutePath
    }
}