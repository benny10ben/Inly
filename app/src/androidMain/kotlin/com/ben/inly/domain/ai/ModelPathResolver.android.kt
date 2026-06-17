package com.ben.inly.domain.ai

actual fun resolveModelPath(fileName: String): String = "/data/data/com.ben.inly/files/$fileName"