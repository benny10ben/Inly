package com.ben.inly.core.util

import java.util.Properties
import java.io.File

object SecretLoader {
    fun getEncryptionKey(): ByteArray {
        val properties = Properties()
        val file = File("secrets.properties")

        if (!file.exists()) {
            throw IllegalStateException("CRITICAL: secrets.properties file is missing! Create it in the root directory.")
        }

        properties.load(file.inputStream())
        val keyString = properties.getProperty("DEV_ENCRYPTION_KEY")
            ?: throw IllegalStateException("Key 'DEV_ENCRYPTION_KEY' missing from secrets.properties")

        return keyString.split(",").map { it.toByte() }.toByteArray()
    }
}