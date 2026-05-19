package com.ben.inly.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcmEncryptionManager : SyncEncryptionManager {

    private val gcmTagLength = 128
    private val ivLength = 12

    private fun getSecretKey(rawKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(rawKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    override fun encryptPayload(jsonPayload: String, base64Key: String): String {
        val secretKey = getSecretKey(base64Key)

        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)
        val cipherText = cipher.doFinal(jsonPayload.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decryptPayload(encryptedBase64: String, base64Key: String): String {
        val secretKey = getSecretKey(base64Key)

        val combined = Base64.getDecoder().decode(encryptedBase64)

        val iv = ByteArray(ivLength)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength, iv)

        val cipherTextSize = combined.size - ivLength
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(combined, ivLength, cipherText, 0, cipherTextSize)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
        val plainTextBytes = cipher.doFinal(cipherText)

        return String(plainTextBytes, Charsets.UTF_8)
    }
}