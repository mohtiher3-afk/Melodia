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
 * Together AI LLM provider.
 * Free tier: 1M tokens free, no expiration, signup required.
 * Models: meta-llama/Llama-3.3-70B-Instruct-Turbo, meta-llama/Llama-3.1-8B-Instruct
 *
 * Configure via BuildConfig:
 *   FREELLM_BASE_URL = "https://api.together.xyz/v1"
 *   FREELLM_API_KEY = "your-together-key"
 *   FREELLM_MODEL = "meta-llama/Llama-3.3-70B-Instruct-Turbo"
 */
class TogetherLlmProvider(
    private val baseUrl: String,
    private val apiKey: String?,
    private val model: String
) : LlmProvider {

    override val id: String = "together"
    override val displayName: String = "Together AI"

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
        baseUrl.isNotBlank() && baseUrl.startsWith("https://", ignoreCase = true)

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): LlmResponse {
        if (!isAvailable()) throw LlmException("Together AI base URL not configured")

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
            throw LlmException("Together AI HTTP ${e.code()} ${e.message().orEmpty()}".trim(), e)
        } catch (e: java.io.IOException) {
            throw LlmException("Together AI network error: ${e.localizedMessage ?: "connection failed"}", e)
        }
        val choice = resp.choices?.firstOrNull()
            ?: throw LlmException("Empty response from Together AI")
        val text = choice.message?.content?.trim().orEmpty()
        if (text.isEmpty()) throw LlmException("Empty content from Together AI")
        return LlmResponse(text = text, providerId = id, model = resp.model, finishReason = choice.finishReason)
    }
}

@JsonClass(generateAdapter = true)
data class TogetherChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<TogetherMessage>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "max_tokens") val max_tokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class TogetherMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class TogetherLlmResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "choices") val choices: List<TogetherChoice>? = null
)

@JsonClass(generateAdapter = true)
data class TogetherChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: TogetherMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)