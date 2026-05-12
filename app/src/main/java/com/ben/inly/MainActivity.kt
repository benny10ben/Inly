package com.ben.inly

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.presentation.InlyApp
import com.ben.inly.ui.theme.InlyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Injecting the repository directly here so the activity can save incoming shared links
    // without needing to spin up a full UI ViewModel.
    @Inject
    lateinit var repository: NoteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if the app was launched by a user sharing a link from another app
        handleIntent(intent)

        setContent {
            InlyTheme {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    InlyApp()
                }
            }
        }
    }

    /**
     * Catches external share intents when the app is already open in the background.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                saveToInbox(sharedText)
            }
        }
    }

    /**
     * Parses incoming text to find URLs, creates an Inbox note if one doesn't exist,
     * and drops the link directly into the Inbox.
     */
    private fun saveToInbox(sharedText: String) {
        val urlRegex = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))\\))+(?:\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))".toRegex()
        val extractedUrl = urlRegex.find(sharedText)?.value ?: sharedText

        Toast.makeText(this, "Saving to Inbox...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allNotes = repository.getAllStandaloneNotes().first()
                var inboxNote = allNotes.find { it.title.equals("Inbox", ignoreCase = true) }

                val noteId: String
                val content: NoteContent

                if (inboxNote == null) {
                    noteId = UUID.randomUUID().toString()
                    inboxNote = NoteMetadataEntity(
                        noteId = noteId,
                        title = "Inbox",
                        icon = "📥",
                        folderId = null,
                        isDaily = false,
                        dateString = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        filePath = "note_$noteId.json",
                        snippet = "Saved links and ideas."
                    )
                    content = NoteContent(blocks = emptyList())
                } else {
                    noteId = inboxNote.noteId
                    content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
                }

                // Create the bookmark block with a trigger title so the background worker
                // knows it needs to fetch the rich website preview metadata later.
                val newBlock = BookmarkBlock(
                    id = UUID.randomUUID().toString(),
                    indentationLevel = 0,
                    url = extractedUrl,
                    title = "Loading preview...",
                    description = null,
                    previewImageUrl = null
                )

                val updatedBlocks = content.blocks + newBlock
                repository.saveStandaloneNote(
                    inboxNote.copy(updatedAt = System.currentTimeMillis()),
                    NoteContent(blocks = updatedBlocks)
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved to Inbox!", Toast.LENGTH_SHORT).show()
                    // If the user used the Android Share Sheet, close the transparent activity instantly
                    // so they aren't ripped out of their current app.
                    if (intent?.action == Intent.ACTION_SEND) {
                        finish()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to save link.", Toast.LENGTH_SHORT).show()
                    if (intent?.action == Intent.ACTION_SEND) finish()
                }
            }
        }
    }
}