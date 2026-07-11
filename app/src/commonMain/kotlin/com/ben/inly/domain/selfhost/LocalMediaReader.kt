package com.ben.inly.domain.selfhost

expect class LocalMediaReader {
    fun readBytes(path: String): ByteArray?
}
