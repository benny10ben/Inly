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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ben.inly.data.worker.BackupScheduler
import com.ben.inly.domain.model.BulletedListBlock
import com.ben.inly.domain.model.CheckboxBlock
import com.ben.inly.domain.model.CodeBlock
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.model.HeadingBlock
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.model.NoteBlock
import com.ben.inly.domain.model.NumberedListBlock
import com.ben.inly.domain.model.QuoteBlock
import com.ben.inly.domain.model.SolidDividerBlock
import com.ben.inly.domain.model.TextBlock
import com.ben.inly.domain.util.generateAndSaveAndroidPdf


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

    private val mediaStorageHelper: com.ben.inly.domain.util.MediaStorageHelper by inject()

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            takePhotoCallback?.invoke(currentPhotoUri.toString())
        }
    }

    private val settingsViewModel: com.ben.inly.presentation.settings.SettingsViewModel by inject()
    private val backupScheduler: BackupScheduler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !InlyApplication.isReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        backupScheduler.toString()

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
                        val context = LocalContext.current

                        val backupFolderPickerLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocumentTree()
                        ) { uri ->
                            uri?.let {
                                try {
                                    // Take persistable permission so the background worker can use it forever
                                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    context.contentResolver.takePersistableUriPermission(it, takeFlags)

                                    // Save the URI and turn on the toggle in the ViewModel
                                    settingsViewModel.setBackupDirectory(it.toString())
                                    settingsViewModel.setAutoBackupEnabled(true)

                                    Toast.makeText(context, "Backup folder linked!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to link folder.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // State to hold payloads while the OS file picker is open
                        var pendingMarkdownContent by remember { mutableStateOf("") }
                        var pendingPdfTitle by remember { mutableStateOf("") }
                        var pendingPdfBlocks by remember { mutableStateOf<List<NoteBlock>>(emptyList()) }

                        // Markdown Saver
                        val exportMarkdownLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("text/markdown")
                        ) { uri ->
                            uri?.let {
                                try {
                                    context.contentResolver.openOutputStream(it)?.use { stream ->
                                        stream.write(pendingMarkdownContent.toByteArray())
                                    }
                                    Toast.makeText(context, "Markdown saved", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // PDF Saver
                        val exportPdfLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/pdf")
                        ) { uri ->
                            uri?.let { generateAndSaveAndroidPdf(context, it, pendingPdfTitle, pendingPdfBlocks, mediaStorageHelper) }
                        }

                        var pendingBackupJson by remember { mutableStateOf("") }
                        val exportBackupLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/zip")
                        ) { uri ->
                            uri?.let { destinationUri ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val filesDir = context.filesDir
                                        val exporter = com.ben.inly.domain.util.AndroidBackupExporter(context)
                                        exporter.exportToZip(destinationUri, pendingBackupJson, filesDir)

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        // BACKUP IMPORT LAUNCHER
                        val importBackupLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            uri?.let { sourceUri ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val filesDir = context.filesDir
                                        val exporter = com.ben.inly.domain.util.AndroidBackupExporter(context)

                                        // Unzip and extract JSON
                                        val jsonString = exporter.importFromZip(sourceUri, filesDir)

                                        if (jsonString != null) {
                                            // Send JSON directly to our commonMain ViewModel to handle the merge!
                                            settingsViewModel.mergeBackupJson(jsonString)

                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }

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
                                try {
                                    // Use our smart helper to perfectly locate the file!
                                    val absolutePath = mediaStorageHelper.getAbsoluteMediaPath(filePath)
                                    val file = java.io.File(absolutePath)

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

                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, finalMimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(viewIntent, "Open file with..."))
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Failed to open file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onExportMarkdown = { fileName, content ->
                                pendingMarkdownContent = content
                                exportMarkdownLauncher.launch(fileName)
                            },
                            onExportPdf = { fileName, title, blocks ->
                                pendingPdfTitle = title
                                pendingPdfBlocks = blocks
                                exportPdfLauncher.launch(fileName)
                            },
                            onExportBackup = { jsonContent ->
                                pendingBackupJson = jsonContent
                                val fileName = "InlyBackup_${System.currentTimeMillis()}.inly"
                                exportBackupLauncher.launch(fileName)
                            },
                            onImportBackupClick = {
                                importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                            },
                            onRequestBackupFolder = {
                                backupFolderPickerLauncher.launch(null)
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
        val extractedUrl = android.util.Patterns.WEB_URL.matcher(sharedText).let {
            if (it.find()) it.group() else sharedText
        }.trim()

        Toast.makeText(this, "Saved to Inbox!", Toast.LENGTH_SHORT).show()

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

                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                    try {
                        val metadata = com.ben.inly.domain.util.HtmlMetadataFetcher.fetchMetadata(extractedUrl)
                        if (metadata.description == "Could not load preview") return@launch

                        val currentContent = repository.getNoteContent(noteId) ?: return@launch

                        val finalizedBlocks = currentContent.blocks.map {
                            if (it.id == newBlock.id && it is BookmarkBlock) {
                                it.copy(
                                    title = metadata.title ?: "Unknown Link",
                                    description = metadata.description,
                                    previewImageUrl = metadata.imageUrl,
                                    updatedAt = System.currentTimeMillis()
                                )
                            } else it
                        }

                        repository.saveNote(
                            inboxNote.copy(updatedAt = System.currentTimeMillis()),
                            NoteContent(blocks = finalizedBlocks)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
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