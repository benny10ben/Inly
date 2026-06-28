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

    // Automatic Backups
    val autoBackupEnabledFlow: Flow<Boolean>
    val backupFrequencyFlow: Flow<String>
    val backupDirectoryUriFlow: Flow<String?>

    fun saveAutoBackupEnabled(enabled: Boolean)
    fun saveBackupFrequency(frequency: String)
    fun saveBackupDirectory(uriString: String)
    val backupTimeFlow: Flow<String>
    val backupDayFlow: Flow<String>
    fun saveBackupTime(time: String)
    fun saveBackupDay(day: String)
}