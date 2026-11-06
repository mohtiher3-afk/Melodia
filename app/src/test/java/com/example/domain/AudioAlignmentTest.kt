package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-side tests for the PCM conversion and lyric-alignment helpers in [LocalWhisperEngine].
 * They run on the JVM (no Android APIs), so they are fast and need no emulator.
 */
class AudioAlignmentTest {

    // --- downmixToMono -------------------------------------------------------

    @Test
    fun downmixToMono_averagesStereoChannels() {
        val stereo = floatArrayOf(1f, 0f, 0.5f, 0.5f, -1f, 1f)
        val mono = downmixToMono(stereo, channels = 2)
        assertEquals(listOf(0.5f, 0.5f, 0f), mono.toList())
    }

    @Test
    fun downmixToMono_leavesMonoUntouched() {
        val input = floatArrayOf(0.1f, 0.2f)
        assertEquals(input.toList(), downmixToMono(input, channels = 1).toList())
    }

    // --- resampling ----------------------------------------------------------

    @Test
    fun resampleLinear_downsamplesByThree() {
        val ramp = FloatArray(48) { it.toFloat() }
        val out = resampleLinear(ramp, srcRate = 48_000, dstRate = 16_000)
        assertEquals(16, out.size)
        assertEquals(listOf(0f, 3f, 6f, 9f, 12f, 15f), out.take(6))
    }

    @Test
    fun resampleLinear_upsamplesByTwoWithInterpolation() {
        val out = resampleLinear(floatArrayOf(0f, 1f, 2f, 3f), srcRate = 8_000, dstRate = 16_000)
        assertEquals(listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f), out.toList())
    }

    @Test
    fun resampleLinear_keepsDurationForCommonRates() {
        // One second of audio must stay (about) one second at 16 kHz - the old decoder
        // reported 44.1 kHz stereo as ~5.5x too long.
        assertEquals(16_000, resampleLinear(FloatArray(44_100), 44_100, 16_000).size)
        assertEquals(16_000, resampleLinear(FloatArray(32_000), 32_000, 16_000).size)
    }

    @Test
    fun streamingResampler_chunkedOutputMatchesOneShot() {
        val ramp = FloatArray(480) { (it % 37).toFloat() }
        val oneShot = resampleLinear(ramp, 48_000, 16_000)

        val resampler = StreamingResampler(48_000, 16_000)
        val chunked = GrowableFloatArray()
        var start = 0
        while (start < ramp.size) {
            val end = minOf(start + 7, ramp.size)
            val chunk = ramp.copyOfRange(start, end)
            resampler.process(chunk, chunk.size, chunked)
            start = end
        }

        assertEquals(oneShot.toList(), chunked.toFloatArray().toList())
    }

    @Test
    fun resampleLinear_samePassesThrough() {
        val input = floatArrayOf(0.3f, -0.3f, 0.9f)
        assertEquals(input.toList(), resampleLinear(input, 16_000, 16_000).toList())
    }

    // --- activity detection --------------------------------------------------

    @Test
    fun activeFrameMask_findsLoudRegionAndIgnoresSilence() {
        // 40 quiet frames, 40 loud frames, 40 quiet frames.
        val rms = FloatArray(120) { if (it in 40 until 80) 0.5f else 0.001f }
        val mask = LocalWhisperEngine.activeFrameMask(rms)
        assertTrue(mask.slice(40 until 80).all { it })
        assertTrue(mask.slice(0 until 35).none { it })
        assertTrue(mask.slice(85 until 120).none { it })
    }

    @Test
    fun activeFrameMask_silentTrackHasNoActiveFrames() {
        assertTrue(LocalWhisperEngine.activeFrameMask(FloatArray(50)).none { it })
    }

    @Test
    fun activeFrameMask_bridgesShortGaps() {
        val rms = FloatArray(60) { 0.5f }
        for (i in 28 until 31) rms[i] = 0.0005f // 150 ms dip inside a loud passage
        val mask = LocalWhisperEngine.activeFrameMask(rms)
        assertTrue(mask.all { it })
    }

    @Test
    fun activeSegmentsMs_returnsEmptyWhenTooLittleActivity() {
        val mask = BooleanArray(10).also { it[3] = true; it[4] = true } // ~100 ms active
        assertTrue(LocalWhisperEngine.activeSegmentsMs(mask, 50, 10_000L).isEmpty())
    }

    // --- word alignment ------------------------------------------------------

    private fun words(count: Int, pause: Double = 0.0) =
        List(count) { LocalWhisperEngine.AlignableWord("w$it", 4.0, pause) }

    @Test
    fun alignWords_spreadsEvenlyOverASingleSegment() {
        val timings = LocalWhisperEngine.alignWordsToSegments(words(4), listOf(0L..4000L), 5000L)
        assertEquals(listOf(0L, 1000L, 2000L, 3000L), timings.map { it.startTimeMs })
        assertEquals(listOf(1000L, 2000L, 3000L, 4000L), timings.map { it.endTimeMs })
    }

    @Test
    fun alignWords_skipsInstrumentalGapsAndNeverSpansThem() {
        val timings = LocalWhisperEngine.alignWordsToSegments(
            words(4),
            listOf(0L..2000L, 5000L..7000L),
            8000L
        )
        assertEquals(listOf(0L, 1000L, 5000L, 6000L), timings.map { it.startTimeMs })
        // No word may stay highlighted across the 2000-5000 ms gap.
        assertTrue(timings.none { it.startTimeMs < 2000L && it.endTimeMs > 2000L })
    }

    @Test
    fun alignWords_fallsBackToWholeTrackWithoutSegments() {
        val timings = LocalWhisperEngine.alignWordsToSegments(words(3), emptyList(), 10_000L)
        assertEquals(500L, timings.first().startTimeMs)
        assertEquals(9500L, timings.last().endTimeMs)
    }

    @Test
    fun alignWords_pauseAfterLineAddsBreathingRoom() {
        val timings = LocalWhisperEngine.alignWordsToSegments(
            listOf(
                LocalWhisperEngine.AlignableWord("a", 4.0, 4.0),
                LocalWhisperEngine.AlignableWord("b", 4.0, 0.0)
            ),
            listOf(0L..12_000L),
            12_000L
        )
        assertEquals(0L, timings[0].startTimeMs)
        assertEquals(4000L, timings[0].endTimeMs)
        assertEquals(8000L, timings[1].startTimeMs)
    }

    @Test
    fun alignWords_timingsAreOrderedAndNeverOverlap() {
        val timings = LocalWhisperEngine.alignWordsToSegments(
            words(25, pause = 1.5),
            listOf(300L..9000L, 12_000L..20_000L, 21_000L..30_000L),
            31_000L
        )
        timings.forEach { assertTrue(it.endTimeMs > it.startTimeMs) }
        timings.zipWithNext().forEach { (a, b) -> assertTrue(b.startTimeMs >= a.endTimeMs) }
    }

    // --- lyric parsing -------------------------------------------------------

    @Test
    fun extractAlignableWords_skipsSectionTagsAndAddsPauses() {
        val lyrics = "[Verse 1]\nHello there\nfriend\n\n[Chorus]\nLa la"
        val result = LocalWhisperEngine.extractAlignableWords(lyrics)

        assertEquals(listOf("Hello", "there", "friend", "La", "la"), result.map { it.display })
        // "friend" ends a line AND precedes a section tag -> longest pause.
        val friend = result[2]
        val there = result[1]
        assertTrue(friend.pauseAfter > there.pauseAfter)
        assertEquals(0.0, result[0].pauseAfter, 0.0)
    }

    @Test
    fun extractAlignableWords_handlesArabicLyrics() {
        val result = LocalWhisperEngine.extractAlignableWords("[مقطع]\nيا ليل يا عين")
        assertEquals(listOf("يا", "ليل", "يا", "عين"), result.map { it.display })
    }
}
