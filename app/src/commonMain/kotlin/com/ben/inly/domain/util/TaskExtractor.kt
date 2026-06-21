package com.ben.inly.domain.util

import com.ben.inly.domain.model.ParsedTask

interface TaskExtractor {
    fun extractTasks(transcript: String): List<ParsedTask>
}