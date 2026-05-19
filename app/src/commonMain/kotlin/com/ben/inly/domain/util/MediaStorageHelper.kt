package com.ben.inly.domain.util

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

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
            val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
            val value = sizeBytes / 1024.0.pow(digitGroups.toDouble())
            val roundedValue = (value * 10.0).roundToInt() / 10.0
            return "$roundedValue ${units[digitGroups]}"
        }
}

/**
 * Multiplatform contract for copying external files into the app's internal storage.
 */
interface MediaStorageHelper {
    suspend fun copyUriToInternalStorage(uriString: String): MediaInfo?
}