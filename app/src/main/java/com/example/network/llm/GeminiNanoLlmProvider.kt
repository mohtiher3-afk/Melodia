package com.example.network.llm

import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device lyric generation through Gemini Nano via the ML Kit GenAI Prompt API
 * (com.google.mlkit:genai-prompt). The model itself is provisioned by Android AICore.
 *
 * [isAvailable] reports only the static prerequisite (Android 12+ / AICore-capable).
 * The authoritative readiness check is [probeStatus] / the check inside [generate],
 * which maps every AICore state to an actionable user message instead of failing
 * silently or faking success.
 */
class GeminiNanoLlmProvider : LlmProvider {

    override val id: String = LlmProviderFactory.PROVIDER_GEMINI_NANO
    override val displayName: String = "Gemini Nano (on-device)"

    /** Static prerequisite only. Real device support is checked in generate()/probeStatus(). */
    override fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= MIN_SDK_FOR_AICORE

    /**
     * Authoritative AICore feature status for this device.
     * Returns one of [FeatureStatus.UNAVAILABLE], DOWNLOADABLE, DOWNLOADING, AVAILABLE.
     */
    suspend fun probeStatus(): Int = withContext(Dispatchers.Default) {
        client().checkStatus()
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse = withContext(Dispatchers.Default) {
        val model = client()
        val status = model.checkStatus()
        if (status != FeatureStatus.AVAILABLE) {
            throw LlmException(statusMessage(status))
        }

        val request: GenerateContentRequest = if (systemPrompt != null) {
            generateContentRequest(SystemInstruction(systemPrompt), TextPart(prompt)) {
                this.temperature = temperature
                this.maxOutputTokens = maxTokens
            }
        } else {
            generateContentRequest(TextPart(prompt)) {
                this.temperature = temperature
                this.maxOutputTokens = maxTokens
            }
        }

        val response = try {
            model.generateContent(request)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Nano generation failed", e)
            throw LlmException(
                "On-device generation failed: ${e.message ?: e.javaClass.simpleName}. " +
                    "Try again or switch to a cloud provider in Settings.",
                e
            )
        }

        val text = response.candidates.firstOrNull()?.text?.trim().orEmpty()
        if (text.isEmpty()) {
            throw LlmException(
                "Gemini Nano returned an empty response. Try rephrasing the prompt " +
                    "or switch to a cloud provider in Settings."
            )
        }

        LlmResponse(
            text = text,
            providerId = id,
            model = "gemini-nano",
            finishReason = "stop"
        )
    }

    companion object {
        private const val TAG = "GeminiNanoLlmProvider"

        /** AICore + the GenAI Prompt API require Android 12 (API 31) or newer. */
        const val MIN_SDK_FOR_AICORE = 31

        @Volatile
        private var cachedClient: GenerativeModel? = null

        /**
         * The client holds native resources; it is created once per process
         * and reused for every request (MusicEngine must not re-create it per call).
         */
        internal fun client(): GenerativeModel =
            cachedClient ?: synchronized(this) {
                cachedClient ?: Generation.getClient(nanoGenerationConfig()).also { cachedClient = it }
            }

        /** Visible for tests: allows a fake GenerativeModel to be injected. */
        @Synchronized
        internal fun setClientForTest(model: GenerativeModel?) {
            cachedClient = model
        }

        internal fun nanoGenerationConfig(): GenerationConfig = generationConfig {
            modelConfig = modelConfig { preference = ModelPreference.FULL }
        }

        /** Maps an AICore [FeatureStatus] to an actionable user-facing message. */
        fun statusMessage(status: Int): String = when (status) {
            FeatureStatus.AVAILABLE ->
                "Gemini Nano is ready."
            FeatureStatus.DOWNLOADING ->
                "The on-device model is downloading. Please wait a moment and try again."
            FeatureStatus.DOWNLOADABLE ->
                "The on-device model needs a one-time download. Open the Google/AICore " +
                    "update on this device to download it, or switch to a cloud provider in Settings."
            else ->
                "Gemini Nano is not available on this device. Use Gemma 3 (local) or a " +
                    "cloud provider in Settings."
        }
    }
}
