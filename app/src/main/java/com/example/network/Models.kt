package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LyricSection(
    val name: String,
    val text: String,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0
)

data class WordTiming(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

data class KaraokeLyrics(
    val lyrics: String,
    val wordTimings: List<WordTiming>
)

@JsonClass(generateAdapter = true)
data class TrebloGenerationRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "lyrics") val lyrics: String? = null,
    @Json(name = "instrumental") val instrumental: Boolean = false,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class TrebloGenerationResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "audio_url") val audioUrl: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

// Gemini API data structures
@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "data") val data: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String = "1:1",
    @Json(name = "imageSize") val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "imageConfig") val imageConfig: GeminiImageConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

// Internal domain result after AI synthesis
data class GeneratedSongDraft(
    val title: String,
    val genre: String,
    val mood: String,
    val language: String,
    val promptText: String,
    val tags: List<String>,
    val lyrics: String,
    val sections: List<LyricSection> = emptyList(),
    val coverConcept: String,
    val coverImagePath: String? = null
)
