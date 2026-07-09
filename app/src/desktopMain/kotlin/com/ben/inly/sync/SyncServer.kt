package com.ben.inly.sync

import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.prefs.SyncConstants
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncPayload
import com.ben.inly.domain.sync.SyncRepository
import com.ben.inly.domain.util.SyncCoordinator
import io.ktor.server.auth.*

import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

fun startSyncServer(settingsManager: SettingsManager, syncRepository: SyncRepository) {
    val port = settingsManager.getSyncPort().let { if (it <= 0) SyncConstants.DEFAULT_PORT else it }

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        install(ContentNegotiation) {
            // coerceInputValues: falls back to SyncEnvelope.entityType's default instead of
            // throwing when a peer running newer code sends an entityType this build's SyncType
            // enum doesn't have a case for - keeps one unrecognized envelope from corrupting the
            // entire sync batch (see matching comment in SyncClient.kt).
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }

        install(Authentication) {
            bearer(SyncConstants.AUTH_REALM) {
                authenticate { tokenCredential ->
                    if (tokenCredential.token == settingsManager.getSyncAuthToken()) {
                        UserIdPrincipal("authorized-mobile")
                    } else null
                }
            }
        }

        routing {
            authenticate(SyncConstants.AUTH_REALM) {

                get(SyncConstants.ROUTE_FETCH) {
                    val fetchStart = System.currentTimeMillis()
                    val changes = SyncCoordinator.mutex.withLock {
                        syncRepository.collectLocalChanges()
                    }
                    call.respond(SyncPayload(changes))
                    settingsManager.saveLastSyncTimestamp(fetchStart)
                }

                post(SyncConstants.ROUTE_PUSH) {
                    try {
                        val payload = call.receive<SyncPayload>()
                        SyncCoordinator.mutex.withLock {
                            syncRepository.applyRemoteChanges(payload.changes)
                        }
                        call.respond(io.ktor.http.HttpStatusCode.OK)
                    } catch (e: Exception) {
                        // A single malformed/unrecognized envelope (e.g. a version mismatch
                        // between paired devices) shouldn't silently drop the whole push - report
                        // it so the client's expectSuccess=true surfaces a real "Failed" status
                        // instead of pretending the sync succeeded.
                        e.printStackTrace()
                        call.respond(io.ktor.http.HttpStatusCode.BadRequest, e.message ?: "Sync push failed")
                    }
                }

                get("/sync/media/{fileName}") {
                    val fileName = call.parameters["fileName"]
                    if (fileName == null) {
                        call.respond(io.ktor.http.HttpStatusCode.BadRequest)
                        return@get
                    }

                    val mediaDir = java.io.File(System.getProperty("user.home"), ".inly/media")
                    val file = java.io.File(mediaDir, fileName)

                    if (file.exists()) {
                        call.respondFile(file)
                    } else {
                        call.respond(io.ktor.http.HttpStatusCode.NotFound)
                    }
                }

                post("/sync/media/{fileName}") {
                    val fileName = call.parameters["fileName"]
                    if (fileName == null) {
                        call.respond(io.ktor.http.HttpStatusCode.BadRequest)
                        return@post
                    }

                    val mediaDir = java.io.File(System.getProperty("user.home"), ".inly/media").apply { mkdirs() }
                    val file = java.io.File(mediaDir, fileName)

                    val fileBytes = call.receive<ByteArray>()
                    file.writeBytes(fileBytes)

                    call.respond(io.ktor.http.HttpStatusCode.OK)
                }
            }
        }
    }.start(wait = false)
}