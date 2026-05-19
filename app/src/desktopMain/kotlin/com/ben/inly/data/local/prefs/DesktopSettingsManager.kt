package com.ben.inly.data.local.prefs

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopSettingsManager : SettingsManager {
    private val prefs = Preferences.userRoot().node("com.ben.inly.settings")

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

    override fun getSyncAuthToken(): String {
        return prefs.get(SyncConstants.KEY_SYNC_AUTH_TOKEN, "")
    }

    override fun saveSyncAuthToken(token: String) {
        prefs.put(SyncConstants.KEY_SYNC_AUTH_TOKEN, token)
    }

    override fun getSyncIpAddress(): String {
        return prefs.get(SyncConstants.KEY_SYNC_IP_ADDRESS, "")
    }

    override fun saveSyncIpAddress(ip: String) {
        prefs.put(SyncConstants.KEY_SYNC_IP_ADDRESS, ip)
    }

    override fun getSyncPort(): Int {
        return prefs.getInt(SyncConstants.KEY_SYNC_PORT, SyncConstants.DEFAULT_PORT)
    }

    override fun saveSyncPort(port: Int) {
        prefs.putInt(SyncConstants.KEY_SYNC_PORT, port)
    }

    override fun getSyncEncryptionKey(): String {
        return prefs.get(SyncConstants.KEY_SYNC_ENCRYPTION_KEY, "")
    }

    override fun saveSyncEncryptionKey(key: String) {
        prefs.put(SyncConstants.KEY_SYNC_ENCRYPTION_KEY, key)
    }
}