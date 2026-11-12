package com.example.network.llm

import android.content.Context
import com.example.domain.LocalModelManager
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

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

    private val modelManager: LocalModelManager by lazy { LocalModelManager.getInstance(context) }

    /**
     * Cached engine (F-18): loading the 584 MB Gemma 3 model takes multiple
     * seconds, so the engine is created once and reused across generate()
     * calls. Invalidated when the model path changes or inference fails.
     */
    private val cachedEngine = AtomicReference<CachedEngine?>(null)

    private class CachedEngine(val modelPath: String, val engine: Engine)

    private fun obtainEngine(modelPath: String): Engine {
        cachedEngine.get()?.let { cached ->
            if (cached.modelPath == modelPath && cached.engine.isInitialized()) {
                return cached.engine
            }
            runCatching { cached.engine.close() }
            cachedEngine.compareAndSet(cached, null)
        }
        synchronized(this) {
            cachedEngine.get()?.let { cached ->
                if (cached.modelPath == modelPath && cached.engine.isInitialized()) {
                    return cached.engine
                }
                runCatching { cached.engine.close() }
                cachedEngine.compareAndSet(cached, null)
            }
            Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            val engine = Engine(engineConfig)
            engine.initialize()
            cachedEngine.set(CachedEngine(modelPath, engine))
            return engine
        }
    }

    private fun invalidateEngine() {
        cachedEngine.getAndSet(null)?.let { runCatching { it.engine.close() } }
    }

    override val id: String = LlmProviderFactory.PROVIDER_GEMMA3_LOCAL
    override val displayName: String = "Gemma 3 1B (Local)"

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

        // Reuse the cached engine when possible (F-18) — model load is the
        // dominant cost of every generation otherwise.
        val engine = obtainEngine(modelPath)

        try {
            engine.createConversation().use { conversation ->
                val responseBuilder = StringBuilder()
                val chunkCount = intArrayOf(0)

                try {
                    // sendMessageAsync emits Message objects (not raw strings).
                    // Extract the text parts of each chunk explicitly — appending
                    // the Message object itself would produce garbage output.
                    conversation.sendMessageAsync(fullPrompt).collect { message ->
                        val chunkText = message.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("") { it.text }
                        responseBuilder.append(chunkText)
                        chunkCount[0]++
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
        } catch (e: Exception) {
            // A failed engine must not be reused — drop the cache so the next
            // call starts from a fresh engine.
            invalidateEngine()
            throw e
        }
    }

    override fun close() {
        invalidateEngine()
    }
}