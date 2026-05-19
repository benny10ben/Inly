package com.ben.inly.domain.util

data class UrlMetadata(
    val title: String?,
    val description: String?,
    val imageUrl: String?
)

expect object HtmlMetadataFetcher {
    suspend fun fetchMetadata(url: String): UrlMetadata
}