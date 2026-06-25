package com.ben.inly.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.sync.SyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(
    private val syncRepository: SyncRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _syncStatus = MutableStateFlow("Idle")
    val syncStatus = _syncStatus.asStateFlow()

    fun resetSyncStatus() {
        _syncStatus.value = "Idle"
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val ip = settingsManager.getSyncIpAddress()
            val token = settingsManager.getSyncAuthToken()

            if (ip.isBlank() || token.isBlank()) {
                _syncStatus.value = "Not Paired!"
                return@launch
            }

            _syncStatus.value = "Syncing..."

            val syncStart = System.currentTimeMillis()

            try {
                val client = SyncClient(settingsManager)

                val localChanges = syncRepository.collectLocalChanges()
                if (localChanges.isNotEmpty()) {
                    client.pushChanges(localChanges)
                }

                _syncStatus.value = "Fetching from Desktop..."
                val remoteChanges = client.fetchChanges()
                if (remoteChanges.isNotEmpty()) {
                    syncRepository.applyRemoteChanges(remoteChanges)
                }

                settingsManager.saveLastSyncTimestamp(syncStart)
                _syncStatus.value = "Success!"

            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = "Failed: ${e.message}"
            }
        }
    }

    fun triggerAutoSync(discoveryManager: com.ben.inly.sync.discovery.SyncDiscoveryManager) {
        viewModelScope.launch {
            val currentAuth = settingsManager.getSyncAuthToken()
            if (currentAuth.isBlank()) return@launch

            discoveryManager.startScanning()

            var foundNewIp = false
            for (i in 1..15) {
                kotlinx.coroutines.delay(200)
                val devices = discoveryManager.discoveredDevices.value
                if (devices.isNotEmpty()) {
                    settingsManager.saveSyncIpAddress(devices.first().ipAddress)
                    foundNewIp = true
                    break
                }
            }

            discoveryManager.stopScanning()

            performSilentSync()
        }
    }

    private suspend fun performSilentSync(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            _syncStatus.value = "Auto-Syncing..."
            val syncStart = System.currentTimeMillis()
            val client = SyncClient(settingsManager)

            val localChanges = syncRepository.collectLocalChanges()
            if (localChanges.isNotEmpty()) {
                client.pushChanges(localChanges)
            }

            val remoteChanges = client.fetchChanges()
            if (remoteChanges.isNotEmpty()) {
                syncRepository.applyRemoteChanges(remoteChanges)
            }

            settingsManager.saveLastSyncTimestamp(syncStart)

            _syncStatus.value = "Synced Successfully"
            true // Success
        } catch (e: java.net.ConnectException) {
            // Desktop is offline. Fail silently without stacktrace spam.
            _syncStatus.value = "Desktop Offline"
            false
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = "Sync Error: ${e.javaClass.simpleName}"
            false
        }
    }

    fun triggerFastSync() {
        viewModelScope.launch {
            val currentAuth = settingsManager.getSyncAuthToken()
            if (currentAuth.isBlank()) return@launch

            performSilentSync()
        }
    }

    private var watchdogJob: kotlinx.coroutines.Job? = null

    fun startForegroundWatchdog() {
        if (watchdogJob?.isActive == true) return

        watchdogJob = viewModelScope.launch {
            var currentDelay = 1500L
            val maxDelay = 30000L // Cap at 30 seconds

            while (true) {
                kotlinx.coroutines.delay(currentDelay)

                if (settingsManager.getSyncIpAddress().isNotBlank()) {
                    val success = performSilentSync()

                    if (success) {
                        // Reset to aggressive polling if the server is alive
                        currentDelay = 1500L
                    } else {
                        // Back off exponentially if the server is dead
                        currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                    }
                }
            }
        }
    }

    fun stopForegroundWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}