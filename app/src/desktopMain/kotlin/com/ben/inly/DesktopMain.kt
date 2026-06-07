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
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Desktop
import java.io.File
import androidx.compose.ui.res.painterResource

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
        InlyTheme {
            InlyApp(
                onPickImage = { onPathSelected ->
                    val dialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
                    dialog.file = "*.png;*.jpg;*.jpeg;*.webp"
                    dialog.isVisible = true
                    dialog.files.firstOrNull()?.let { file ->
                        onPathSelected(file.absolutePath)
                    }
                },
                onPickDocument = { onPathSelected ->
                    val dialog = FileDialog(null as Frame?, "Select Document", FileDialog.LOAD)
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
                }
            )
        }
    }
}