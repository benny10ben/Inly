package com.ben.inly.sync

import com.ben.inly.data.local.prefs.SettingsManager
import com.ben.inly.data.local.prefs.SyncConstants
import com.ben.inly.domain.sync.SyncEnvelope
import com.ben.inly.domain.sync.SyncPayload
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

class SyncClient(
    private val settingsManager: SettingsManager
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Auth) {
            bearer {
                loadTokens { BearerTokens(settingsManager.getSyncAuthToken(), "") }
            }
        }
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

    // --- MEDIA ROUTES ---

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