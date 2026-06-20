package com.ben.inly

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.model.NoteContent
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.presentation.InlyApp
import com.ben.inly.ui.theme.InlyTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ben.inly.domain.sync.AutoSyncTrigger
import com.ben.inly.sync.discovery.SyncDiscoveryManager
import com.ben.inly.presentation.sync.SyncViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

class MainActivity : ComponentActivity() {

    private val repository: NoteRepository by inject()
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imagePickerCallback?.invoke(uri?.toString() ?: "") }

    private val pickDocument = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> documentPickerCallback?.invoke(uri?.toString() ?: "") }

    private var imagePickerCallback: ((String) -> Unit)? = null
    private var documentPickerCallback: ((String) -> Unit)? = null
    private var takePhotoCallback: ((String) -> Unit)? = null
    private var currentPhotoUri: Uri? = null

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            takePhotoCallback?.invoke(currentPhotoUri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !InlyApplication.isReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val syncViewModel: SyncViewModel by inject()
        val discoveryManager: SyncDiscoveryManager by inject()

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    syncViewModel.triggerAutoSync(discoveryManager)
                    syncViewModel.startForegroundWatchdog()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    syncViewModel.stopForegroundWatchdog()
                }
                else -> {}
            }
        })

        @OptIn(FlowPreview::class)
        lifecycleScope.launch {
            AutoSyncTrigger.syncRequests
                .debounce(1500L)
                .collect {
                    syncViewModel.triggerFastSync()
                }
        }

        setContent {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components {
                        add(KtorNetworkFetcherFactory(HttpClient(OkHttp)))
                    }
                    .crossfade(true)
                    .build()
            }

            InlyTheme {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    KoinAndroidContext {
                        InlyApp(
                            onPickImage = { callback ->
                                imagePickerCallback = callback
                                pickImage.launch("image/*")
                            },
                            onTakePhoto = { callback ->
                                takePhotoCallback = callback

                                val photoFile = java.io.File(this@MainActivity.filesDir, "camera_${UUID.randomUUID()}.jpg")
                                if (!photoFile.exists()) {
                                    photoFile.createNewFile()
                                }

                                currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "${applicationContext.packageName}.fileprovider",
                                    photoFile
                                )
                                takePhoto.launch(currentPhotoUri!!)
                            },
                            onPickDocument = { callback ->
                                documentPickerCallback = callback
                                pickDocument.launch("*/*")
                            },
                            onOpenFile = { filePath, mimeType ->
                                android.util.Log.d("FileOpen", "filePath=$filePath  mimeType=$mimeType")
                                try {
                                    val file = if (filePath.contains("/")) {
                                        java.io.File(filePath)
                                    } else {
                                        java.io.File(this@MainActivity.filesDir, filePath)
                                    }
                                    android.util.Log.d("FileOpen", "exists=${file.exists()} size=${file.length()}")
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${applicationContext.packageName}.fileprovider",
                                        file
                                    )

                                    var finalMimeType = mimeType
                                    if (finalMimeType == "*/*" || finalMimeType.isBlank()) {
                                        val extension = file.extension.lowercase()
                                        finalMimeType = android.webkit.MimeTypeMap.getSingleton()
                                            .getMimeTypeFromExtension(extension) ?: "*/*"
                                    }

                                    android.util.Log.d("FileOpen", "uri=$uri finalMimeType=$finalMimeType")
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, finalMimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(viewIntent, "Open file with..."))
                                } catch (e: Exception) {
                                    android.util.Log.e("FileOpen", "EXCEPTION: ${e::class.simpleName}: ${e.message}")
                                    Toast.makeText(this@MainActivity, "Failed to open file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

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

    private fun saveToInbox(sharedText: String) {
        val urlRegex = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))\\))+(?:\\((?:[^\\s()<>]+|\\([^\\s()<>]+\\))\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”指標’]))".toRegex()
        val extractedUrl = urlRegex.find(sharedText)?.value ?: sharedText

        Toast.makeText(this, "Saving to Inbox...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allNotes: List<NoteMetadataEntity> = repository.getAllNotes().first()
                var inboxNote: NoteMetadataEntity? = allNotes.find { it.title.equals("Inbox", ignoreCase = true) }

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

                val newBlock = BookmarkBlock(
                    id = UUID.randomUUID().toString(),
                    indentationLevel = 0,
                    url = extractedUrl,
                    title = "Loading preview...",
                    description = null,
                    previewImageUrl = null
                )

                val updatedBlocks = content.blocks + newBlock
                repository.saveNote(
                    inboxNote.copy(updatedAt = System.currentTimeMillis()),
                    NoteContent(blocks = updatedBlocks)
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved to Inbox!", Toast.LENGTH_SHORT).show()
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