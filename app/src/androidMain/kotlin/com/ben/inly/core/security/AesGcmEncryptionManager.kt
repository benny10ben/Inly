package com.ben.inly.core.security

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcmEncryptionManager : SyncEncryptionManager {

    private val gcmTagLength = 128
    private val ivLength = 12
    private val streamBufferSize = 8 * 1024

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

    // Fully reads a fixed number of bytes (short of EOF), since InputStream.read() may return fewer than requested
    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = input.read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) break
            offset += bytesRead
        }
        return offset
    }

    override fun encryptStream(input: InputStream, output: OutputStream, base64Key: String) {
        val secretKey = getSecretKey(base64Key)

        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)
        // The IV is written in the clear ahead of the ciphertext so the receiver can rebuild the same GCM spec
        output.write(iv)

        val gcmParameterSpec = GCMParameterSpec(gcmTagLength, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

        // CipherOutputStream.close() finalizes the cipher (writes the GCM auth tag) without closing `output`
        // early on our own, so we let `use` drive that close and rely on the caller to close `output` itself.
        CipherOutputStream(output, cipher).use { cipherOut ->
            val buffer = ByteArray(streamBufferSize)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                cipherOut.write(buffer, 0, bytesRead)
            }
        }
    }

    override fun decryptStream(input: InputStream, output: OutputStream, base64Key: String) {
        val secretKey = getSecretKey(base64Key)

        val iv = ByteArray(ivLength)
        val ivBytesRead = readFully(input, iv)
        require(ivBytesRead == ivLength) { "Encrypted stream ended before a full IV could be read" }

        val gcmParameterSpec = GCMParameterSpec(gcmTagLength, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

        // CipherInputStream verifies the GCM auth tag once the underlying stream is exhausted, throwing if it
        // was tampered with; `use` ensures we still drain/close it correctly even if that check fails midway.
        CipherInputStream(input, cipher).use { cipherIn ->
            val buffer = ByteArray(streamBufferSize)
            while (true) {
                val bytesRead = cipherIn.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
            }
        }
        output.flush()
    }
}