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

    private val settingsViewModel: com.ben.inly.presentation.settings.SettingsViewModel by inject()

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
                        val context = LocalContext.current

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
                            uri?.let { generateAndSaveAndroidPdf(context, it, pendingPdfTitle, pendingPdfBlocks) }
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

    // Native Android PDF Generator
    private fun generateAndSaveAndroidPdf(
        context: android.content.Context,
        uri: Uri,
        title: String,
        blocks: List<NoteBlock>
    ) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = android.text.TextPaint().apply {
                textSize = 12f
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }

            val titlePaint = android.text.TextPaint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }

            var currentY = 70f
            val startX = 50f
            val endX = pageWidth - 50f
            val maxContentWidth = (endX - startX).toInt()
            val maxY = pageHeight - 50f

            fun checkPagination(neededHeight: Float) {
                if (currentY + neededHeight > maxY) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 70f
                }
            }

            // Draw Title
            val safeTitle = title.ifBlank { "Untitled Note" }
            val titleLayout = android.text.StaticLayout.Builder.obtain(safeTitle, 0, safeTitle.length, titlePaint, maxContentWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .build()

            checkPagination(titleLayout.height.toFloat())
            canvas.save()
            canvas.translate(startX, currentY)
            titleLayout.draw(canvas)
            canvas.restore()
            currentY += titleLayout.height + 30f

            // Render Blocks
            for (block in blocks) {
                if (block.isDeleted) continue

                val indent = block.indentationLevel * 20f
                val availableWidth = maxContentWidth - indent.toInt()
                if (availableWidth <= 0) continue

                when (block) {
                    is TextBlock, is HeadingBlock, is BulletedListBlock, is NumberedListBlock, is CheckboxBlock, is QuoteBlock -> {

                        val isBold = when(block) { is TextBlock -> block.isBold; is CheckboxBlock -> block.isBold; is BulletedListBlock -> block.isBold; is NumberedListBlock -> block.isBold; is QuoteBlock -> block.isBold; else -> false }
                        val isItalic = when(block) { is TextBlock -> block.isItalic; is CheckboxBlock -> block.isItalic; is BulletedListBlock -> block.isItalic; is NumberedListBlock -> block.isItalic; is QuoteBlock -> block.isItalic; else -> false }
                        val isStrike = when(block) { is TextBlock -> block.isStrikeThrough; is CheckboxBlock -> block.isStrikeThrough; is BulletedListBlock -> block.isStrikeThrough; is NumberedListBlock -> block.isStrikeThrough; is QuoteBlock -> block.isStrikeThrough; else -> false }
                        val isUnder = when(block) { is TextBlock -> block.isUnderlined; is CheckboxBlock -> block.isUnderlined; is BulletedListBlock -> block.isUnderlined; is NumberedListBlock -> block.isUnderlined; is QuoteBlock -> block.isUnderlined; else -> false }

                        textPaint.isFakeBoldText = isBold || block is HeadingBlock
                        textPaint.textSkewX = if (isItalic) -0.25f else 0f
                        textPaint.isStrikeThruText = isStrike
                        textPaint.isUnderlineText = isUnder

                        if (block is HeadingBlock) {
                            textPaint.textSize = if (block.level == 1) 18f else 14f
                        } else {
                            textPaint.textSize = 12f
                        }

                        val textStr = when (block) {
                            is TextBlock -> block.text
                            is HeadingBlock -> block.text
                            is BulletedListBlock -> "•  ${block.text}"
                            is NumberedListBlock -> "${block.number}.  ${block.text}"
                            is CheckboxBlock -> "${if (block.isChecked) "[x]" else "[ ]"}  ${block.text}"
                            is QuoteBlock -> block.text
                            else -> ""
                        }

                        if (textStr.isBlank() && block is TextBlock) {
                            currentY += (12f * 1.4f) + 12f
                            continue
                        }

                        val drawX = startX + indent + if (block is QuoteBlock) 15f else 0f
                        val actualAvailableWidth = availableWidth - if (block is QuoteBlock) 15 else 0

                        val layout = android.text.StaticLayout.Builder.obtain(textStr, 0, textStr.length, textPaint, actualAvailableWidth)
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .build()

                        val blockHeight = layout.height.toFloat()
                        val totalHeightNeeded = if (block is QuoteBlock) blockHeight + 10f else blockHeight

                        checkPagination(totalHeightNeeded)

                        canvas.save()
                        canvas.translate(drawX, currentY)
                        layout.draw(canvas)
                        canvas.restore()

                        if (block is QuoteBlock) {
                            val linePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.LTGRAY
                                strokeWidth = 3f
                            }
                            canvas.drawLine(startX + indent, currentY, startX + indent, currentY + blockHeight, linePaint)
                        }

                        val bottomGap = if (block is HeadingBlock) 2f else 12f
                        currentY += totalHeightNeeded + bottomGap
                    }

                    is CodeBlock -> {
                        textPaint.typeface = android.graphics.Typeface.MONOSPACE
                        textPaint.textSize = 10f
                        textPaint.isFakeBoldText = false

                        val padding = 10f
                        val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#F5F5F5") }

                        // Split by actual newlines to preserve code formatting
                        val paragraphs = block.code.split("\n")

                        for (p in paragraphs) {
                            val pText = p.ifEmpty { " " } // Force empty lines to render their background
                            val layout = android.text.StaticLayout.Builder.obtain(pText, 0, pText.length, textPaint, availableWidth - (padding * 2).toInt())
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                .build()

                            // Draw each wrapped line individually so it breaks across pages perfectly
                            for (i in 0 until layout.lineCount) {
                                val lineStart = layout.getLineStart(i)
                                val lineEnd = layout.getLineEnd(i)
                                val lineText = pText.substring(lineStart, lineEnd).replace("\n", "")

                                val singleLineLayout = android.text.StaticLayout.Builder.obtain(lineText, 0, lineText.length, textPaint, availableWidth - (padding * 2).toInt())
                                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                    .build()

                                val lineHeight = singleLineLayout.height.toFloat()
                                checkPagination(lineHeight)

                                // Draw line background
                                canvas.drawRect(
                                    startX + indent,
                                    currentY,
                                    startX + indent + availableWidth,
                                    currentY + lineHeight,
                                    bgPaint
                                )

                                // Draw line text
                                canvas.save()
                                canvas.translate(startX + indent + padding, currentY)
                                singleLineLayout.draw(canvas)
                                canvas.restore()

                                currentY += lineHeight
                            }
                        }

                        textPaint.typeface = android.graphics.Typeface.DEFAULT
                        currentY += 12f // Gap after the code block ends
                    }

                    is SolidDividerBlock -> {
                        checkPagination(20f)
                        val linePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            strokeWidth = 1f
                        }
                        currentY += 10f
                        canvas.drawLine(startX + indent, currentY, startX + availableWidth, currentY, linePaint)
                        currentY += 10f
                    }

                    is DatabaseBlock -> {
                        textPaint.textSize = 10f
                        textPaint.isFakeBoldText = true

                        val validCols = block.columns.filter { !it.isDeleted }
                        if (validCols.isEmpty()) continue

                        val colWidth = availableWidth / validCols.size
                        val rowHeight = 20f

                        checkPagination(rowHeight)

                        // Draw Headers
                        var currentX = startX + indent
                        val borderPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            style = android.graphics.Paint.Style.STROKE
                        }

                        for (col in validCols) {
                            val truncated = android.text.TextUtils.ellipsize(col.name, textPaint, colWidth.toFloat() - 10f, android.text.TextUtils.TruncateAt.END).toString()
                            canvas.drawText(truncated, currentX + 5f, currentY + 14f, textPaint)
                            canvas.drawRect(currentX, currentY, currentX + colWidth, currentY + rowHeight, borderPaint)
                            currentX += colWidth
                        }
                        currentY += rowHeight

                        textPaint.isFakeBoldText = false

                        for (row in block.rows.filter { !it.isDeleted }) {
                            checkPagination(rowHeight)
                            currentX = startX + indent
                            for (col in validCols) {
                                val cellText = row.cells[col.id]?.replace("\n", " ") ?: ""
                                val truncated = android.text.TextUtils.ellipsize(cellText, textPaint, colWidth.toFloat() - 10f, android.text.TextUtils.TruncateAt.END).toString()

                                canvas.drawText(truncated, currentX + 5f, currentY + 14f, textPaint)
                                canvas.drawRect(currentX, currentY, currentX + colWidth, currentY + rowHeight, borderPaint)
                                currentX += colWidth
                            }
                            currentY += rowHeight
                        }
                        currentY += 15f
                    }
                    is ImageBlock -> {
                        val filePath = block.localFilePath ?: continue

                        // Safely locate the file using existing Android storage logic
                        val imgFile = if (filePath.contains("/")) {
                            java.io.File(filePath)
                        } else {
                            java.io.File(context.filesDir, filePath)
                        }

                        if (imgFile.exists()) {
                            try {
                                val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                                if (bitmap != null) {
                                    // Calculate aspect ratio
                                    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                                    var drawWidth = availableWidth.toFloat()
                                    var drawHeight = drawWidth * aspectRatio

                                    // Prevent exceptionally tall images from taking up multiple pages
                                    if (drawHeight > 400f) {
                                        drawHeight = 400f
                                        drawWidth = drawHeight / aspectRatio
                                    }

                                    checkPagination(drawHeight + 20f)

                                    // Draw the bitmap
                                    val destRect = android.graphics.RectF(
                                        startX + indent,
                                        currentY,
                                        startX + indent + drawWidth,
                                        currentY + drawHeight
                                    )
                                    canvas.drawBitmap(bitmap, null, destRect, null)

                                    currentY += drawHeight + 15f // spacing after image
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    else -> {
                        // safely ignore audio, sketch, image, and document blocks for the PDF export
                    }
                }
            }

            pdfDocument.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                pdfDocument.writeTo(stream)
            }
            pdfDocument.close()
            Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    }
}