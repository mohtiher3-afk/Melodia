package com.example.network.llm

import com.example.BuildConfig
import com.example.network.ApiClient
import com.example.network.GeminiGenerateRequest
import com.example.network.GeminiGenerateResponse

class GeminiLlmProvider : LlmProvider {
    override val id: String = LlmProviderFactory.PROVIDER_GEMINI
    override val displayName: String = "Gemini (Google)"

    override fun isAvailable(): Boolean =
        !BuildConfig.GEMINI_API_KEY.isNullOrBlank() && !BuildConfig.GEMINI_API_KEY.startsWith("MY_")

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse {
        if (!isAvailable()) throw LlmException("Gemini API key not configured")
        val request = GeminiGenerateRequest(
            contents = listOf(
                com.example.network.GeminiContent(
                    parts = listOf(com.example.network.GeminiPart(text = prompt))
                )
            ),
            systemInstruction = systemPrompt?.let {
                com.example.network.GeminiContent(parts = listOf(com.example.network.GeminiPart(text = it)))
            },
            generationConfig = com.example.network.GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens.coerceAtLeast(1)
            )
        )
        val resp = ApiClient.geminiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
        if (!resp.isSuccessful) throw LlmException("Gemini HTTP ${resp.code()}")
        val body = resp.body() ?: throw LlmException("Empty Gemini response")
        val text = body.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim().orEmpty()
        if (text.isEmpty()) throw LlmException("Empty content from Gemini")
        return LlmResponse(text = text, providerId = id, model = "gemini-2.5-flash")
    }
}