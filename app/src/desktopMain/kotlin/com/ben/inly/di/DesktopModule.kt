package com.ben.inly.di

import com.ben.inly.core.security.AesGcmEncryptionManager
import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.data.local.prefs.DesktopSettingsManager
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.data.local.room.BlockDao
import com.ben.inly.data.local.room.BookmarkBlockDao
import com.ben.inly.data.local.room.CalendarTaskDao
import com.ben.inly.data.local.room.CategoryDao
import com.ben.inly.data.local.room.DatabaseTemplateDao
import com.ben.inly.data.local.room.DocumentBlockDao
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.ImageBlockDao
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.sync.SyncRepositoryImpl
import com.ben.inly.data.worker.DesktopBackupRescheduler
import com.ben.inly.data.worker.BackupRescheduler
import com.ben.inly.database.DatabaseDriverFactory
import com.ben.inly.domain.ai.LocalAiEngine
import com.ben.inly.domain.repository.RagRepository
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.DesktopAudioRecorder
import com.ben.inly.domain.util.DesktopMediaStorageHelper
import com.ben.inly.domain.util.DesktopVoiceRecognizer
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.VoiceRecognizer
import com.ben.inly.presentation.rag.RagViewModel
import com.ben.inly.presentation.reminders.DesktopReminderScheduler
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.sync.discovery.DesktopDiscoveryManager
import com.ben.inly.sync.discovery.SyncDiscoveryManager
import com.inly.database.InlyDatabase
import org.koin.dsl.module

val desktopModule = module {

    // Room
    single<AppDatabase> {
        val builder = com.ben.inly.data.local.room.getDatabaseBuilder()
        builder.fallbackToDestructiveMigration(dropAllTables = true)
        com.ben.inly.data.local.room.getRoomDatabase(builder)
    }
    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<TagDao> { get<AppDatabase>().tagDao() }
    single<BlockDao> { get<AppDatabase>().blockDao() }
    single<CalendarTaskDao> { get<AppDatabase>().calendarTaskDao() }
    single<ImageBlockDao> { get<AppDatabase>().imageBlockDao() }
    single<DocumentBlockDao> { get<AppDatabase>().documentBlockDao() }
    single<BookmarkBlockDao> { get<AppDatabase>().bookmarkBlockDao() }
    single<DatabaseTemplateDao> { get<AppDatabase>().databaseTemplateDao() }
    single<CategoryDao> { get<AppDatabase>().categoryDao() }
    single<VoiceRecognizer> { DesktopVoiceRecognizer() }

    // SQLDelight
    single { DatabaseDriverFactory().createDriver() }
    single { InlyDatabase(get()) }

    // AI
    single { LocalAiEngine() }
    single { RagRepository(get(), get()) }
    factory { RagViewModel(get()) }

    // Platform implementations
    single<SettingsManager> { DesktopSettingsManager() }
    single<ReminderScheduler> { DesktopReminderScheduler() }
    single<MediaStorageHelper> { DesktopMediaStorageHelper() }
    single<AudioRecorder> { DesktopAudioRecorder() }

    // Sync
    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<SyncDiscoveryManager> { DesktopDiscoveryManager() }
    single<com.ben.inly.sync.SyncClient> { com.ben.inly.sync.SyncClient(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get()) }
    factory { SyncViewModel(get(), get()) }

    // Automatic Backup
    single<BackupRescheduler> { DesktopBackupRescheduler() }
}