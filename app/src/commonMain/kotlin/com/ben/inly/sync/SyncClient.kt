package com.ben.inly.sync

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
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File

class SyncClient(
    private val settingsManager: SettingsManager,
    private val hmacSigner: SyncHmacSigner
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

    // MEDIA ROUTES

    suspend fun downloadMedia(fileName: String, destinationFile: File): Boolean {
        return try {
            val response: io.ktor.client.statement.HttpResponse = client.get("$serverUrl/sync/media/$fileName")
            if (response.status.value in 200..299) {
                val bytes: ByteArray = response.body()
                destinationFile.writeBytes(bytes)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadMedia(fileName: String, file: File): Boolean {
        return try {
            val response = client.post("$serverUrl/sync/media/$fileName") {
                contentType(ContentType.Application.OctetStream)
                setBody(file.readBytes())
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}