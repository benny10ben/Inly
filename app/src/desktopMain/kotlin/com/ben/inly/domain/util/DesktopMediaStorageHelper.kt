package com.ben.inly.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Desktop implementation to copy external files into the app's internal storage directory.
 */
class DesktopMediaStorageHelper : MediaStorageHelper {

    private val mediaStorageDir = File(System.getProperty("user.home"), ".inly/media").apply {
        mkdirs()
    }

    override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(uriString)
            if (!sourceFile.exists()) return@withContext null

            val extension = sourceFile.extension
            val displayName = sourceFile.name
            val size = sourceFile.length()

            val localFileName = "media_${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
            val destFile = File(mediaStorageDir, localFileName)

            sourceFile.copyTo(destFile, overwrite = true)

            val mimeType = Files.probeContentType(destFile.toPath()) ?: "application/octet-stream"

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