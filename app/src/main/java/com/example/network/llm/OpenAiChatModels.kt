package com.example.network.llm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI-compatible chat contract shared by cloud LLM providers
 * (HuggingFace, OpenRouter, any OpenAI-compatible endpoint).
 *
 * Extracted from the deleted FreeLlmProvider.kt — the provider class itself
 * was dead code, but this contract is used by live providers.
 */
interface OpenAiChatService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String?,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenAiMessage>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "choices") val choices: List<OpenAiChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: OpenAiMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)
