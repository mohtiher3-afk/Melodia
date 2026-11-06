package com.example.network.llm

import com.google.mlkit.genai.prompt.DownloadStatus
import com.google.mlkit.genai.prompt.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.collect

/**
 * On-device lyric generation through Gemini Nano / Android AICore.
 * Device support is checked at request time because it varies by phone and AICore rollout.
 */
class GeminiNanoLlmProvider : LlmProvider {
    override val id: String = "gemini_nano"
    override val displayName: String = "Gemini Nano (on-device)"

    // The model is provisioned by Android AICore, so its status cannot be checked synchronously.
    // generate() performs the authoritative check and reports unsupported devices clearly.
    override fun isAvailable(): Boolean = true

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse {
        val model = Generation.getClient()
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE -> {
                model.download().collect { status ->
                    if (status is DownloadStatus.DownloadFailed) {
                        throw LlmException("Gemini Nano model download failed: ${status.e.message}", status.e)
                    }
                }
            }
            FeatureStatus.DOWNLOADING -> {
                throw LlmException("Gemini Nano is downloading in Android AICore. Try again when it finishes.")
            }
            FeatureStatus.UNAVAILABLE -> {
                throw LlmException(
                    "On-device Gemini Nano is not supported on this phone or is not yet available from AICore. " +
                        "Choose another provider or use a supported device."
                )
            }
        }

        val fullPrompt = buildString {
            if (!systemPrompt.isNullOrBlank()) {
                append(systemPrompt.trim())
                append("\n\n")
            }
            append(prompt.trim())
        }
        if (fullPrompt.length > 12_000) {
            throw LlmException("This prompt may exceed Gemini Nano's on-device context limit. Shorten the lyrics or request.")
        }
        if (model.checkStatus() != FeatureStatus.AVAILABLE) {
            throw LlmException("Gemini Nano did not become available after its download. Check the phone's AICore status and retry.")
        }
        val response = try {
            model.generateContent(
                generateContentRequest(TextPart(fullPrompt)) {
                    temperature = temperature.coerceIn(0f, 1f)
                    maxOutputTokens = maxTokens.coerceIn(1, 4096)
                }
            )
        } catch (e: Exception) {
            throw LlmException("On-device Gemini Nano generation failed: ${e.localizedMessage ?: "unknown error"}", e)
        }
        val text = response.text?.trim().orEmpty()
        if (text.isBlank()) throw LlmException("Gemini Nano returned an empty response")
        return LlmResponse(
            text = text,
            providerId = id,
            model = "Gemini Nano on-device",
            finishReason = null
        )
    }
}
