package com.ben.inly.data.local.prefs

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import java.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopSettingsManager : SettingsManager {
    // Standard unencrypted preferences for basic app state
    private val prefs = Preferences.userRoot().node("com.ben.inly.settings")

    // The identifier that will show up in the OS Keychain/Credential Manager
    private val serviceName = "InlyAppVault"

    // Lazily initialize the native OS keyring bridge
    private val keyring by lazy {
        try {
            Keyring.create()
        } catch (e: Exception) {
            null // Fallback if the OS doesn't have an active secret service (e.g. headless Linux)
        }
    }

    private val _sortType = MutableStateFlow(prefs.get(SyncConstants.KEY_SORT_TYPE, SyncConstants.DEFAULT_SORT_TYPE))
    private val _sortOrder = MutableStateFlow(prefs.get(SyncConstants.KEY_SORT_ORDER, SyncConstants.DEFAULT_SORT_ORDER))
    private val _lastOpenedState = MutableStateFlow(prefs.get(SyncConstants.KEY_LAST_OPENED_STATE, ""))

    override val sortTypeFlow: Flow<String> = _sortType
    override val sortOrderFlow: Flow<String> = _sortOrder
    override val lastOpenedDesktopStateFlow: Flow<String> = _lastOpenedState

    override fun saveSortSettings(type: String, order: String) {
        prefs.put(SyncConstants.KEY_SORT_TYPE, type)
        prefs.put(SyncConstants.KEY_SORT_ORDER, order)
        _sortType.value = type
        _sortOrder.value = order
    }

    override fun saveLastOpenedDesktopState(state: String) {
        prefs.put(SyncConstants.KEY_LAST_OPENED_STATE, state)
        _lastOpenedState.value = state
    }

    override fun getLastSyncTimestamp(): Long {
        return prefs.getLong(SyncConstants.KEY_SYNC_TIMESTAMP, 0L)
    }

    override fun saveLastSyncTimestamp(timestamp: Long) {
        prefs.putLong(SyncConstants.KEY_SYNC_TIMESTAMP, timestamp)
    }

    // --- SECURE STORAGE IMPLEMENTATION ---

    private fun saveSecureString(account: String, secret: String) {
        try {
            keyring?.setPassword(serviceName, account, secret)
        } catch (e: Exception) {
            // Fallback to obfuscated prefs if the OS keyring rejects the request
            prefs.put("SECURE_$account", secret)
        }
    }

    private fun getSecureString(account: String): String {
        return try {
            keyring?.getPassword(serviceName, account) ?: prefs.get("SECURE_$account", "")
        } catch (e: PasswordAccessException) {
            prefs.get("SECURE_$account", "")
        } catch (e: Exception) {
            ""
        }
    }

    // Route sensitive Auth and Encryption keys to the OS Keyring
    override fun getSyncAuthToken(): String = getSecureString(SyncConstants.KEY_SYNC_AUTH_TOKEN)
    override fun saveSyncAuthToken(token: String) = saveSecureString(SyncConstants.KEY_SYNC_AUTH_TOKEN, token)

    override fun getSyncEncryptionKey(): String = getSecureString(SyncConstants.KEY_SYNC_ENCRYPTION_KEY)
    override fun saveSyncEncryptionKey(key: String) = saveSecureString(SyncConstants.KEY_SYNC_ENCRYPTION_KEY, key)

    // Route non-sensitive connection details to standard prefs
    override fun getSyncIpAddress(): String = prefs.get(SyncConstants.KEY_SYNC_IP_ADDRESS, "")
    override fun saveSyncIpAddress(ip: String) = prefs.put(SyncConstants.KEY_SYNC_IP_ADDRESS, ip)

    override fun getSyncPort(): Int = prefs.getInt(SyncConstants.KEY_SYNC_PORT, SyncConstants.DEFAULT_PORT)
    override fun saveSyncPort(port: Int) = prefs.putInt(SyncConstants.KEY_SYNC_PORT, port)
}