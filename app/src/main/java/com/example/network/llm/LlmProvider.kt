package com.example.network.llm

/**
 * Abstraction over a Large Language Model backend.
 *
 * Implementations must be stateless and safe to call from a coroutine.
 * All network/IO errors are propagated as exceptions so callers can
 * decide on retry/fallback policy.
 */
interface LlmProvider {
    /** Stable identifier used for logging, settings and telemetry. */
    val id: String

    /** Human readable name shown in the UI. */
    val displayName: String

    /** Whether the provider is currently configured and usable. */
    fun isAvailable(): Boolean

    /**
     * Generate a text completion.
     *
     * @param prompt user prompt.
     * @param systemPrompt optional system/instruction prompt.
     * @param temperature sampling temperature (0..2). Provider may clamp.
     * @param maxTokens maximum output tokens.
     */
    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        maxTokens: Int = 1024
    ): LlmResponse
}

data class LlmResponse(
    val text: String,
    val providerId: String,
    val model: String? = null,
    val finishReason: String? = null
)

class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)