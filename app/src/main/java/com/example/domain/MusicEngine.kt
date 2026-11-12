package com.example.domain

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.network.ApiClient
import com.example.network.GeminiContent
import com.example.network.GeminiGenerateRequest
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiInlineData
import com.example.network.GeminiPart
import com.example.network.GeneratedSongDraft
import com.example.network.LyricSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PromptPolishResult(
    val prompt: String,
    val usedFallback: Boolean = false,
    val fallbackReason: String? = null
)

object MusicEngine {
    private const val TAG = "MusicEngine"

    suspend fun generateSongDraft(
        genre: String,
        mood: String,
        language: String,
        userTopic: String,
        moodKeywords: String = "",
        providerId: String = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
        appContext: Context? = null
    ): GeneratedSongDraft? = withContext(Dispatchers.IO) {
        val provider = com.example.network.llm.LlmProviderFactory.create(providerId, appContext)
        if (!provider.isAvailable()) {
            Log.w(TAG, "LLM provider '$providerId' not available")
            return@withContext null
        }
        try {
            val promptText = buildGeminiPrompt(genre, mood, language, userTopic, moodKeywords)
            val systemPrompt = "You are a world-class music producer, songwriter, and Nothing OS audio architect. " +
                    "You generate radio-ready song titles, precise Melodia prompt tags, " +
                    "and poetic, rhyming lyrics structured with [Intro], [Verse 1], [Chorus], [Verse 2], [Bridge], [Chorus], [Outro]. " +
                    "Always respond in valid JSON with keys: title, promptText, tags (array of strings), lyrics, coverConcept."
            val response = provider.generate(promptText, systemPrompt, 0.8f, 2048)
            if (response.text.isNotBlank()) {
                return@withContext parseGeminiJson(response.text, genre, mood, language)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking LLM provider '$providerId'", e)
            throw e
        }
        null
    }

    suspend fun autoPolishPrompt(
        rawPrompt: String,
        genre: String,
        mood: String,
        vocalStyle: String,
        bpm: String,
        lyricalTheme: String = "",
        providerId: String = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
        appContext: Context? = null
    ): PromptPolishResult = withContext(Dispatchers.IO) {
        val systemPrompt = "You are a master audio engineer and music producer. " +
                "Expand the raw music prompt into a vivid production prompt with instruments, acoustic space, mix balance, " +
                "and vocal nuances. Return only the polished prompt, without markdown or quotes."
        val userText = "Raw prompt: '$rawPrompt'\nGenre: $genre\nMood: $mood\nVocal Style: $vocalStyle\nTempo: $bpm\n\n" +
                (if (lyricalTheme.isNotBlank()) "Lyrical theme/key idea: '$lyricalTheme'" else "") +
                "\n\nPolish and enrich this into an optimized Melodia prompt."
        var fallbackReason = "No available text provider was found."
        try {
            val provider = com.example.network.llm.LlmProviderFactory.create(providerId, appContext)
            if (provider.isAvailable()) {
                val polished = provider.generate(userText, systemPrompt, temperature = 0.7f, maxTokens = 800)
                    .text.trim()
                if (polished.isNotBlank()) {
                    return@withContext PromptPolishResult(prompt = polished)
                }
                fallbackReason = "The provider returned an empty response."
            } else {
                fallbackReason = "The selected provider is not available on this device."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking LLM provider '$providerId' for prompt polishing", e)
            fallbackReason = "The selected provider could not complete the request."
        }
        PromptPolishResult(
            prompt = "$genre, $mood, $bpm, $vocalStyle, ${rawPrompt.ifBlank { "high fidelity studio recording" }}, wide stereo soundstage, punchy bass, pristine mix balance",
            usedFallback = true,
            fallbackReason = fallbackReason
        )
    }

    suspend fun suggestTagsFromPrompt(
        polishedPrompt: String,
        providerId: String = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
        appContext: Context? = null
    ): List<String> = withContext(Dispatchers.IO) {
        val systemPrompt = "Analyze the music production prompt and suggest 5-8 genre, mood, and instrumentation tags. " +
                "Return only comma-separated lowercase tags, without sentences or markdown."
        try {
            val provider = com.example.network.llm.LlmProviderFactory.create(providerId, appContext)
            if (provider.isAvailable()) {
                val tags = provider.generate("Prompt: '$polishedPrompt'", systemPrompt, 0.5f, 250)
                    .text.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                if (tags.isNotEmpty()) return@withContext tags
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking LLM provider '$providerId' for tag suggestions", e)
        }
        listOf("melodic", "electronic", "studio-quality")
    }

    private fun buildGeminiPrompt(
        genre: String,
        mood: String,
        language: String,
        userTopic: String,
        moodKeywords: String
    ): String {
        val moodContext = if (moodKeywords.isNotBlank()) "$mood ($moodKeywords)" else mood
        return """
            Write a complete song draft with structured, rhyming lyrics based on these criteria:
            - Music Genre: $genre
            - Mood & Tone: $moodContext
            - Mood Keywords: ${if (moodKeywords.isNotBlank()) moodKeywords else "rhythmic, expressive, atmospheric"}
            - Language: $language
            - Topic/Inspiration: ${if (userTopic.isBlank()) "Midnight city lights, ambition, heartbeat, and freedom" else userTopic}

            Structure the lyrics cleanly using standard bracketed section markers:
            [Intro]
            ...
            [Verse 1]
            ...
            [Pre-Chorus]
            ...
            [Chorus]
            ...
            [Verse 2]
            ...
            [Bridge]
            ...
            [Chorus]
            ...
            [Outro]

            Return a single JSON object with EXACTLY this schema:
            {
              "title": "A catchy, poetic song title",
              "promptText": "Detailed Melodia production prompt specifying instrumentation, BPM, vocal tone, and acoustic space",
              "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
              "lyrics": "Full structured song lyrics with [Intro], [Verse 1], [Chorus], [Verse 2], [Bridge], [Chorus], [Outro]",
              "coverConcept": "Minimalist Nothing OS inspired monochrome and red visual aesthetic description for the album cover"
            }
        """.trimIndent()
    }

    suspend fun generateNextLine(
        existingLyrics: String,
        genre: String,
        mood: String,
        language: String,
        providerId: String = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
        appContext: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val lastLine = existingLyrics.lines().lastOrNull { it.isNotBlank() && !it.startsWith("[") } ?: ""
        val promptText = "The user is writing a $genre song ($mood, $language). " +
                "Current lyrics so far:\n$existingLyrics\n\n" +
                "Generate exactly one rhyming next line that continues the verse and rhymes with '$lastLine'. " +
                "Return only the single line, without quotes, markdown, or section tags."
        try {
            val provider = com.example.network.llm.LlmProviderFactory.create(providerId, appContext)
            if (provider.isAvailable()) {
                val line = provider.generate(promptText, temperature = 0.7f, maxTokens = 100)
                    .text.trim().lineSequence().firstOrNull { it.isNotBlank() }
                if (!line.isNullOrBlank()) return@withContext line
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking LLM provider '$providerId' for next line", e)
        }
        if (language.contains("Arabic", true)) "ونجم الليل يسبق الأماني في سمانا" else "And every rhythm takes us higher than before"
    }

    fun parseLyricsIntoSections(lyrics: String): List<LyricSection> {
        val sections = mutableListOf<LyricSection>()
        var currentSection: LyricSection? = null
        var currentText = StringBuilder()
        var currentOffsetMs = 0L
        val defaultSectionDurationMs = 25000L

        lyrics.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (currentSection != null) {
                    val endOffset = currentOffsetMs + defaultSectionDurationMs
                    sections.add(
                        currentSection!!.copy(
                            text = currentText.toString().trim(),
                            startTimeMs = currentOffsetMs,
                            endTimeMs = endOffset
                        )
                    )
                    currentOffsetMs = endOffset
                }
                currentSection = LyricSection(name = trimmed.removeSurrounding("[", "]"), text = "")
                currentText = StringBuilder()
            } else if (currentSection != null) {
                currentText.append(line).append("\n")
            }
        }
        if (currentSection != null) {
            val endOffset = currentOffsetMs + defaultSectionDurationMs
            sections.add(
                currentSection!!.copy(
                    text = currentText.toString().trim(),
                    startTimeMs = currentOffsetMs,
                    endTimeMs = endOffset
                )
            )
        }
        return sections
    }

    private fun parseGeminiJson(
        jsonString: String,
        genre: String,
        mood: String,
        language: String
    ): GeneratedSongDraft? {
        return try {
            val cleaned = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleaned)
            val title = obj.optString("title", "Untitled Wave")
            val promptText = obj.optString("promptText", "$genre $mood melody")
            val tagsArray = obj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(i))
                }
            } else {
                tags.addAll(listOf(genre.lowercase(), mood.lowercase(), "melodic"))
            }
            val lyrics = obj.optString("lyrics", "[Verse 1]\nEchoes in the dark...")
            val sections = parseLyricsIntoSections(lyrics)
            val coverConcept = obj.optString("coverConcept", "Nothing OS minimalist monochrome dot-matrix with red glow")

            val resolvedGenre = obj.optString("genre", genre)
            val resolvedMood = obj.optString("mood", mood)
            val resolvedLanguage = obj.optString("language", language)

            GeneratedSongDraft(
                title = title,
                genre = resolvedGenre,
                mood = resolvedMood,
                language = resolvedLanguage,
                promptText = promptText,
                tags = tags,
                lyrics = lyrics,
                sections = sections,
                coverConcept = coverConcept
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response from Gemini", e)
            null
        }
    }

    suspend fun transcribeLyricsFromAudio(
        audioMetadata: ImportedAudioMetadata,
        allowCloudUpload: Boolean = false
    ): GeneratedSongDraft? = withContext(Dispatchers.IO) {
        if (!allowCloudUpload) {
            Log.w(TAG, "Cloud audio transcription blocked because explicit upload consent was not supplied")
            return@withContext null
        }
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val base64Data = LocalAudioImporter.getBase64AudioPayload(audioMetadata.localFile)
                if (!base64Data.isNullOrBlank()) {
                    val promptText = "Listen to this audio track (\"${audioMetadata.title}\" by ${audioMetadata.artist}). " +
                            "1. Transcribe the vocal lyrics accurately. Format them clearly with section markers such as [Intro], [Verse 1], [Chorus], [Verse 2], [Bridge], [Chorus], [Outro]. " +
                            "2. Detect the musical genre. 3. Detect the emotional mood. 4. Detect the dominant language. " +
                            "5. Write a production prompt describing the instrumentation, mix, and sound design. " +
                            "6. Provide 4-6 style tags and a Nothing OS monochrome cover concept. " +
                            "Return ONLY valid JSON with keys: title, genre, mood, language, promptText, tags (array of strings), lyrics, coverConcept."

                    val request = GeminiGenerateRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(
                                    GeminiPart(
                                        inlineData = GeminiInlineData(
                                            mimeType = audioMetadata.mimeType,
                                            data = base64Data
                                        )
                                    ),
                                    GeminiPart(text = promptText)
                                )
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.3f,
                            responseMimeType = "application/json"
                        ),
                        systemInstruction = GeminiContent(
                            parts = listOf(
                                GeminiPart(
                                    text = "You are a professional audio analysis and lyric transcription engineer for Nothing OS Melodia. " +
                                            "You listen to audio recordings and extract vocal lyrics, musical structure, and production metadata into clean JSON."
                                )
                            )
                        )
                    )

                    val response = ApiClient.geminiService.generateContent(apiKey, request)
                    if (response.isSuccessful) {
                        val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!candidateText.isNullOrBlank()) {
                            val parsed = parseGeminiJson(
                                candidateText,
                                genre = "Modern Beats",
                                mood = "Dynamic",
                                language = "Auto-Detected"
                            )
                            if (parsed != null) {
                                val resolvedTitle = if (parsed.title.isBlank() || parsed.title.contains("Untitled", true)) {
                                    audioMetadata.title
                                } else {
                                    parsed.title
                                }
                                return@withContext parsed.copy(title = resolvedTitle)
                            }
                        }
                    } else {
                        Log.w(TAG, "Gemini audio transcription API call failed: ${response.code()} ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking Gemini audio transcription", e)
            }
        }
        null
    }
}
