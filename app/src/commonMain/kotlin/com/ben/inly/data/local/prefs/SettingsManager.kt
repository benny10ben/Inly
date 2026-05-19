package com.ben.inly.data.local.prefs

import kotlinx.coroutines.flow.Flow

/**
 * Multiplatform contract for handling user preferences.
 */
interface SettingsManager {
    val sortTypeFlow: Flow<String>
    val sortOrderFlow: Flow<String>
    val lastOpenedDesktopStateFlow: Flow<String>

    fun saveSortSettings(type: String, order: String)
    fun saveLastOpenedDesktopState(state: String)

    fun getLastSyncTimestamp(): Long
    fun saveLastSyncTimestamp(timestamp: Long)

    fun getSyncAuthToken(): String
    fun saveSyncAuthToken(token: String)

    fun getSyncIpAddress(): String
    fun saveSyncIpAddress(ip: String)

    fun getSyncPort(): Int
    fun saveSyncPort(port: Int)

    fun getSyncEncryptionKey(): String
    fun saveSyncEncryptionKey(key: String)
}