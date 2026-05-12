package com.ben.inly.presentation.shared.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.inly.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Trash screen.
 * Observes all soft-deleted notes and handles restoring them to their original folders
 * or permanently wiping their data and associated files from the device.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val trashedNotes = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            repository.restoreNote(noteId)
        }
    }

    fun permanentlyDelete(noteId: String, filePath: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId, filePath)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val currentTrash = trashedNotes.value
            currentTrash.forEach { note ->
                repository.deleteNote(note.noteId, note.filePath)
            }
        }
    }
}