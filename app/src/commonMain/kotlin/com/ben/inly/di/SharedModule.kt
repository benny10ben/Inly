package com.ben.inly.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.repository.NoteRepositoryImpl
import com.ben.inly.domain.repository.NoteIndexer
import com.ben.inly.presentation.tabs.daily.DailyEditorViewModel
import com.ben.inly.presentation.trash.TrashViewModel

val sharedModule = module {

    single {
        NoteIndexer(
            database = get(),
            aiEngine = get()
        )
    }

    single<NoteRepository> {
        NoteRepositoryImpl(
            noteDao = get(),
            folderDao = get(),
            tagDao = get(),
            blockDao = get(),
            noteIndexer = get(),
            calendarTaskDao = get(),
            imageBlockDao = get(),
            documentBlockDao = get(),
            bookmarkBlockDao = get()
        )
    }

    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.HomeViewModel(
            repository = get(),
            settingsManager = get(),
            reminderScheduler = get(),
            taskExtractor = get(),
            voiceRecognizer = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.reminders.RemindersViewModel(
            repository = get(),
            reminderScheduler = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.images.ImagesViewModel(
            repository = get(),
            mediaStorageHelper = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.documents.DocumentsViewModel(
            repository = get(),
            mediaStorageHelper = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.overview.bookmarks.BookmarksViewModel(
            repository = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.tabs.home.note.NoteEditorViewModel(
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