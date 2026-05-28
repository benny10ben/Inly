package com.ben.inly.presentation.shared.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.sync.SyncClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncViewModel(
    private val syncRepository: SyncRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _syncStatus = MutableStateFlow("Idle")
    val syncStatus = _syncStatus.asStateFlow()

    fun triggerManualSync() {
        viewModelScope.launch {
            val ip = settingsManager.getSyncIpAddress()
            val token = settingsManager.getSyncAuthToken()

            if (ip.isBlank() || token.isBlank()) {
                _syncStatus.value = "Not Paired!"
                return@launch
            }

            _syncStatus.value = "Syncing..."

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

                settingsManager.saveLastSyncTimestamp(System.currentTimeMillis())
                _syncStatus.value = "Success!"

            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = "Failed: ${e.message}"
            }
        }
    }
}