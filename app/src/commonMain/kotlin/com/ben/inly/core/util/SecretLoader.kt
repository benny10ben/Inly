package com.ben.inly.core.util

import java.util.Properties
import java.io.File

object SecretLoader {
    fun getEncryptionKey(): ByteArray {
        val properties = Properties()

        // Let's make it smart enough to look in the current folder,
        // the parent folder, and the grand-parent folder.
        val possiblePaths = listOf(
            File("secrets.properties"),       // If running directly from root
            File("../secrets.properties"),    // If running from inside app/
            File("../../secrets.properties")  // Just in case of nested build folders
        )

        // Find the first file that actually exists
        val file = possiblePaths.firstOrNull { it.exists() }
            ?: throw IllegalStateException(
                "CRITICAL: secrets.properties file is missing!\n" +
                        "I looked in these exact locations and couldn't find it:\n" +
                        possiblePaths.joinToString("\n") { it.absolutePath }
            )

        properties.load(file.inputStream())
        val keyString = properties.getProperty("DEV_ENCRYPTION_KEY")
            ?: throw IllegalStateException("Key 'DEV_ENCRYPTION_KEY' missing from secrets.properties")

        return keyString.split(",").map { it.toByte() }.toByteArray()
    }
}