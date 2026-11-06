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
 * Hugging Face Inference API LLM provider.
 * Free tier: Generous limits, email signup only, no credit card.
 * Models: meta-llama/Llama-3.2-1B-Instruct, google/gemma-2b-it, microsoft/phi-2
 *
 * Configure via BuildConfig:
 *   FREELLM_BASE_URL = "https://api.huggingface.co"
 *   FREELLM_API_KEY = "your-hf-token"
 *   FREELLM_MODEL = "meta-llama/Llama-3.2-1B-Instruct"
 */
class HuggingFaceLlmProvider(
    private val baseUrl: String,
    private val apiKey: String?,
    private val model: String
) : LlmProvider {

    override val id: String = "huggingface"
    override val displayName: String = "Hugging Face AI"

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
            if (!isAvailable()) throw LlmException("Hugging Face base URL not configured")

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
                throw LlmException("Hugging Face HTTP ${e.code()} ${e.message().orEmpty()}".trim(), e)
            } catch (e: java.io.IOException) {
                throw LlmException("Hugging Face network error: ${e.localizedMessage ?: "connection failed"}", e)
            }
            val choice = resp.choices?.firstOrNull()
                ?: throw LlmException("Empty response from Hugging Face")
            val text = choice.message?.content?.trim().orEmpty()
            if (text.isEmpty()) throw LlmException("Empty content from Hugging Face")
            return LlmResponse(text = text, providerId = id, model = resp.model, finishReason = choice.finishReason)
        }
    }

@JsonClass(generateAdapter = true)
data class HuggingFaceLlmResponse(
    @Json(name = "generated_text") val generated_text: String? = null,
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class HuggingFaceResponse(
    @Json(name = "generated_text") val generated_text: String?,
    @Json(name = "text") val text: String?
)