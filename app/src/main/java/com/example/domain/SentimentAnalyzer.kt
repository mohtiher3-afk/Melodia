package com.example.domain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import org.json.JSONObject

data class SentimentResult(
    val dominantTone: String,
    val joyScore: Int,
    val energyScore: Int,
    val melancholyScore: Int,
    val nostalgiaScore: Int,
    val summary: String
)

object SentimentAnalyzer {
    private const val TAG = "SentimentAnalyzer"

    suspend fun analyzeLyrics(lyrics: String): SentimentResult = withContext(Dispatchers.IO) {
        if (lyrics.isNotBlank()) {
            // Nano is unavailable on emulators and unsupported devices — that is an
            // expected state, so skip silently (info only) instead of error-logging
            // a stack trace before the heuristic fallback.
            val provider = com.example.network.llm.LlmProviderFactory.create(
                com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO
            )
            if (provider.checkReady()) {
                try {
                    val prompt = """
                        Analyze the emotional tone and sentiment of these song lyrics. 
                        Provide percentages (0-100 sum to 100) for Joy, Energy, Melancholy, and Nostalgia, determine the dominant emotional tone, and write a 1-sentence poetic summary.
                        Return valid JSON with keys: dominantTone (string), joyScore (int), energyScore (int), melancholyScore (int), nostalgiaScore (int), summary (string).

                        Lyrics:
                        $lyrics
                    """.trimIndent()

                    val candidate = provider.generate(prompt, temperature = 0.3f, maxTokens = 400).text
                    val json = JSONObject(candidate.trim().removeSurrounding("```json", "```").trim())
                    return@withContext SentimentResult(
                        dominantTone = json.optString("dominantTone", "Energetic"),
                        joyScore = json.optInt("joyScore", 35),
                        energyScore = json.optInt("energyScore", 40),
                        melancholyScore = json.optInt("melancholyScore", 15),
                        nostalgiaScore = json.optInt("nostalgiaScore", 10),
                        summary = json.optString("summary", "High energy pulse with uplifting lyrical themes.")
                    )
                } catch (e: Exception) {
                    // Handled fallback, not an error state — warn without a stack trace.
                    Log.w(TAG, "On-device sentiment analysis failed; using heuristic analysis: ${e.message}")
                }
            } else {
                Log.i(TAG, "On-device sentiment analysis unavailable; using heuristic analysis")
            }
        }

        // Heuristic fallback sentiment analysis
        val lower = lyrics.lowercase(Locale.ROOT)
        val joyWords = listOf("hope", "light", "smile", "joy", "sun", "أمل", "نور", "فرح", "سعادة")
        val energyWords = listOf("fire", "burn", "run", "fast", "night", "neon", "شعلة", "نار", "سرعة")
        val melancholyWords = listOf("rain", "tear", "alone", "dark", "shadow", "حزن", "دموع", "وحدة", "ظلام")
        val nostalgiaWords = listOf("memory", "past", "old", "time", "dream", "ذكريات", "أمس", "حنين", "حلم")

        val joyCount = joyWords.count { lower.contains(it) }.coerceAtLeast(1)
        val energyCount = energyWords.count { lower.contains(it) }.coerceAtLeast(2)
        val melCount = melancholyWords.count { lower.contains(it) }.coerceAtLeast(1)
        val nosCount = nostalgiaWords.count { lower.contains(it) }.coerceAtLeast(1)

        val total = joyCount + energyCount + melCount + nosCount
        val joyPct = (joyCount * 100) / total
        val energyPct = (energyCount * 100) / total
        val melPct = (melCount * 100) / total
        val nosPct = 100 - (joyPct + energyPct + melPct)

        val dominant = when {
            energyPct >= joyPct && energyPct >= melPct -> "High Energy & Drive"
            joyPct >= energyPct && joyPct >= melPct -> "Euphoric & Uplifting"
            melPct >= joyPct && melPct >= energyPct -> "Melancholic & Reflective"
            else -> "Nostalgic & Atmospheric"
        }

        SentimentResult(
            dominantTone = dominant,
            joyScore = joyPct,
            energyScore = energyPct,
            melancholyScore = melPct,
            nostalgiaScore = nosPct.coerceAtLeast(5),
            summary = "Detected emotional landscape balances rhythmic intensity with expressive resonance."
        )
    }

    fun getWordSentimentColor(word: String): androidx.compose.ui.graphics.Color {
        val w = word.lowercase(Locale.ROOT).trim()
        val joy = listOf("hope", "light", "smile", "joy", "sun", "free", "أمل", "نور", "فرح", "حرية")
        val energy = listOf("fire", "burn", "run", "fast", "night", "neon", "beat", "شعلة", "نار", "سرعة", "نبض")
        val melancholy = listOf("rain", "tear", "alone", "dark", "shadow", "cold", "حزن", "دموع", "وحدة", "ظلام", "برد")
        val nostalgia = listOf("memory", "past", "old", "time", "dream", "echo", "ذكريات", "أمس", "حنين", "حلم", "صدى")

        return when {
            joy.any { w.contains(it) } -> androidx.compose.ui.graphics.Color(0xFF00E676) // Green (Joy)
            energy.any { w.contains(it) } -> androidx.compose.ui.graphics.Color(0xFFD71920) // Red (Energy)
            melancholy.any { w.contains(it) } -> androidx.compose.ui.graphics.Color(0xFF2979FF) // Blue (Melancholy)
            nostalgia.any { w.contains(it) } -> androidx.compose.ui.graphics.Color(0xFF7C4DFF) // Purple (Nostalgia)
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
    }
}
