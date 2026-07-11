package com.ben.inly.domain.selfhost

import com.ben.inly.core.security.SyncEncryptionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.w3c.dom.Element
import org.xml.sax.InputSource

@OptIn(ExperimentalEncodingApi::class)
class WebDavSyncClient(
    private val secureSyncKeyStorage: SecureSyncKeyStorage,
    private val syncEncryptionManager: SyncEncryptionManager
) {

    private val httpClient = HttpClient {
        expectSuccess = false
    }

    private fun requireCredentials(): SelfHostServerCredentials {
        val credentials = secureSyncKeyStorage.getServerCredentials()
            ?: throw WebDavConfigurationException("No self-hosted WebDAV server has been configured yet")
        WebDavServerUrlValidator.validate(credentials.serverUrl)
        return credentials
    }

    private fun requireEncryptionKeyBase64(): String {
        val key = secureSyncKeyStorage.getEncryptionKey()
            ?: throw WebDavConfigurationException("No sync encryption key has been derived yet")
        return Base64.Default.encode(key)
    }

    private fun joinUrl(baseUrl: String, path: String): String {
        val strippedBase = baseUrl.trimEnd('/')
        val strippedPath = path.trimStart('/')
        return "$strippedBase/$strippedPath"
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    private fun resolveUrl(credentials: SelfHostServerCredentials, remotePath: String): String =
        joinUrl(credentials.serverUrl, remotePath)

    private fun basicAuthHeaderValue(credentials: SelfHostServerCredentials): String {
        val raw = "${credentials.username}:${credentials.password}".encodeToByteArray()
        return "Basic ${Base64.Default.encode(raw)}"
    }

    suspend fun ensureRemoteLayoutExists() {
        createDirectory(WebDavSyncPaths.ROOT)
        createDirectory(WebDavSyncPaths.NOTES_DIR)
        createDirectory(WebDavSyncPaths.DAILY_DIR)
        createDirectory(WebDavSyncPaths.MEDIA_DIR)
    }

    suspend fun createDirectory(remotePath: String): Boolean {
        val credentials = requireCredentials()
        val url = ensureTrailingSlash(joinUrl(credentials.serverUrl, remotePath))
        val response = httpClient.request(url) {
            method = HttpMethod("MKCOL")
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
        }
        return when {
            response.status.value in 200..299 -> true
            response.status.value == HttpStatusCode.MethodNotAllowed.value -> true
            else -> throw statusException("MKCOL", remotePath, response)
        }
    }

    suspend fun listDirectory(remotePath: String): List<WebDavResourceInfo> {
        val response = propfind(remotePath, depth = "1", credentials = requireCredentials())
        if (response.status.value !in 200..299) {
            throw statusException("PROPFIND", remotePath, response)
        }
        return parseMultistatusXml(response.bodyAsText(), remotePath, excludeSelf = true)
    }

    suspend fun getResourceInfo(remotePath: String): WebDavResourceInfo? {
        val response = propfind(remotePath, depth = "0", credentials = requireCredentials())
        if (response.status.value == HttpStatusCode.NotFound.value) return null
        if (response.status.value !in 200..299) {
            throw statusException("PROPFIND", remotePath, response)
        }
        return parseMultistatusXml(response.bodyAsText(), remotePath, excludeSelf = false).firstOrNull()
    }

    suspend fun testConnection(credentials: SelfHostServerCredentials): WebDavConnectionTestResult {
        return try {
            WebDavServerUrlValidator.validate(credentials.serverUrl)
            val response = propfind(WebDavSyncPaths.ROOT, depth = "0", credentials = credentials)
            when {
                response.status.value in 200..299 -> WebDavConnectionTestResult.Success
                response.status.value == HttpStatusCode.NotFound.value -> WebDavConnectionTestResult.Success
                response.status.value == HttpStatusCode.Unauthorized.value -> WebDavConnectionTestResult.InvalidCredentials
                response.status.value == HttpStatusCode.Forbidden.value -> WebDavConnectionTestResult.InvalidCredentials
                else -> WebDavConnectionTestResult.ServerError(response.status.value)
            }
        } catch (cause: WebDavConfigurationException) {
            WebDavConnectionTestResult.NetworkFailure(cause)
        } catch (cause: Exception) {
            WebDavConnectionTestResult.NetworkFailure(cause)
        }
    }

    suspend fun checkVaultExists(credentials: SelfHostServerCredentials): Boolean {
        WebDavServerUrlValidator.validate(credentials.serverUrl)
        val response = httpClient.request(resolveUrl(credentials, WebDavSyncPaths.SALT_FILE)) {
            method = HttpMethod.Head
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
        }
        return when {
            response.status.value in 200..299 -> true
            response.status.value == HttpStatusCode.NotFound.value -> false
            else -> throw statusException("HEAD", WebDavSyncPaths.SALT_FILE, response)
        }
    }

    private suspend fun propfind(remotePath: String, depth: String, credentials: SelfHostServerCredentials): HttpResponse {
        val requestBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:propfind xmlns:D="DAV:">
              <D:prop>
                <D:resourcetype/>
                <D:getetag/>
                <D:getcontentlength/>
              </D:prop>
            </D:propfind>
        """.trimIndent()

        return httpClient.request(resolveUrl(credentials, remotePath)) {
            method = HttpMethod("PROPFIND")
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
            header("Depth", depth)
            contentType(ContentType.Text.Xml)
            setBody(requestBody)
        }
    }

    suspend fun uploadSaltFile(salt: ByteArray, failIfExists: Boolean = false) {
        uploadPlainBytes(WebDavSyncPaths.SALT_FILE, salt, failIfExists = failIfExists)
    }

    suspend fun downloadSaltFile(): ByteArray? {
        return downloadPlainBytes(WebDavSyncPaths.SALT_FILE)
    }

    suspend fun uploadPlainBytes(remotePath: String, bytes: ByteArray, failIfExists: Boolean = false) {
        val credentials = requireCredentials()
        val response = httpClient.put(resolveUrl(credentials, remotePath)) {
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
            if (failIfExists) {
                header(HttpHeaders.IfNoneMatch, "*")
            }
            contentType(ContentType.Text.Plain)
            setBody(Base64.Default.encode(bytes))
        }
        when {
            response.status.value in 200..299 -> Unit
            response.status.value == HttpStatusCode.PreconditionFailed.value -> throw WebDavConflictException(
                "A file already exists at $remotePath"
            )
            else -> throw statusException("PUT", remotePath, response)
        }
    }

    suspend fun downloadPlainBytes(remotePath: String): ByteArray? {
        val credentials = requireCredentials()
        val response = httpClient.get(resolveUrl(credentials, remotePath)) {
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
        }
        if (response.status.value == HttpStatusCode.NotFound.value) return null
        if (response.status.value !in 200..299) {
            throw statusException("GET", remotePath, response)
        }
        return Base64.Default.decode(response.bodyAsText().trim())
    }

    suspend fun uploadNote(noteId: String, jsonPayload: String, ifMatchEtag: String? = null): String? =
        uploadEncryptedJson(WebDavSyncPaths.notePath(noteId), jsonPayload, ifMatchEtag)

    suspend fun downloadNote(noteId: String): String? =
        downloadAndDecryptJson(WebDavSyncPaths.notePath(noteId))

    suspend fun uploadDaily(dateString: String, jsonPayload: String, ifMatchEtag: String? = null): String? =
        uploadEncryptedJson(WebDavSyncPaths.dailyPath(dateString), jsonPayload, ifMatchEtag)

    suspend fun downloadDaily(dateString: String): String? =
        downloadAndDecryptJson(WebDavSyncPaths.dailyPath(dateString))

    suspend fun uploadMedia(mediaId: String, rawBytes: ByteArray, ifMatchEtag: String? = null): String? =
        uploadEncryptedBytes(WebDavSyncPaths.mediaPath(mediaId), rawBytes, ifMatchEtag)

    suspend fun downloadMedia(mediaId: String): ByteArray? =
        downloadAndDecryptBytes(WebDavSyncPaths.mediaPath(mediaId))

    suspend fun uploadEncryptedJson(remotePath: String, jsonPayload: String, ifMatchEtag: String? = null): String? {
        val encryptedBase64 = syncEncryptionManager.encryptPayload(jsonPayload, requireEncryptionKeyBase64())
        return putFile(remotePath, Base64.Default.decode(encryptedBase64), ifMatchEtag)
    }

    suspend fun downloadAndDecryptJson(remotePath: String): String? {
        val encryptedBytes = getFile(remotePath) ?: return null
        val encryptedBase64 = Base64.Default.encode(encryptedBytes)
        return syncEncryptionManager.decryptPayload(encryptedBase64, requireEncryptionKeyBase64())
    }

    suspend fun uploadEncryptedBytes(remotePath: String, rawBytes: ByteArray, ifMatchEtag: String? = null): String? {
        val encryptedBytes = syncEncryptionManager.encryptBytes(rawBytes, requireEncryptionKeyBase64())
        return putFile(remotePath, encryptedBytes, ifMatchEtag)
    }

    suspend fun downloadAndDecryptBytes(remotePath: String): ByteArray? {
        val encryptedBytes = getFile(remotePath) ?: return null
        return syncEncryptionManager.decryptBytes(encryptedBytes, requireEncryptionKeyBase64())
    }

    private suspend fun putFile(remotePath: String, bytes: ByteArray, ifMatchEtag: String?): String? {
        val credentials = requireCredentials()
        val response = httpClient.put(resolveUrl(credentials, remotePath)) {
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
            contentType(ContentType.Application.OctetStream)
            if (ifMatchEtag != null) {
                header(HttpHeaders.IfMatch, "\"$ifMatchEtag\"")
            }
            setBody(bytes)
        }

        return when (response.status.value) {
            in 200..299 -> response.headers[HttpHeaders.ETag]?.trim('"')
            HttpStatusCode.PreconditionFailed.value -> throw WebDavConflictException(
                "Remote file $remotePath was modified by another client since it was last synced"
            )
            else -> throw statusException("PUT", remotePath, response)
        }
    }

    private suspend fun getFile(remotePath: String): ByteArray? {
        val credentials = requireCredentials()
        val response = httpClient.get(resolveUrl(credentials, remotePath)) {
            header(HttpHeaders.Authorization, basicAuthHeaderValue(credentials))
        }
        if (response.status.value == HttpStatusCode.NotFound.value) return null
        if (response.status.value !in 200..299) {
            throw statusException("GET", remotePath, response)
        }
        val bytes: ByteArray = response.body()
        return bytes.takeIf { it.isNotEmpty() }
    }

    private fun statusException(verb: String, remotePath: String, response: HttpResponse): WebDavException {
        return WebDavException(
            "$verb failed for $remotePath with status ${response.status.value}",
            statusCode = response.status.value
        )
    }

    private fun parseMultistatusXml(xml: String, requestPath: String, excludeSelf: Boolean): List<WebDavResourceInfo> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))

        val normalizedRequestPath = requestPath.trimEnd('/')
        val responseNodes = document.getElementsByTagNameNS("DAV:", "response")
        val results = mutableListOf<WebDavResourceInfo>()

        for (index in 0 until responseNodes.length) {
            val responseElement = responseNodes.item(index) as? Element ?: continue
            val rawHref = responseElement.getElementsByTagNameNS("DAV:", "href")
                .item(0)?.textContent?.trim() ?: continue

            val href = if (rawHref.startsWith("http://") || rawHref.startsWith("https://")) {
                Url(rawHref).encodedPath
            } else {
                rawHref
            }

            if (excludeSelf && href.trimEnd('/') == normalizedRequestPath) continue

            var etag: String? = null
            var isCollection = false
            var contentLength: Long? = null

            val propstatNodes = responseElement.getElementsByTagNameNS("DAV:", "propstat")
            for (propIndex in 0 until propstatNodes.length) {
                val propstatElement = propstatNodes.item(propIndex) as? Element ?: continue
                val status = propstatElement.getElementsByTagNameNS("DAV:", "status")
                    .item(0)?.textContent.orEmpty()
                if (!status.contains("200")) continue

                val propElement = propstatElement.getElementsByTagNameNS("DAV:", "prop")
                    .item(0) as? Element ?: continue

                etag = propElement.getElementsByTagNameNS("DAV:", "getetag")
                    .item(0)?.textContent?.trim()?.trim('"')

                isCollection = (propElement.getElementsByTagNameNS("DAV:", "resourcetype")
                    .item(0) as? Element)
                    ?.getElementsByTagNameNS("DAV:", "collection")
                    ?.length?.let { it > 0 } == true

                contentLength = propElement.getElementsByTagNameNS("DAV:", "getcontentlength")
                    .item(0)?.textContent?.trim()?.toLongOrNull()
            }

            results += WebDavResourceInfo(
                href = href,
                etag = etag,
                isCollection = isCollection,
                contentLength = contentLength
            )
        }

        return results
    }
}
