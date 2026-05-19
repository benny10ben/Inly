package com.ben.inly.presentation.shared.sync

import java.net.InetAddress
import java.util.UUID

actual fun getLocalNetworkIp(): String {
    return try {
        InetAddress.getLocalHost().hostAddress
    } catch (e: Exception) {
        "127.0.0.1"
    }
}

actual fun generateSecureToken(): String {
    return UUID.randomUUID().toString()
}