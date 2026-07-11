package com.ben.inly.domain.selfhost

expect object SelfHostSyncLog {
    fun d(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
