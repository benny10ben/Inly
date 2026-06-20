package com.ben.inly.di

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cash.sqldelight.db.SqlDriver
import com.ben.inly.core.security.AesGcmEncryptionManager
import com.ben.inly.core.security.EncryptionManager
import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.data.local.prefs.AndroidSettingsManager
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.data.local.room.BlockDao
import com.ben.inly.data.local.room.BookmarkBlockDao
import com.ben.inly.data.local.room.CalendarTaskDao
import com.ben.inly.data.local.room.DocumentBlockDao
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.ImageBlockDao
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.local.room.TagDao
import com.ben.inly.data.sync.SyncRepositoryImpl
import com.ben.inly.database.DatabaseDriverFactory
import com.ben.inly.domain.repository.RagRepository
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.util.AndroidAudioRecorder
import com.ben.inly.domain.util.AndroidMediaStorageHelper
import com.ben.inly.domain.util.AndroidTaskExtractor
import com.ben.inly.domain.util.AudioRecorder
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.NativeVoiceRecognizer
import com.ben.inly.domain.util.TaskExtractor
import com.ben.inly.domain.util.VoiceRecognizer
import com.ben.inly.presentation.rag.RagViewModel
import com.ben.inly.presentation.reminders.AndroidReminderScheduler
import com.ben.inly.presentation.reminders.ReminderScheduler
import com.ben.inly.presentation.sync.SyncViewModel
import com.ben.inly.sync.discovery.AndroidDiscoveryManager
import com.ben.inly.sync.discovery.SyncDiscoveryManager
import com.inly.database.InlyDatabase
import net.sqlcipher.database.SupportFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {

    // Platform implementations
    single<MediaStorageHelper> { AndroidMediaStorageHelper(androidContext()) }
    single<VoiceRecognizer> { NativeVoiceRecognizer(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<AudioRecorder> { AndroidAudioRecorder(androidContext()) }
    single<TaskExtractor> { AndroidTaskExtractor() }

    single<SharedPreferences> {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            "inly_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single<SettingsManager> { AndroidSettingsManager(sharedPreferences = get()) }

    // Room
    single<ByteArray> { EncryptionManager.getDatabasePassphrase(androidContext()) }

    single<AppDatabase> {
        val passphrase = get<ByteArray>()
        val supportFactory = SupportFactory(passphrase)

        val builder = com.ben.inly.data.local.room.getDatabaseBuilder(androidContext())
        builder
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration()

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

    // SQLDelight
    single<SqlDriver> { DatabaseDriverFactory(androidContext()).createDriver() }
    single { InlyDatabase(get()) }

    // AI
    single { com.ben.inly.domain.ai.LocalAiEngine() }
    single { RagRepository(database = get(), aiEngine = get()) }
    viewModel { RagViewModel(ragRepository = get()) }

    // Sync
    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<SyncDiscoveryManager> { AndroidDiscoveryManager(androidContext()) }
    single<com.ben.inly.sync.SyncClient> { com.ben.inly.sync.SyncClient(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get()) }
    viewModel { SyncViewModel(get(), get()) }
}