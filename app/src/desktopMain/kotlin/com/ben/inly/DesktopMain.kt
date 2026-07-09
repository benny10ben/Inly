package com.ben.inly

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.di.desktopModule
import com.ben.inly.di.sharedModule
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.presentation.InlyApp
import com.ben.inly.presentation.desktop.DesktopSearchShortcutBus
import com.ben.inly.sync.startSyncServer
import com.ben.inly.ui.theme.InlyTheme
import com.ben.inly.domain.util.handleExportBackup
import com.ben.inly.domain.util.handleExportMarkdown
import com.ben.inly.domain.util.handleExportPdf
import com.ben.inly.domain.util.handleImportBackup
import com.ben.inly.presentation.navigation.Screen
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Frame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() = application {

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
        val hmacSigner = koin.get<com.ben.inly.core.security.SyncHmacSigner>()

        startSyncServer(settingsManager, syncRepository, hmacSigner)

        val discoveryManager = koin.get<com.ben.inly.sync.discovery.SyncDiscoveryManager>()
        val port = settingsManager.getSyncPort().let { if (it <= 0) 8080 else it }
        discoveryManager.startBroadcasting(port, "Inly Desktop")
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Inly",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        icon = painterResource("app_icon.png"),
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed)) {
                DesktopSearchShortcutBus.requestOpen()
                true
            } else {
                false
            }
        }
    ) {
        val currentWindow = this.window as Frame

        InlyTheme {
            InlyApp(
                startRoute = Screen.Splash.route,
                onPickImage = { onPathSelected ->
                    val dialog = java.awt.FileDialog(currentWindow, "Select Image", java.awt.FileDialog.LOAD)
                    dialog.file = "*.png;*.jpg;*.jpeg;*.webp"
                    dialog.isVisible = true
                    dialog.files.firstOrNull()?.let { file -> onPathSelected(file.absolutePath) }
                },
                onPickDocument = { onPathSelected ->
                    val dialog = java.awt.FileDialog(currentWindow, "Select Document", java.awt.FileDialog.LOAD)
                    dialog.isVisible = true
                    dialog.files.firstOrNull()?.let { file -> onPathSelected(file.absolutePath) }
                },
                onOpenFile = { path, _ ->
                    try {
                        val cleanPath = path.removePrefix("file://")
                        val originalFile = if (cleanPath.contains("/") || cleanPath.contains("\\")) {
                            java.io.File(cleanPath)
                        } else {
                            java.io.File(System.getProperty("user.home"), ".inly/media/$cleanPath")
                        }

                        val tmpDir = java.io.File(System.getProperty("java.io.tmpdir"), "inly_view").apply { mkdirs() }
                        val viewFile = java.io.File(tmpDir, originalFile.name)

                        if (!viewFile.exists() || viewFile.length() != originalFile.length()) {
                            originalFile.copyTo(viewFile, overwrite = true)
                        }

                        java.awt.Desktop.getDesktop().open(viewFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onExportMarkdown = { fileName, content ->
                    Thread { handleExportMarkdown(currentWindow, fileName, content) }.start()
                },
                onExportPdf = { fileName, title, blocks ->
                    Thread { handleExportPdf(currentWindow, fileName, title, blocks) }.start()
                },
                onExportBackup = { jsonContent ->
                    Thread { handleExportBackup(currentWindow, jsonContent) }.start()
                },
                onImportBackupClick = {
                    Thread { handleImportBackup(currentWindow) }.start()
                },
                onRequestBackupFolder = {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            currentWindow,
                            "Automated background backups are currently only supported on the Android app. You can still use the manual 'Export Backup' button!",
                            "Desktop Feature",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                }
            )
        }
    }
}