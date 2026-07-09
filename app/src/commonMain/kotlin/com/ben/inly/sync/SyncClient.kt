package com.ben.inly.sync

import com.ben.inly.core.security.SyncEncryptionManager
import com.ben.inly.core.security.SyncHmacSigner
import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.prefs.SyncConstants
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncPayload
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File

class SyncClient(
    private val settingsManager: SettingsManager,
    private val hmacSigner: SyncHmacSigner,
    private val syncEncryptionManager: SyncEncryptionManager
) {
    private val client = HttpClient {
        // expectSuccess: without this, Ktor doesn't throw on a non-2xx response, so pushChanges()
        // would silently treat a server-side decode failure (e.g. a peer running older code that
        // doesn't recognize a new SyncType case) as success - the caller's try/catch in
        // SyncViewModel never sees anything went wrong, and the whole batch is just dropped.
        expectSuccess = true
        install(ContentNegotiation) {
            // coerceInputValues: falls back to SyncEnvelope.entityType's default instead of
            // throwing when decoding an entityType this build's SyncType enum doesn't have a
            // case for - keeps one unrecognized envelope from corrupting the entire sync batch.
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        // Signs every outgoing request with HMAC-SHA256 over the path + timestamp instead of a
        // static bearer token, so a sniffed request can't be replayed once its 30s window lapses.
        install(createClientPlugin("HmacAuthPlugin") {
            onRequest { request, _ ->
                val timestampMillis = Clock.System.now().toEpochMilliseconds()
                val signature = hmacSigner.sign(
                    path = request.url.encodedPath,
                    timestampMillis = timestampMillis,
                    secretKey = settingsManager.getSyncEncryptionKey()
                )
                request.headers.append(SyncConstants.HEADER_SYNC_TIMESTAMP, timestampMillis.toString())
                request.headers.append(SyncConstants.HEADER_SYNC_SIGNATURE, signature)
            }
        })
    }

    // Helper to dynamically get the URL
    private val serverUrl: String
        get() {
            val ip = settingsManager.getSyncIpAddress()
            val port = settingsManager.getSyncPort()
            return "http://$ip:$port"
        }

    suspend fun pushChanges(changes: List<SyncEnvelope>) {
        if (changes.isEmpty()) return

        client.post("$serverUrl${SyncConstants.ROUTE_PUSH}") {
            contentType(ContentType.Application.Json)
            setBody(SyncPayload(changes))
        }
    }

    suspend fun fetchChanges(): List<SyncEnvelope> {
        val response = client.get("$serverUrl${SyncConstants.ROUTE_FETCH}")
        val payload: SyncPayload = response.body()
        return payload.changes
    }

    // MEDIA ROUTES - streamed through AES/GCM so a large file is never fully buffered in memory

    suspend fun downloadMedia(fileName: String, destinationFile: File): Boolean {
        return try {
            client.prepareGet("$serverUrl/sync/media/$fileName").execute { response ->
                if (response.status.value !in 200..299) return@execute false
                response.bodyAsChannel().toInputStream().use { encryptedInput ->
                    destinationFile.outputStream().use { plainOutput ->
                        syncEncryptionManager.decryptStream(encryptedInput, plainOutput, settingsManager.getSyncEncryptionKey())
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadMedia(fileName: String, file: File): Boolean {
        // Encrypting to a sibling temp file first - rather than bridging the cipher's blocking
        // OutputStream onto Ktor's suspend ByteWriteChannel live during the HTTP write - means the
        // encrypted bytes are already complete and static on disk before the request ever starts.
        // No coroutine hand-off has to be timed against the engine's own read/flush schedule.
        val tempEncryptedFile = File(file.parentFile, "$fileName.enc.tmp")
        return try {
            withContext(Dispatchers.IO) {
                file.inputStream().use { plainInput ->
                    tempEncryptedFile.outputStream().use { encryptedOutput ->
                        syncEncryptionManager.encryptStream(plainInput, encryptedOutput, settingsManager.getSyncEncryptionKey())
                    }
                }
            }

            val response = client.post("$serverUrl/sync/media/$fileName") {
                contentType(ContentType.Application.OctetStream)
                setBody(object : OutgoingContent.ReadChannelContent() {
                    override val contentType = ContentType.Application.OctetStream
                    override val contentLength = tempEncryptedFile.length()
                    // A fresh channel per call means a retried request re-reads the same finished
                    // file from the start instead of resuming a half-drained live stream.
                    override fun readFrom(): ByteReadChannel = tempEncryptedFile.inputStream().toByteReadChannel()
                })
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            tempEncryptedFile.delete()
        }
    }
}