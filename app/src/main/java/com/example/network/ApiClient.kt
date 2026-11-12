package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>

    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateImage(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

object ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Moves the `key` query parameter into the `x-goog-api-key` header before the request is
     * logged or sent, so the Gemini API key never appears in URLs (logcat, proxies, crash logs).
     * The Retrofit interface keeps its `@Query("key")` signature so call sites don't change.
     */
    private val apiKeyToHeaderInterceptor = Interceptor { chain ->
        val request = chain.request()
        val key = request.url.queryParameter("key")
        if (key.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            val cleanUrl = request.url.newBuilder().removeAllQueryParameters("key").build()
            chain.proceed(
                request.newBuilder()
                    .url(cleanUrl)
                    .header("x-goog-api-key", key)
                    .build()
            )
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // Must be added BEFORE the logging interceptor so logs see the key-free URL.
        .addInterceptor(apiKeyToHeaderInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("x-goog-api-key")
        })
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
