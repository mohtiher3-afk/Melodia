package com.example.network.llm

import kotlinx.coroutines.flow.collect

/**
 * On-device lyric generation through Gemini Nano / Android AICore.
 * TODO: Integrate with ML Kit GenAI Prompt API when the API stabilizes.
 * Currently returns UNAVAILABLE to gracefully fall back to cloud providers.
 */
class GeminiNanoLlmProvider : LlmProvider {
    override val id: String = "gemini_nano"
    override val displayName: String = "Gemini Nano (on-device)"

    // The model is provisioned by Android AICore, so its status cannot be checked synchronously.
    // generate() performs the authoritative check and reports unsupported devices clearly.
    override fun isAvailable(): Boolean = false

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse {
        throw LlmException(
            "Gemini Nano on-device generation is not yet integrated. " +
            "ML Kit GenAI Prompt API integration pending. Please use a cloud provider."
        )
    }
}