package com.ben.inly.domain.repository

import com.ben.inly.domain.ai.LocalAiEngine
import com.inly.database.InlyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.sqrt

class RagRepository(
    private val database: InlyDatabase,
    private val aiEngine: LocalAiEngine
) {
    fun queryAiStream(userQuestion: String): Flow<String> = flow {
        val queryVector = aiEngine.generateEmbedding(userQuestion)
        val queryVectorString = queryVector.joinToString(prefix = "[", postfix = "]")

        val retrievedChunks = findSimilarNotes(queryVectorString)

        if (retrievedChunks.isEmpty()) {
            emit("I couldn't find any relevant information in your notes to answer that.")
            return@flow
        }

        val contextBlock = retrievedChunks.joinToString("\n\n") { chunk ->
            "--- Note Fragment ---\n$chunk"
        }

        val systemInstructions = """
            You are Inly's offline AI assistant.
            Your job is to answer the user's question using ONLY the provided Note Fragments.
            RULES:
            1. If the answer cannot be found in the fragments, explicitly state: "I cannot find the answer in your notes."
            2. Do not guess or make up facts.
            3. Be concise.
        """.trimIndent()

        aiEngine.generateResponseStream(
            systemPrompt = systemInstructions,
            userQuestion = userQuestion,
            contextBlock = contextBlock
        ).collect { token -> emit(token) }

    }.flowOn(Dispatchers.Default)

    private fun findSimilarNotes(queryVectorString: String): List<String> {
        val queryVector = parseVector(queryVectorString)
        val allBlocks = database.vectorStoreQueries.getAllBlocks().executeAsList()

        return allBlocks
            .map { row -> Pair(row.chunk_text, cosineSimilarity(queryVector, parseVector(row.embedding))) }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    private fun parseVector(jsonString: String): List<Float> {
        return jsonString
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().toFloat() }
    }

    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
        var dot = 0f; var norm1 = 0f; var norm2 = 0f
        val size = minOf(v1.size, v2.size)
        for (i in 0 until size) {
            dot += v1[i] * v2[i]; norm1 += v1[i] * v1[i]; norm2 += v2[i] * v2[i]
        }
        if (norm1 == 0f || norm2 == 0f) return 0f
        return dot / (sqrt(norm1) * sqrt(norm2))
    }
}