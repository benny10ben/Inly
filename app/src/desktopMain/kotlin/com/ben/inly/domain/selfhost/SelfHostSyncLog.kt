package com.ben.inly.domain.selfhost

actual object SelfHostSyncLog {
    private const val TAG = "InlySyncEngine"

    actual fun d(message: String) {
        println("[$TAG] $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        println("[$TAG] $message")
        throwable?.printStackTrace()
    }
}
