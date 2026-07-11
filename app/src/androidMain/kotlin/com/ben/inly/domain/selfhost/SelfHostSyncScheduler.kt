package com.ben.inly.domain.selfhost

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class SelfHostSyncScheduler(private val context: Context) {

    private val _isSyncActive = MutableStateFlow(false)
    actual val isSyncActive: StateFlow<Boolean> = _isSyncActive.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    actual val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val manualSyncObserver = Observer<List<WorkInfo>> { infos ->
        val active = infos.isNotEmpty() && !infos.all { it.state.isFinished }
        SelfHostSyncLog.d("Scheduler: manual sync WorkInfo update, states=${infos.map { it.state }}, active=$active")
        _isSyncActive.value = active

        val failedInfo = infos.firstOrNull { it.state == WorkInfo.State.FAILED }
        if (failedInfo != null) {
            val message = failedInfo.outputData.getString(SelfHostSyncWorker.KEY_ERROR_MESSAGE) ?: "Sync failed"
            SelfHostSyncLog.e("Scheduler: manual sync chain failed: $message")
            _syncError.value = message
        }
    }

    private val appCloseObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            scheduleDeferredSyncAfterAppClose()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appCloseObserver)
        mainHandler.post {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(WORK_NAME_MANUAL)
                .observeForever(manualSyncObserver)
        }
    }

    actual fun scheduleDailySync() {
        val request = PeriodicWorkRequestBuilder<SelfHostSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(anyNetworkConstraints())
            .setInputData(workDataOf(SelfHostSyncWorker.KEY_SYNC_SCOPE to SelfHostSyncWorker.SCOPE_TEXT))
            .addTag(TAG_TEXT_SYNC)
            .build()

        SelfHostSyncLog.d("Scheduler: enqueueing daily text sync (policy=UPDATE)")
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_DAILY, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    actual fun scheduleDeferredSyncAfterAppClose() {
        val request = OneTimeWorkRequestBuilder<SelfHostSyncWorker>()
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(anyNetworkConstraints())
            .setInputData(workDataOf(SelfHostSyncWorker.KEY_SYNC_SCOPE to SelfHostSyncWorker.SCOPE_TEXT))
            .addTag(TAG_TEXT_SYNC)
            .build()

        SelfHostSyncLog.d("Scheduler: enqueueing deferred post-close text sync")
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME_DEFERRED, ExistingWorkPolicy.REPLACE, request)
    }

    actual fun scheduleMediaSync() {
        val request = PeriodicWorkRequestBuilder<SelfHostSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(anyNetworkConstraints())
            .setInputData(workDataOf(SelfHostSyncWorker.KEY_SYNC_SCOPE to SelfHostSyncWorker.SCOPE_MEDIA))
            .addTag(TAG_MEDIA_SYNC)
            .build()

        SelfHostSyncLog.d("Scheduler: enqueueing periodic media sync (policy=UPDATE)")
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_MEDIA, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    actual fun syncNow() {
        _syncError.value = null

        val textRequest = OneTimeWorkRequestBuilder<SelfHostSyncWorker>()
            .setConstraints(anyNetworkConstraints())
            .setInputData(workDataOf(SelfHostSyncWorker.KEY_SYNC_SCOPE to SelfHostSyncWorker.SCOPE_TEXT))
            .addTag(TAG_TEXT_SYNC)
            .build()

        val mediaRequest = OneTimeWorkRequestBuilder<SelfHostSyncWorker>()
            .setConstraints(anyNetworkConstraints())
            .setInputData(workDataOf(SelfHostSyncWorker.KEY_SYNC_SCOPE to SelfHostSyncWorker.SCOPE_MEDIA))
            .addTag(TAG_MEDIA_SYNC)
            .build()

        SelfHostSyncLog.d("Scheduler: syncNow() chaining text sync -> media sync, media will not dispatch until text finishes")
        WorkManager.getInstance(context)
            .beginUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.REPLACE, textRequest)
            .then(mediaRequest)
            .enqueue()
    }

    actual fun cancelAll() {
        SelfHostSyncLog.d("Scheduler: cancelAll() cancelling all self-host WorkManager jobs")
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(WORK_NAME_DAILY)
            cancelUniqueWork(WORK_NAME_DEFERRED)
            cancelUniqueWork(WORK_NAME_MEDIA)
            cancelUniqueWork(WORK_NAME_MANUAL)
        }
        mainHandler.post {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(WORK_NAME_MANUAL)
                .removeObserver(manualSyncObserver)
        }
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appCloseObserver)
    }

    private fun anyNetworkConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val WORK_NAME_DAILY = "InlySelfHostDailyTextSync"
        const val WORK_NAME_DEFERRED = "InlySelfHostDeferredTextSync"
        const val WORK_NAME_MEDIA = "InlySelfHostMediaSync"
        const val WORK_NAME_MANUAL = "InlySelfHostManualSync"
        const val TAG_TEXT_SYNC = "InlySelfHostTextSync"
        const val TAG_MEDIA_SYNC = "InlySelfHostMediaSync"
    }
}
