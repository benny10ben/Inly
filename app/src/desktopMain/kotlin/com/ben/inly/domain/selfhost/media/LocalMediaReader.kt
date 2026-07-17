package com.ben.inly.domain.selfhost.media

import com.ben.inly.domain.selfhost.sync.SelfHostSyncLog
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

actual class LocalMediaReader {

    actual fun readBytes(path: String): ByteArray? {
        return try {
            val file = File(path)
            if (!file.exists()) {
                SelfHostSyncLog.e("LocalMediaReader: no file exists at path=$path")
                return null
            }
            file.readBytes()
        } catch (cause: FileNotFoundException) {
            SelfHostSyncLog.e("LocalMediaReader: file not found for path=$path", cause)
            null
        } catch (cause: IOException) {
            SelfHostSyncLog.e("LocalMediaReader: IO error reading path=$path", cause)
            null
        }
    }
}