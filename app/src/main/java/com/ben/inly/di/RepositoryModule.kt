package com.ben.inly.di

import android.content.Context
import com.ben.inly.data.local.file.FileStorageManager
import com.ben.inly.data.local.room.FolderDao
import com.ben.inly.data.local.room.NoteDao
import com.ben.inly.data.repository.NoteRepositoryImpl
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.data.local.room.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * I keep the repository bindings in their own module to keep things organized.
 * This just tells Hilt to use NoteRepositoryImpl whenever a ViewModel asks for the NoteRepository interface.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        folderDao: FolderDao,
        tagDao: TagDao,
        fileStorageManager: FileStorageManager,
        @ApplicationContext context: Context
    ): NoteRepository {
        return NoteRepositoryImpl(noteDao, folderDao, tagDao, fileStorageManager, context)
    }
}