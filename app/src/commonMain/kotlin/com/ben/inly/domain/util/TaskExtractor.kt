package com.ben.inly.domain.util

data class TaskExtractionResult(
    val taskText: String,
    val timestamp: Long?
)

interface TaskExtractor {
    fun extractTaskAndDate(transcript: String): TaskExtractionResult
}