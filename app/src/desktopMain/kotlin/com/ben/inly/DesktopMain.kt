package com.ben.inly

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.di.desktopModule
import com.ben.inly.di.sharedModule
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.presentation.InlyApp
import com.ben.inly.sync.startSyncServer
import com.ben.inly.ui.theme.InlyTheme
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Desktop
import org.koin.java.KoinJavaComponent.inject
import androidx.compose.ui.res.painterResource
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import com.ben.inly.domain.model.*
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main(args: Array<String>) = application {

    startKoin {
        modules(sharedModule, desktopModule)
    }

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    LaunchedEffect(Unit) {
        val koin = GlobalContext.get()
        val settingsManager = koin.get<SettingsManager>()
        val syncRepository = koin.get<SyncRepository>()

        startSyncServer(settingsManager, syncRepository)

        val discoveryManager = koin.get<com.ben.inly.sync.discovery.SyncDiscoveryManager>()
        val port = settingsManager.getSyncPort().let { if (it <= 0) 8080 else it }
        discoveryManager.startBroadcasting(port, "Inly Desktop")
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Inly",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        icon = painterResource("app_icon.png")
    ) {
        val currentWindow = this.window

        InlyTheme {
            InlyApp(
                onPickImage = { onPathSelected ->
                    val dialog = FileDialog(currentWindow, "Select Image", FileDialog.LOAD)
                    dialog.file = "*.png;*.jpg;*.jpeg;*.webp"
                    dialog.isVisible = true
                    dialog.files.firstOrNull()?.let { file ->
                        onPathSelected(file.absolutePath)
                    }
                },
                onPickDocument = { onPathSelected ->
                    val dialog = FileDialog(currentWindow, "Select Document", FileDialog.LOAD)
                    dialog.isVisible = true
                    dialog.files.firstOrNull()?.let { file ->
                        onPathSelected(file.absolutePath)
                    }
                },
                onOpenFile = { path, mime ->
                    try {
                        val cleanPath = path.removePrefix("file://")
                        val file = if (cleanPath.contains("/") || cleanPath.contains("\\")) {
                            File(cleanPath)
                        } else {
                            File(System.getProperty("user.home"), ".inly/media/$cleanPath")
                        }
                        Desktop.getDesktop().open(file)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onExportMarkdown = { fileName, content ->
                    Thread {
                        try {
                            val dialog = FileDialog(currentWindow, "Export Markdown", FileDialog.SAVE).apply {
                                file = fileName
                            }
                            dialog.isVisible = true

                            val chosenFileStr = dialog.file ?: return@Thread
                            val chosenDirStr = dialog.directory

                            // Safely handles the null directory bug
                            var saveFile = if (chosenDirStr != null) File(chosenDirStr, chosenFileStr) else File(chosenFileStr)

                            // Enforce proper extension and strip sneaky OS .txt additions
                            if (saveFile.name.endsWith(".txt", ignoreCase = true)) {
                                saveFile = File(saveFile.absolutePath.removeSuffix(".txt").removeSuffix(".TXT"))
                            }
                            if (!saveFile.name.endsWith(".md", ignoreCase = true)) {
                                saveFile = File(saveFile.absolutePath + ".md")
                            }

                            saveFile.writeText(content)

                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Markdown saved successfully:\n${saveFile.name}", "Success", JOptionPane.INFORMATION_MESSAGE)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Error saving Markdown:\n${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                            }
                        }
                    }.start()
                },
                onExportPdf = { fileName, title, blocks ->
                    Thread {
                        try {
                            val dialog = FileDialog(currentWindow, "Export PDF", FileDialog.SAVE).apply {
                                file = fileName
                            }
                            dialog.isVisible = true

                            val chosenFileStr = dialog.file ?: return@Thread
                            val chosenDirStr = dialog.directory

                            var saveFile = if (chosenDirStr != null) File(chosenDirStr, chosenFileStr) else File(chosenFileStr)

                            if (saveFile.name.endsWith(".txt", ignoreCase = true)) {
                                saveFile = File(saveFile.absolutePath.removeSuffix(".txt").removeSuffix(".TXT"))
                            }
                            if (!saveFile.name.endsWith(".pdf", ignoreCase = true)) {
                                saveFile = File(saveFile.absolutePath + ".pdf")
                            }

                            // Generate the PDF
                            generateDesktopPdf(saveFile, title, blocks)

                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "PDF saved successfully:\n${saveFile.name}", "Success", JOptionPane.INFORMATION_MESSAGE)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Error saving PDF:\n${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                            }
                        }
                    }.start()
                },

                onExportBackup = { jsonContent ->
                    Thread {
                        try {
                            val fileName = "InlyBackup_${System.currentTimeMillis()}.inly"
                            val dialog = FileDialog(currentWindow, "Export Inly Backup", FileDialog.SAVE).apply {
                                file = fileName
                            }
                            dialog.isVisible = true

                            val chosenFileStr = dialog.file ?: return@Thread
                            val chosenDirStr = dialog.directory
                            val saveFile = if (chosenDirStr != null) File(chosenDirStr, chosenFileStr) else File(chosenFileStr)

                            // Direct references to your home file directory
                            val mediaDir = File(System.getProperty("user.home"), ".inly/media")

                            val exporter = com.ben.inly.util.DesktopBackupExporter()
                            exporter.exportToZip(saveFile, jsonContent, mediaDir)

                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Backup saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Export failed:\n${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                            }
                        }
                    }.start()
                },

                // ------ ADD BACKUP IMPORT TRIGGER ------
                onImportBackupClick = {
                    Thread {
                        try {
                            val dialog = FileDialog(currentWindow, "Import Inly Backup", FileDialog.LOAD).apply {
                                file = "*.inly"
                            }
                            dialog.isVisible = true

                            val chosenFileStr = dialog.file ?: return@Thread
                            val chosenDirStr = dialog.directory
                            val sourceFile = if (chosenDirStr != null) File(chosenDirStr, chosenFileStr) else File(chosenFileStr)

                            val mediaDir = File(System.getProperty("user.home"), ".inly/media")

                            val exporter = com.ben.inly.util.DesktopBackupExporter()
                            val jsonString = exporter.importFromZip(sourceFile, mediaDir)

                            if (jsonString != null) {
                                // Reach directly into your shared module Koin scope to invoke the database merger
                                val koin = GlobalContext.get()
                                val settingsViewModel = koin.get<com.ben.inly.presentation.settings.SettingsViewModel>()

                                // Fire the multiplatform engine!
                                kotlinx.coroutines.runBlocking {
                                    settingsViewModel.mergeBackupJson(jsonString)
                                }

                                SwingUtilities.invokeLater {
                                    JOptionPane.showMessageDialog(currentWindow, "Backup restored successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                                }
                            } else {
                                SwingUtilities.invokeLater {
                                    JOptionPane.showMessageDialog(currentWindow, "Invalid or corrupted backup file.", "Error", JOptionPane.ERROR_MESSAGE)
                                }
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            SwingUtilities.invokeLater {
                                JOptionPane.showMessageDialog(currentWindow, "Import failed:\n${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                            }
                        }
                    }.start()
                }
            )
        }
    }
}

@Throws(Exception::class)
private fun generateDesktopPdf(file: File, title: String, blocks: List<NoteBlock>) {
    val document = PDDocument()
    try {
        var page = PDPage()
        document.addPage(page)

        var contentStream = PDPageContentStream(document, page)

        val titleFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val boldFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val italicFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)
        val boldItalicFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)
        val codeFont = PDType1Font(Standard14Fonts.FontName.COURIER)

        val margin = 72f
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height

        var currentY = pageHeight - margin
        val maxY = margin

        fun checkPagination(neededHeight: Float) {
            if (currentY - neededHeight < maxY) {
                contentStream.close()
                page = PDPage()
                document.addPage(page)
                contentStream = PDPageContentStream(document, page)
                currentY = pageHeight - margin
            }
        }

        fun wrapText(text: String, font: PDType1Font, fontSize: Float, maxWidth: Float): List<String> {
            val lines = mutableListOf<String>()
            var currentLine = ""

            val safeText = text.replace(Regex("[^\\x20-\\x7E\\u00A0-\\u00FF\\u2022\\u201C\\u201D\\u2018\\u2019\\u2013\\u2014]"), "")
            val words = safeText.split(" ")

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                try {
                    val width = (font.getStringWidth(testLine) / 1000f) * fontSize
                    if (width > maxWidth && currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                } catch (e: IllegalArgumentException) {
                    val stripped = word.replace(Regex("[^\\x20-\\x7E]"), "")
                    val testLineStripped = if (currentLine.isEmpty()) stripped else "$currentLine $stripped"
                    currentLine = testLineStripped
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)

            return lines.ifEmpty { listOf("") }
        }

        val safeTitle = title.ifBlank { "Untitled Note" }
        val titleLines = wrapText(safeTitle, titleFont, 24f, pageWidth - (margin * 2))

        for (line in titleLines) {
            checkPagination(28f)
            if (line.isNotBlank()) {
                contentStream.beginText()
                contentStream.setFont(titleFont, 24f)
                contentStream.newLineAtOffset(margin, currentY)
                contentStream.showText(line)
                contentStream.endText()
            }
            currentY -= 28f
        }
        currentY -= 12f

        for (block in blocks) {
            if (block.isDeleted) continue
            val indent = block.indentationLevel * 20f
            val startX = margin + indent
            val maxWidth = pageWidth - margin - startX

            when (block) {
                is TextBlock, is HeadingBlock, is BulletedListBlock, is NumberedListBlock, is CheckboxBlock, is QuoteBlock -> {
                    val isHeading = block is HeadingBlock
                    val fontSize = if (isHeading && (block as HeadingBlock).level == 1) 18f
                    else if (isHeading) 14f else 12f

                    val useBold = isHeading || when(block) { is TextBlock -> block.isBold; is CheckboxBlock -> block.isBold; is BulletedListBlock -> block.isBold; is NumberedListBlock -> block.isBold; is QuoteBlock -> block.isBold; else -> false }
                    val useItalic = when(block) { is TextBlock -> block.isItalic; is CheckboxBlock -> block.isItalic; is BulletedListBlock -> block.isItalic; is NumberedListBlock -> block.isItalic; is QuoteBlock -> block.isItalic; else -> false }

                    val currentFont = when {
                        useBold && useItalic -> boldItalicFont
                        useBold -> boldFont
                        useItalic -> italicFont
                        else -> bodyFont
                    }

                    val textStr = when (block) {
                        is TextBlock -> block.text
                        is HeadingBlock -> block.text
                        is BulletedListBlock -> "-  ${block.text}"
                        is NumberedListBlock -> "${block.number}.  ${block.text}"
                        is CheckboxBlock -> "${if (block.isChecked) "[x]" else "[ ]"}  ${block.text}"
                        is QuoteBlock -> "\"${block.text}\""
                        else -> ""
                    }

                    val leading = fontSize * 1.4f

                    if (block is TextBlock && textStr.isBlank()) {
                        checkPagination(leading)
                        currentY -= (leading + 8f)
                        continue
                    }

                    val paragraphs = textStr.split("\n", "\r\n")

                    for (p in paragraphs) {
                        val lines = wrapText(p, currentFont, fontSize, maxWidth - (if (block is QuoteBlock) 15f else 0f))
                        for (line in lines) {
                            checkPagination(leading)

                            if (line.isNotBlank()) {
                                contentStream.beginText()
                                contentStream.setFont(currentFont, fontSize)
                                contentStream.newLineAtOffset(startX + (if (block is QuoteBlock) 15f else 0f), currentY)
                                contentStream.showText(line)
                                contentStream.endText()
                            }

                            currentY -= leading
                        }
                    }

                    currentY -= if (isHeading) 2f else 8f
                }
                is SolidDividerBlock -> {
                    checkPagination(20f)
                    currentY -= 4f
                    contentStream.moveTo(startX, currentY)
                    contentStream.lineTo(pageWidth - margin, currentY)
                    contentStream.setStrokingColor(Color.LIGHT_GRAY)
                    contentStream.stroke()
                    currentY -= 12f
                }

                is CodeBlock -> {
                    val fontSize = 10f
                    val leading = fontSize * 1.4f
                    val padding = 10f

                    val paragraphs = block.code.split("\n", "\r\n")

                    for (p in paragraphs) {
                        val pText = p.ifEmpty { " " }
                        val lines = wrapText(pText, codeFont, fontSize, maxWidth - (padding * 2))

                        for (line in lines) {
                            checkPagination(leading)

                            contentStream.setNonStrokingColor(Color(245, 245, 245))
                            contentStream.addRect(startX, currentY - 4f, maxWidth, leading)
                            contentStream.fill()

                            contentStream.setNonStrokingColor(Color.BLACK)
                            if (line.isNotBlank()) {
                                contentStream.beginText()
                                contentStream.setFont(codeFont, fontSize)
                                contentStream.newLineAtOffset(startX + padding, currentY)
                                contentStream.showText(line)
                                contentStream.endText()
                            }

                            currentY -= leading
                        }
                    }
                    currentY -= 8f // Gap after the code block ends
                }

                is DatabaseBlock -> {
                    val validCols = block.columns.filter { !it.isDeleted }
                    if (validCols.isEmpty()) continue

                    val rowHeight = 22f
                    val cellPadding = 4f
                    val tableFontSize = 10f
                    val colWidth = maxWidth / validCols.size

                    checkPagination(rowHeight + 10f)

                    contentStream.setStrokingColor(Color.LIGHT_GRAY)
                    contentStream.setLineWidth(0.5f)

                    // Helper function to safely truncate long cell text
                    fun truncateForCell(text: String, font: PDType1Font, maxW: Float): String {
                        val safeText = text.replace(Regex("[^\\x20-\\x7E\\u00A0-\\u00FF]"), "").trim()
                        if (safeText.isEmpty()) return ""
                        try {
                            if ((font.getStringWidth(safeText) / 1000f) * tableFontSize <= maxW) return safeText
                            var truncated = safeText
                            while (truncated.isNotEmpty() && (font.getStringWidth("$truncated...") / 1000f) * tableFontSize > maxW) {
                                truncated = truncated.dropLast(1)
                            }
                            return if (truncated.isEmpty()) "" else "$truncated..."
                        } catch (e: Exception) {
                            return ""
                        }
                    }

                    // Draw Headers
                    var currentX = startX
                    for (col in validCols) {
                        // Draw Cell Border
                        contentStream.addRect(currentX, currentY - rowHeight, colWidth, rowHeight)
                        contentStream.stroke()

                        // Draw Header Text (Bold)
                        val text = truncateForCell(col.name, boldFont, colWidth - (cellPadding * 2))
                        if (text.isNotEmpty()) {
                            contentStream.beginText()
                            contentStream.setFont(boldFont, tableFontSize)
                            contentStream.newLineAtOffset(currentX + cellPadding, currentY - rowHeight + 7f)
                            contentStream.showText(text)
                            contentStream.endText()
                        }
                        currentX += colWidth
                    }
                    currentY -= rowHeight

                    // Draw Rows
                    for (row in block.rows.filter { !it.isDeleted }) {
                        checkPagination(rowHeight)
                        currentX = startX

                        for (col in validCols) {
                            // Draw Cell Border
                            contentStream.addRect(currentX, currentY - rowHeight, colWidth, rowHeight)
                            contentStream.stroke()

                            // Draw Cell Text (Regular)
                            val rawVal = row.cells[col.id]?.replace(Regex("[\\n\\r\\t]"), " ") ?: ""
                            val text = truncateForCell(rawVal, bodyFont, colWidth - (cellPadding * 2))
                            if (text.isNotEmpty()) {
                                contentStream.beginText()
                                contentStream.setFont(bodyFont, tableFontSize)
                                contentStream.newLineAtOffset(currentX + cellPadding, currentY - rowHeight + 7f)
                                contentStream.showText(text)
                                contentStream.endText()
                            }
                            currentX += colWidth
                        }
                        currentY -= rowHeight
                    }

                    // Add a bottom margin after the table finishes
                    currentY -= 16f
                }
                is ImageBlock -> {
                    val filePath = block.localFilePath ?: continue
                    val cleanPath = filePath.removePrefix("file://")

                    // Safely locate the file using existing Desktop storage logic
                    val imgFile = if (cleanPath.contains("/") || cleanPath.contains("\\")) {
                        File(cleanPath)
                    } else {
                        File(System.getProperty("user.home"), ".inly/media/$cleanPath")
                    }

                    if (imgFile.exists()) {
                        try {
                            // Load image into PDFBox
                            val pdImage = PDImageXObject.createFromFile(imgFile.absolutePath, document)

                            // Calculate aspect ratio
                            val aspectRatio = pdImage.height.toFloat() / pdImage.width.toFloat()
                            var drawWidth = maxWidth
                            var drawHeight = drawWidth * aspectRatio

                            if (drawHeight > 400f) {
                                drawHeight = 400f
                                drawWidth = drawHeight / aspectRatio
                            }

                            checkPagination(drawHeight + 20f)

                            // PDFBox draws from the bottom-left corner of the image, so we subtract height first
                            currentY -= drawHeight

                            contentStream.drawImage(pdImage, startX, currentY, drawWidth, drawHeight)

                            currentY -= 15f // Add spacing after image
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                else -> { /* Ignore complex blocks */ }
            }
        }

        contentStream.close()
        document.save(file)
    } finally {
        document.close()
    }
}

fun chooseFileForExport(defaultFileName: String): File? {
    val dialog = FileDialog(null as Frame?, "Export Inly Backup", FileDialog.SAVE)
    dialog.file = defaultFileName
    dialog.isVisible = true

    val file = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, file)
}

fun chooseFileForImport(): File? {
    val dialog = FileDialog(null as Frame?, "Import Inly Backup", FileDialog.LOAD)
    dialog.file = "*.inly" // Suggests the file extension
    dialog.isVisible = true

    val file = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, file)
}