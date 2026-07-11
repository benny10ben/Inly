package com.ben.inly.domain.selfhost

interface KeyDerivationManager {
    fun deriveAesKey(passphrase: CharArray, salt: ByteArray): ByteArray
    fun generateSalt(): ByteArray
    fun generatePassphrase(): String
}
