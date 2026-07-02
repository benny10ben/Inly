package com.ben.inly.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.repository.NoteRepositoryImpl
import com.ben.inly.domain.repository.NoteIndexer
import com.ben.inly.domain.util.HeuristicTaskExtractor
import com.ben.inly.domain.util.TaskExtractor
import com.ben.inly.presentation.mobile.daily.DailyEditorViewModel
import com.ben.inly.presentation.search.SearchViewModel
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

    single<com.ben.inly.domain.repository.BackupRepository> {
        com.ben.inly.domain.repository.BackupRepositoryImpl(
            noteDao = get(),
            folderDao = get(),
            tagDao = get(),
            blockDao = get(),
            calendarTaskDao = get(),
            imageBlockDao = get(),
            documentBlockDao = get(),
            bookmarkBlockDao = get()
        )
    }

    viewModel {
        com.ben.inly.presentation.settings.SettingsViewModel(
            backupRepository = get(),
            noteRepository = get(),
            settingsManager = get(),
            backupRescheduler = get()
        )
    }

    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.HomeViewModel(
            repository = get(),
            settingsManager = get(),
            reminderScheduler = get(),
            taskExtractor = get(),
            voiceRecognizer = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.reminders.RemindersViewModel(
            repository = get(),
            reminderScheduler = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.images.ImagesViewModel(
            repository = get(),
            mediaStorageHelper = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.documents.DocumentsViewModel(
            repository = get(),
            mediaStorageHelper = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.overview.bookmarks.BookmarksViewModel(
            repository = get()
        )
    }
    viewModel {
        _root_ide_package_.com.ben.inly.presentation.mobile.home.note.NoteEditorViewModel(
            repository = get(),
            mediaStorageHelper = get(),
            reminderScheduler = get(),
            audioRecorder = get(),
            settingsManager = get()
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
    viewModel { SearchViewModel(repository = get()) }
    single<TaskExtractor> { HeuristicTaskExtractor() }
}