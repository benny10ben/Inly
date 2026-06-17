package com.ben.inly

import com.ben.inly.domain.util.TaskExtractor
import com.ben.inly.domain.util.VoiceRecognizer
import com.ben.inly.domain.util.TaskExtractionResult

class DesktopTaskExtractor : TaskExtractor {
    override fun extractTaskAndDate(transcript: String): TaskExtractionResult {
        TODO()
    }
}

class DesktopVoiceRecognizer : VoiceRecognizer {
    override fun startListening(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Stub
    }

    override fun stopListening() {
        // Stub
    }

    override fun destroy() {
        // Stub
    }
}