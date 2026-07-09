package com.ben.inly.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.core.security.SyncHmacSigner
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
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.security.MessageDigest

// Verifies the X-Sync-Timestamp/X-Sync-Signature headers against a freshly computed HMAC, rejecting
// stale (replayed) or tampered requests before they reach the sync repository.
private fun ApplicationCall.hasValidSyncSignature(settingsManager: SettingsManager, hmacSigner: SyncHmacSigner): Boolean {
    val timestampMillis = request.headers[SyncConstants.HEADER_SYNC_TIMESTAMP]?.toLongOrNull() ?: return false
    val signature = request.headers[SyncConstants.HEADER_SYNC_SIGNATURE] ?: return false

    val age = System.currentTimeMillis() - timestampMillis
    if (age > SyncConstants.MAX_REQUEST_AGE_MS || age < -SyncConstants.MAX_REQUEST_AGE_MS) return false

    val expectedSignature = hmacSigner.sign(
        path = request.path(),
        timestampMillis = timestampMillis,
        secretKey = settingsManager.getSyncEncryptionKey()
    )
    // Constant-time comparison so a timing attack can't leak the signature byte-by-byte.
    return MessageDigest.isEqual(expectedSignature.toByteArray(), signature.toByteArray())
}

fun startSyncServer(
    settingsManager: SettingsManager,
    syncRepository: SyncRepository,
    hmacSigner: SyncHmacSigner,
    syncEncryptionManager: SyncEncryptionManager
) {
    val port = settingsManager.getSyncPort().let { if (it <= 0) SyncConstants.DEFAULT_PORT else it }

    embeddedServer(Netty, host = "0.0.0.0", port = port) {
        install(ContentNegotiation) {
            // coerceInputValues: falls back to SyncEnvelope.entityType's default instead of
            // throwing when a peer running newer code sends an entityType this build's SyncType
            // enum doesn't have a case for - keeps one unrecognized envelope from corrupting the
            // entire sync batch (see matching comment in SyncClient.kt).
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }

        routing {
            get(SyncConstants.ROUTE_FETCH) {
                if (!call.hasValidSyncSignature(settingsManager, hmacSigner)) {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid or expired sync signature")
                    return@get
                }

                val fetchStart = System.currentTimeMillis()
                val changes = SyncCoordinator.mutex.withLock {
                    syncRepository.collectLocalChanges()
                }
                call.respond(SyncPayload(changes))
                settingsManager.saveLastSyncTimestamp(fetchStart)
            }

            post(SyncConstants.ROUTE_PUSH) {
                if (!call.hasValidSyncSignature(settingsManager, hmacSigner)) {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid or expired sync signature")
                    return@post
                }

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
                if (!call.hasValidSyncSignature(settingsManager, hmacSigner)) {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid or expired sync signature")
                    return@get
                }

                val fileName = call.parameters["fileName"]
                if (fileName == null) {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest)
                    return@get
                }

                val mediaDir = java.io.File(System.getProperty("user.home"), ".inly/media")
                val file = java.io.File(mediaDir, fileName)

                if (!file.exists()) {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound)
                    return@get
                }

                // Streams the plaintext file through AES/GCM straight into the HTTP response body
                // in fixed-size chunks - the file is never fully loaded into memory. `this.use { }`
                // guarantees the response stream is closed (flushing the final GCM tag) even if
                // encryptStream throws partway through.
                call.respondOutputStream(ContentType.Application.OctetStream) {
                    this.use { responseOutput ->
                        file.inputStream().use { plainInput ->
                            syncEncryptionManager.encryptStream(plainInput, responseOutput, settingsManager.getSyncEncryptionKey())
                        }
                    }
                }
            }

            post("/sync/media/{fileName}") {
                if (!call.hasValidSyncSignature(settingsManager, hmacSigner)) {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid or expired sync signature")
                    return@post
                }

                val fileName = call.parameters["fileName"]
                if (fileName == null) {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest)
                    return@post
                }

                val mediaDir = java.io.File(System.getProperty("user.home"), ".inly/media").apply { mkdirs() }
                val file = java.io.File(mediaDir, fileName)

                // Streams the encrypted request body through AES/GCM straight onto disk in fixed-size
                // chunks - the upload is never fully buffered into memory.
                call.receiveChannel().toInputStream().use { encryptedInput ->
                    file.outputStream().use { plainOutput ->
                        syncEncryptionManager.decryptStream(encryptedInput, plainOutput, settingsManager.getSyncEncryptionKey())
                    }
                }

                call.respond(io.ktor.http.HttpStatusCode.OK)
            }
        }
    }.start(wait = false)
}