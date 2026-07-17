package com.ben.inly.domain.selfhost.sync

import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.domain.selfhost.webdav.WebDavSyncClient
import com.ben.inly.domain.selfhost.webdav.WebDavSyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class ForegroundSyncPoller(
    private val webDavSyncClient: WebDavSyncClient,
    private val selfHostSyncEngine: SelfHostSyncEngine,
    private val settingsManager: SettingsManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val manifestJson = Json { ignoreUnknownKeys = true }
    private var pollingJob: Job? = null

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL)
                try {
                    pollOnce()
                } catch (cause: Exception) {
                    SelfHostSyncLog.e("ForegroundSyncPoller: poll failed, will retry next interval", cause)
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollOnce() {
        val supportsETags = settingsManager.getSelfHostSupportsETags()
            ?: webDavSyncClient.checkETagSupport().also { settingsManager.saveSelfHostSupportsETags(it) }

        val changed = if (supportsETags) checkViaETag() else checkViaTimestamp()
        if (changed) {
            SelfHostSyncLog.d("ForegroundSyncPoller: remote manifest changed, triggering text sync then media sync")
            selfHostSyncEngine.runSync()
            selfHostSyncEngine.syncMedia()
        }
    }

    private suspend fun checkViaETag(): Boolean {
        val currentEtag = webDavSyncClient.getResourceInfo(WebDavSyncPaths.MANIFEST_FILE)?.etag ?: return false
        val previousEtag = settingsManager.getSelfHostManifestEtag()
        settingsManager.saveSelfHostManifestEtag(currentEtag)
        return previousEtag == null || previousEtag != currentEtag
    }

    private suspend fun checkViaTimestamp(): Boolean {
        val raw = webDavSyncClient.downloadAndDecryptJson(WebDavSyncPaths.MANIFEST_FILE) ?: return false
        val manifest = try {
            manifestJson.decodeFromString(SelfHostManifest.serializer(), raw)
        } catch (cause: Exception) {
            SelfHostSyncLog.e("ForegroundSyncPoller: could not parse manifest during poll", cause)
            return false
        }
        val lastSyncTimestamp = settingsManager.getSelfHostLastSyncTimestamp()
        return manifest.entries.any { it.updatedAt > lastSyncTimestamp }
    }

    private companion object {
        val POLL_INTERVAL = 120.seconds
    }
}
