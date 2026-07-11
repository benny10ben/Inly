package com.ben.inly.domain.selfhost

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

actual class LocalMediaReader(private val context: Context) {

    actual fun readBytes(path: String): ByteArray? {
        return try {
            if (path.startsWith("content://")) {
                readFromContentUri(path)
            } else {
                readFromPlainFile(path)
            }
        } catch (cause: FileNotFoundException) {
            SelfHostSyncLog.e("LocalMediaReader: file not found for path=$path", cause)
            null
        } catch (cause: IOException) {
            SelfHostSyncLog.e("LocalMediaReader: IO error reading path=$path", cause)
            null
        } catch (cause: SecurityException) {
            SelfHostSyncLog.e("LocalMediaReader: permission denied reading path=$path", cause)
            null
        }
    }

    private fun readFromContentUri(path: String): ByteArray? {
        val uri = Uri.parse(path)
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: run {
                SelfHostSyncLog.e("LocalMediaReader: contentResolver returned null stream for uri=$path")
                null
            }
    }

    private fun readFromPlainFile(path: String): ByteArray? {
        val file = File(path)
        if (!file.exists()) {
            SelfHostSyncLog.e("LocalMediaReader: no file exists at path=$path")
            return null
        }
        return file.readBytes()
    }
}
