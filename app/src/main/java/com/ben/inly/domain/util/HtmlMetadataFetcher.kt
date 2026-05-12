package com.ben.inly.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI

data class UrlMetadata(
    val title: String?,
    val description: String?,
    val imageUrl: String?
)

/**
 * A simple utility I use to grab preview data (like the title, description, and thumbnail)
 * from a website link. This powers the rich bookmark blocks in the editor.
 */
object HtmlMetadataFetcher {

    /**
     * Connects to a website and scrapes its metadata tags.
     * I use a standard browser User-Agent here so sites don't immediately block the request thinking it's a bot.
     * It also automatically converts relative image paths into full, usable URLs.
     */
    suspend fun fetchMetadata(url: String): UrlMetadata = withContext(Dispatchers.IO) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url

            val document = Jsoup.connect(validUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .timeout(5000)
                .maxBodySize(250 * 1024)
                .get()

            val title = document.title().takeIf { it.isNotBlank() }
                ?: document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }

            val description = document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }

            var imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("link[rel=apple-touch-icon]").attr("href").takeIf { it.isNotBlank() }
                ?: document.select("link[rel=\"shortcut icon\"]").attr("href").takeIf { it.isNotBlank() }
                ?: document.select("link[rel=icon]").attr("href").takeIf { it.isNotBlank() }

            if (imageUrl != null && !imageUrl.startsWith("http")) {
                try {
                    val uri = URI(validUrl)
                    imageUrl = if (imageUrl.startsWith("/")) {
                        "${uri.scheme}://${uri.host}$imageUrl"
                    } else {
                        "${uri.scheme}://${uri.host}/$imageUrl"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            UrlMetadata(title = title, description = description, imageUrl = imageUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            UrlMetadata(title = url, description = "Could not load preview", imageUrl = null)
        }
    }
}