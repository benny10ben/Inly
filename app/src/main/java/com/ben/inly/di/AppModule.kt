package com.ben.inly.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ben.inly.core.security.EncryptionManager
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.domain.util.MediaStorageHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileStorageManager(@ApplicationContext context: Context): FileStorageManager {
        return FileStorageManager(context)
    }

    /**
     * Separate prefs file for user-facing settings (sort order etc).
     * Intentionally different from the secure prefs EncryptionManager uses internally.
     */
    @Provides
    @Singleton
    fun provideSettingsSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "inly_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideDatabasePassphrase(@ApplicationContext context: Context): ByteArray {
        return EncryptionManager.getDatabasePassphrase(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray
    ): AppDatabase {
        val supportFactory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "inly_metadata.db"
        )
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    @Singleton
    fun provideMediaStorageHelper(@ApplicationContext context: Context): MediaStorageHelper {
        return MediaStorageHelper(context)
    }
}