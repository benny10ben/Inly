package com.ben.inly.domain.selfhost.media

expect class LocalMediaReader {
    fun readBytes(path: String): ByteArray?
}