package com.ben.inly.domain.util

import android.widget.Toast
import org.koin.mp.KoinPlatform

actual val isDesktopPlatform = false
actual fun showFeedback(message: String) {
    val context = KoinPlatform.getKoin().get<android.content.Context>()
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}