package com.ben.inly.sync

import com.ben.inly.data.local.prefs.SyncConstants
import com.ben.inly.domain.sync.SyncEnvelope
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*

class SyncClient(
    private val serverUrl: String,
    private val authToken: String
) {
    private val client = HttpClient {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                loadTokens { BearerTokens(authToken, "") }
            }
        }
    }

    suspend fun pushChanges(changes: List<SyncEnvelope>) {
        client.post("$serverUrl${SyncConstants.ROUTE_PUSH}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
    }

    suspend fun fetchChanges(): List<SyncEnvelope> {
        return client.get("$serverUrl${SyncConstants.ROUTE_FETCH}").body()
    }
}