package com.ben.inly.domain.selfhost

expect class SecureSyncKeyStorage {
    fun saveEncryptionKey(key: ByteArray)
    fun getEncryptionKey(): ByteArray?
    fun saveServerCredentials(credentials: SelfHostServerCredentials)
    fun getServerCredentials(): SelfHostServerCredentials?
    fun clearAll()
}
