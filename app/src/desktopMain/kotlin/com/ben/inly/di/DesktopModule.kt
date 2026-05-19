package com.ben.inly.di

import com.ben.inly.DesktopTaskExtractor
import com.ben.inly.DesktopVoiceRecognizer
import com.ben.inly.core.security.AesGcmEncryptionManager
import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.core.util.SecretLoader
import com.ben.inly.data.local.file.DesktopFileStorageManager
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.prefs.DesktopSettingsManager
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.domain.util.TaskExtractor
import com.ben.inly.domain.util.VoiceRecognizer
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.data.sync.SyncRepositoryImpl
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.util.DesktopAudioRecorder
import com.ben.inly.domain.util.DesktopMediaStorageHelper
import com.ben.inly.presentation.reminders.DesktopReminderScheduler
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.sync.discovery.DesktopDiscoveryManager
import com.ben.inly.sync.discovery.SyncDiscoveryManager
import org.koin.dsl.module

val desktopModule = module {
    single<FileStorageManager> { DesktopFileStorageManager(SecretLoader.getEncryptionKey()) }

    single<AppDatabase> {
        val builder = com.ben.inly.data.local.room.getDatabaseBuilder()
        com.ben.inly.data.local.room.getRoomDatabase(builder)
    }

    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().folderDao() }
    single { get<AppDatabase>().tagDao() }

    single<SettingsManager> { DesktopSettingsManager() }
    single<ReminderScheduler> { DesktopReminderScheduler() }
    single<TaskExtractor> { DesktopTaskExtractor() }
    single<VoiceRecognizer> { DesktopVoiceRecognizer() }
    single<MediaStorageHelper> { DesktopMediaStorageHelper() }
    single<AudioRecorder> { DesktopAudioRecorder() }

    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<SyncDiscoveryManager> { DesktopDiscoveryManager() }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get()) }
    factory { SyncViewModel(get(), get()) }
}