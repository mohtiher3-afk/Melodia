package com.example.network.llm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible LLM provider for OpenRouter AI.
 * Free tier: $5 credit, never expires, email signup only.
 * Models: anthropic/claude-3-haiku, openai/gpt-3.5-turbo, meta-llama/llama-3.1-8b-instruct
 *
 * Configure via BuildConfig:
 *   FREELLM_BASE_URL = "https://openrouter.ai/api/v1"
 *   FREELLM_API_KEY = "your-openrouter-key"
 *   FREELLM_MODEL = "anthropic/claude-3-haiku" (or other free model)
 */
class OpenRouterLlmProvider(
    private val baseUrl: String,
    private val apiKey: String?,
    private val model: String
) : LlmProvider {

    override val id: String = "openrouter"
    override val displayName: String = "OpenRouter AI"

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val service: OpenAiChatService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiChatService::class.java)
    }

    override fun isAvailable(): Boolean =
        baseUrl.isNotBlank() &&
            (baseUrl.startsWith("http://", ignoreCase = true) || baseUrl.startsWith("https://", ignoreCase = true))

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse {
        if (!isAvailable()) throw LlmException("OpenRouter base URL not configured")

        val messages = mutableListOf<OpenAiMessage>()
        if (!systemPrompt.isNullOrBlank()) messages += OpenAiMessage(role = "system", content = systemPrompt)
        messages += OpenAiMessage(role = "user", content = prompt)

        val body = OpenAiChatRequest(
            model = model,
            messages = messages,
            temperature = temperature.toDouble(),
            maxTokens = maxTokens
        )

        val auth = if (apiKey.isNullOrBlank()) null else "Bearer $apiKey"
        val resp = try {
            service.chatCompletions(auth, body)
        } catch (e: retrofit2.HttpException) {
            throw LlmException("OpenRouter HTTP ${e.code()} ${e.message().orEmpty()}".trim(), e)
        } catch (e: java.io.IOException) {
            throw LlmException("OpenRouter network error: ${e.localizedMessage ?: "connection failed"}", e)
        }
        val choice = resp.choices?.firstOrNull()
            ?: throw LlmException("Empty response from OpenRouter")
        val text = choice.message?.content?.trim().orEmpty()
        if (text.isEmpty()) throw LlmException("Empty content from OpenRouter")
        return LlmResponse(text = text, providerId = id, model = resp.model, finishReason = choice.finishReason)
    }
}

@JsonClass(generateAdapter = true)
data class OpenRouterLlmResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "choices") val choices: List<OpenRouterChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: OpenRouterMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String? = "assistant",
    @Json(name = "content") val content: String? = null
)