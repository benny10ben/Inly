package com.ben.inly.domain.selfhost

import android.util.Log

actual object SelfHostSyncLog {
    private const val TAG = "InlySyncEngine"

    actual fun d(message: String) {
        Log.d(TAG, message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
