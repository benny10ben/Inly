package com.ben.inly.domain.ai

import java.io.File

actual fun resolveModelPath(fileName: String): String {
    val modelsDir = File(System.getProperty("user.home"), ".inly/models")
    modelsDir.mkdirs()
    return File(modelsDir, fileName).absolutePath
}