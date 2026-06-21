package com.ben.inly.domain.util

import androidx.compose.runtime.Composable

/**
 * Multiplatform contract for voice-to-text recognition.
 */
interface VoiceRecognizer {
    fun startListening(onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit, onPermissionNeeded: () -> Unit)
    fun stopListening()
    fun destroy()
}