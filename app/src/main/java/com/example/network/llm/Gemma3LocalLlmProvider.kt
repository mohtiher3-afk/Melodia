package com.example.network.llm

import android.content.Context
import com.example.domain.LocalModelManager
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * Local LLM provider using Gemma 3 1B via LiteRT-LM.
 * Requires the model to be downloaded via LocalModelManager first.
 *
 * The LiteRT-LM engine is created once and reused across [generate] calls; loading a 600 MB
 * model from disk on every single inference would dominate latency and thrash memory, so the
 * engine lives for the lifetime of this provider and is released via [close].
 */
class Gemma3LocalLlmProvider(
    private val context: Context
) : LlmProvider, Closeable {

    private val modelManager: LocalModelManager by lazy { LocalModelManager(context) }

    override val id: String = LlmProviderFactory.PROVIDER_GEMMA3_LOCAL
    override val displayName: String = "Gemma 3 1B (Local)"

    @Volatile
    private var engine: Engine? = null

    override fun isAvailable(): Boolean {
        val modelInfo = LocalModelManager.getModel("gemma3_1b")
        return modelInfo != null && modelManager.isModelDownloaded(modelInfo)
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse = withContext(Dispatchers.Default) {
        val modelInfo = LocalModelManager.getModel("gemma3_1b")
            ?: throw LlmException("Gemma 3 1B model info not found")

        val modelPath = modelManager.getModelPath(modelInfo)
            ?: throw LlmException("Gemma 3 1B model not downloaded. Please download it from Settings.")

        // Build the full prompt with system instruction
        val fullPrompt = if (systemPrompt != null) {
            "$systemPrompt\n\n$prompt"
        } else {
            prompt
        }

        val eng = getOrCreateEngine(modelPath)

        eng.createConversation().use { conversation ->
            val responseBuilder = StringBuilder()
            val tokenCount = intArrayOf(0)

            try {
                conversation.sendMessageAsync(fullPrompt).collect { token ->
                    responseBuilder.append(token)
                    tokenCount[0]++
                }
            } catch (e: Exception) {
                throw LlmException("LiteRT-LM inference failed: ${e.message}", e)
            }

            val responseText = responseBuilder.toString().trim()

            if (responseText.isEmpty()) {
                throw LlmException("LiteRT-LM returned empty response")
            }

            LlmResponse(
                text = responseText,
                providerId = id,
                model = "gemma-3-1b-it-int4",
                finishReason = "stop"
            )
        }
    }

    /**
     * Returns the cached engine, creating it on first use. The engine is initialized off the
     * calling thread inside [withContext(Dispatchers.Default)] and retained for the lifetime of
     * this provider so subsequent prompts do not pay the model-loading cost again.
     */
    private suspend fun getOrCreateEngine(modelPath: String): Engine {
        engine?.let { return it }

        return withContext(Dispatchers.Default) {
            Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            Engine(engineConfig).also { engine = it }
        }
    }

    /**
     * Rough token count estimate (~4 chars per token for English text).
     * LiteRT-LM does not expose token counts directly, so we estimate.
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4.0).toInt().coerceAtLeast(1)
    }

    override fun close() {
        engine?.let {
            try { it.close() } catch (_: Exception) { /* best-effort */ }
        }
        engine = null
    }
}