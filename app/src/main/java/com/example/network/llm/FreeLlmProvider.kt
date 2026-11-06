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
 * OpenAI-compatible LLM provider (works with FreeLLM API and any
 * OpenAI-compatible endpoint: base URL + optional API key).
 *
 * Configure via BuildConfig (injected from .env):
 *   FREELLM_BASE_URL, FREELLM_API_KEY, FREELLM_MODEL
 */
class FreeLlmProvider(
    private val baseUrl: String,
    private val apiKey: String?,
    private val model: String
) : LlmProvider {

    override val id: String = "freellm"
    override val displayName: String = "FreeLLM API"

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val service: OpenAiChatService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
        if (!isAvailable()) throw LlmException("FreeLLM base URL not configured")

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
            throw LlmException("FreeLLM HTTP ${e.code()} ${e.message().orEmpty()}".trim(), e)
        } catch (e: java.io.IOException) {
            throw LlmException("FreeLLM network error: ${e.localizedMessage ?: "connection failed"}", e)
        }
        val choice = resp.choices?.firstOrNull()
            ?: throw LlmException("Empty response from FreeLLM")
        val text = choice.message?.content?.trim().orEmpty()
        if (text.isEmpty()) throw LlmException("Empty content from FreeLLM")
        return LlmResponse(text = text, providerId = id, model = resp.model, finishReason = choice.finishReason)
    }
}

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