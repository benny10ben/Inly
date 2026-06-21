package com.ben.inly.domain.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberMicrophonePermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit