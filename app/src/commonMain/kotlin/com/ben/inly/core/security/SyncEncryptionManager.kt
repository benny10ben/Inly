package com.ben.inly.core.security

/**
 * Multiplatform contract for encrypting sync payloads.
 */
interface SyncEncryptionManager {
    fun encryptPayload(jsonPayload: String, base64Key: String): String
    fun decryptPayload(encryptedBase64: String, base64Key: String): String
}