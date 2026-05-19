package com.ben.inly.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.repository.NoteRepositoryImpl
import com.ben.inly.presentation.daily.DailyEditorViewModel
import com.ben.inly.presentation.notes.NotesViewModel
import com.ben.inly.presentation.notes.notes.StandaloneEditorViewModel
import com.ben.inly.presentation.notes.overview.bookmarks.BookmarksViewModel
import com.ben.inly.presentation.notes.overview.documents.DocumentsViewModel
import com.ben.inly.presentation.notes.overview.images.ImagesViewModel
import com.ben.inly.presentation.notes.overview.reminders.RemindersViewModel
import com.ben.inly.presentation.shared.trash.TrashViewModel

val sharedModule = module {
    // Repositories
    single<NoteRepository> {
        NoteRepositoryImpl(
            noteDao = get(),
            folderDao = get(),
            tagDao = get(),
            fileStorageManager = get(),
        )
    }

    // ViewModels
    viewModel {
        NotesViewModel(
            repository = get(),
            settingsManager = get(),
            reminderScheduler = get(),
            taskExtractor = get(),
            voiceRecognizer = get()
        )
    }
    viewModel { RemindersViewModel(repository = get(), reminderScheduler = get()) }
    viewModel { ImagesViewModel(repository = get(), mediaStorageHelper = get()) }
    viewModel { DocumentsViewModel(repository = get(), mediaStorageHelper = get()) }
    viewModel { BookmarksViewModel(repository = get()) }
    viewModel {
        StandaloneEditorViewModel(
            repository = get(),
            mediaStorageHelper = get(),
            reminderScheduler = get(),
            audioRecorder = get()
        )
    }
    viewModel {
        DailyEditorViewModel(
            repository = get(),
            mediaStorageHelper = get(),
            reminderScheduler = get(),
            audioRecorder = get()
        )
    }
    viewModel { TrashViewModel(repository = get()) }
}