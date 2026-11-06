package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Data model for submitting music creation requests to Treblo Melodia API.
 */
@JsonClass(generateAdapter = true)
data class TrebloMusicRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "lyrics") val lyrics: String? = null,
    @Json(name = "instrumental") val instrumental: Boolean = false,
    @Json(name = "title") val title: String? = null
)

/**
 * Response model received from Treblo Melodia generation endpoints.
 */
@JsonClass(generateAdapter = true)
data class TrebloMusicResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "audio_url") val audioUrl: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

/**
 * Retrofit service interface for Treblo API (https://api.treblo.com/v1).
 * Automatically pulls the API key from BuildConfig.TREBLO_API_KEY.
 */
interface TrebloApiService {

    /**
     * Generate music and vocals using Melodia v3 engine.
     * Authorization header is injected automatically via OkHttp Interceptor from BuildConfig.
     */
    @POST("v1/generations/v3")
    suspend fun generateMusic(
        @Body request: TrebloMusicRequest
    ): Response<TrebloMusicResponse>

    /**
     * Backward-compatible call supporting explicit TrebloGenerationRequest.
     */
    @POST("v1/generations/v3")
    suspend fun createGeneration(
        @Header("Authorization") authorization: String,
        @Body request: TrebloGenerationRequest
    ): Response<TrebloGenerationResponse>

    /**
     * Check status and retrieve audio stream URL for a previously created generation.
     */
    @GET("v1/generations/{id}")
    suspend fun getGenerationStatus(
        @Path("id") generationId: String
    ): Response<TrebloMusicResponse>

    companion object {
        private const val BASE_URL = "https://api.treblo.com/"

        /**
         * Creates an instance of [TrebloApiService] that securely extracts the
         * API key from [BuildConfig.TREBLO_API_KEY] and attaches it to every request.
         *
         * @param dynamicKeyProvider Optional lambda to supply a user-configured key at runtime.
         */
        fun create(dynamicKeyProvider: (() -> String)? = null): TrebloApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                // Pull API key: First try dynamic provider if provided, otherwise fallback to BuildConfig
                val customKey = dynamicKeyProvider?.invoke()?.trim()
                val resolvedKey = if (!customKey.isNullOrBlank()) {
                    customKey
                } else {
                    BuildConfig.TREBLO_API_KEY.trim()
                }

                if (resolvedKey.isNotBlank() && resolvedKey != "MY_TREBLO_API_KEY") {
                    val bearerToken = if (resolvedKey.startsWith("Bearer ", ignoreCase = true)) {
                        resolvedKey
                    } else {
                        "Bearer $resolvedKey"
                    }
                    requestBuilder.header("Authorization", bearerToken)
                }

                requestBuilder.header("Content-Type", "application/json")
                requestBuilder.header("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(TrebloApiService::class.java)
        }

        /**
         * Default singleton instance pulling credentials from BuildConfig.
         */
        val instance: TrebloApiService by lazy {
            create()
        }
    }
}
