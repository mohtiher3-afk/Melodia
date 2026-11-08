package com.example.domain

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.network.KaraokeLyrics
import com.example.network.WordTiming
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

object LocalWhisperEngine {
    private const val TAG = "LocalWhisperEngine"

    /**
     * Normalizes Arabic text for token matching while preserving display characters.
     * Removes tashkeel (\u064B-\u0652), normalizes alef variants and alef maqsura/ta marbouta.
     */
    fun normalizeArabicText(text: String): String {
        return text
            .replace(Regex("[\u064B-\u0652\u0670]"), "") // Remove diacritics
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            // Keep letters/marks/digits (Arabic and Latin) and whitespace only, so that
            // Arabic punctuation such as ، ؛ ؟ is stripped too.
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), "")
            .lowercase()
            .trim()
    }

    /**
     * Decodes the audio file at [audioUri] to 16 kHz mono 32-bit float PCM.
     *
     * The decoder's real output format (sample rate, channel count, PCM encoding) is read from the
     * codec, then the stream is down-mixed to mono and resampled chunk by chunk, so memory stays
     * proportional to the final 16 kHz mono signal rather than the source file's format.
     * Returns an empty array when the file has no decodable audio track.
     */
    suspend fun decodeAudioTo16kHzFloatPcm(context: Context, audioUri: Uri): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, audioUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in $audioUri")
                return@withContext FloatArray(0)
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_AAC
            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()

            var sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, 44100)
            var channels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, 2)
            var isFloatPcm = false
            var resampler = StreamingResampler(sampleRate, TARGET_SAMPLE_RATE)
            val pcm = GrowableFloatArray()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var idleLoops = 0

            while (!outputDone) {
                ensureActive()

                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // Guard against a decoder that never signals end-of-stream (~5 s of stall).
                        idleLoops++
                        if (inputDone && idleLoops > 500) {
                            Log.w(TAG, "Decoder stalled after end of input; stopping early")
                            outputDone = true
                        }
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        idleLoops = 0
                        val outFormat = codec.outputFormat
                        sampleRate = outFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channels = outFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                        isFloatPcm = outFormat.intOr(
                            MediaFormat.KEY_PCM_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT
                        ) == AudioFormat.ENCODING_PCM_FLOAT
                        resampler = StreamingResampler(sampleRate, TARGET_SAMPLE_RATE)
                    }
                    outputIndex >= 0 -> {
                        idleLoops = 0
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outputBuffer.order(ByteOrder.nativeOrder())

                            val interleaved: FloatArray
                            if (isFloatPcm) {
                                val floats = outputBuffer.asFloatBuffer()
                                interleaved = FloatArray(floats.remaining())
                                floats.get(interleaved)
                            } else {
                                val shorts = outputBuffer.asShortBuffer()
                                interleaved = FloatArray(shorts.remaining())
                                for (k in interleaved.indices) {
                                    interleaved[k] = shorts.get() / 32768.0f
                                }
                            }

                            val mono = downmixToMono(interleaved, channels)
                            resampler.process(mono, mono.size, pcm)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            // Flush the resampler's tail: the last sample of each chunk is held back as the
            // interpolation anchor for the next chunk, so without this the final sample of the
            // stream is silently dropped and the returned PCM is one sample short.
            resampler.flush(pcm)
            pcm.toFloatArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio for lyric alignment", e)
            FloatArray(0)
        } finally {
            decoder?.let {
                try { it.stop() } catch (_: Exception) { /* already stopped / never started */ }
                try { it.release() } catch (_: Exception) { }
            }
            extractor.release()
        }
    }

    /**
     * Aligns [lyrics] to the imported audio and returns per-word timings.
     *
     * NOTE: this is an energy-aware heuristic, not speech recognition. It finds where the audio is
     * actually active (skipping leading/trailing silence and long quiet gaps), then spreads the
     * words over that active time weighted by word length, with extra breathing room at line ends
     * and before section tags. Real Whisper alignment needs the native engine to be enabled.
     */
    suspend fun alignLyricsWithAudio(
        context: Context,
        audioUri: Uri,
        lyrics: String
    ): Result<KaraokeLyrics> = withContext(Dispatchers.Default) {
        try {
            val pcmData = decodeAudioTo16kHzFloatPcm(context, audioUri)
            val audioDurationMs = if (pcmData.isNotEmpty()) {
                ((pcmData.size.toDouble() / TARGET_SAMPLE_RATE.toDouble()) * 1000.0).toLong()
            } else {
                FALLBACK_DURATION_MS
            }

            val words = extractAlignableWords(lyrics)
            if (words.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Lyrics contain no display words to align"))
            }

            val segments = if (pcmData.isNotEmpty()) {
                val mask = activeFrameMask(frameRms(pcmData, TARGET_SAMPLE_RATE, FRAME_MS))
                activeSegmentsMs(mask, FRAME_MS, audioDurationMs)
            } else {
                emptyList()
            }

            val wordTimings = alignWordsToSegments(words, segments, audioDurationMs)
            Result.success(KaraokeLyrics(lyrics = lyrics, wordTimings = wordTimings))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Alignment failed", e)
            Result.failure(e)
        }
    }

    internal const val TARGET_SAMPLE_RATE = 16_000
    internal const val FRAME_MS = 50
    private const val FALLBACK_DURATION_MS = 180_000L

    /** One lyric word with its relative sung duration and the silence expected after it. */
    internal data class AlignableWord(
        val display: String,
        val weight: Double,
        val pauseAfter: Double
    )

    /**
     * Splits lyrics into words, skipping `[Section]` tag lines. A line break adds a short pause
     * after the line's last word, and a section tag adds a longer one.
     */
    internal fun extractAlignableWords(lyrics: String): List<AlignableWord> {
        val result = mutableListOf<AlignableWord>()
        for (rawLine in lyrics.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                if (result.isNotEmpty()) {
                    val last = result.removeAt(result.lastIndex)
                    result.add(last.copy(pauseAfter = last.pauseAfter + SECTION_PAUSE_WEIGHT))
                }
                continue
            }
            val lineWords = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            lineWords.forEachIndexed { index, word ->
                val weight = normalizeArabicText(word).length.coerceIn(2, 10).toDouble()
                val pause = if (index == lineWords.lastIndex) LINE_PAUSE_WEIGHT else 0.0
                result.add(AlignableWord(word, weight, pause))
            }
        }
        return result
    }

    private const val LINE_PAUSE_WEIGHT = 1.5
    private const val SECTION_PAUSE_WEIGHT = 2.5
    private const val MIN_WORD_MS = 120L

    /**
     * Distributes [words] over the active [segments] (start/end pairs in ms) of the audio.
     * With no segments, falls back to the whole track minus a short lead-in and tail.
     */
    internal fun alignWordsToSegments(
        words: List<AlignableWord>,
        segments: List<LongRange>,
        audioDurationMs: Long
    ): List<WordTiming> {
        if (words.isEmpty()) return emptyList()

        val usable = if (segments.isNotEmpty()) {
            segments
        } else {
            val start = 500L.coerceAtMost(audioDurationMs / 4)
            val end = (audioDurationMs - 500L).coerceAtLeast(start + 1000L)
            listOf(start..end)
        }
        val totalActiveMs = usable.sumOf { it.last - it.first }.coerceAtLeast(1L)
        val totalWeight = words.sumOf { it.weight + it.pauseAfter }

        // Maps a position on the "active time" axis (silences removed) back to real audio time.
        // Returns the real time and the index of the segment it falls in. A word's start sitting
        // exactly on a segment edge belongs to the next segment; its end belongs to the current one.
        fun toRealTime(activeMs: Double, isEnd: Boolean): Pair<Long, Int> {
            var remaining = activeMs
            for ((i, seg) in usable.withIndex()) {
                val len = (seg.last - seg.first).toDouble()
                val fits = if (isEnd) remaining <= len else remaining < len
                if (fits || i == usable.lastIndex) {
                    return Pair(seg.first + remaining.coerceIn(0.0, len).toLong(), i)
                }
                remaining -= len
            }
            return Pair(usable.last().last, usable.lastIndex)
        }

        val timings = ArrayList<WordTiming>(words.size)
        var cumulative = 0.0
        var previousEnd = 0L
        for (word in words) {
            val startActive = cumulative / totalWeight * totalActiveMs
            val endActive = (cumulative + word.weight) / totalWeight * totalActiveMs
            cumulative += word.weight + word.pauseAfter

            val (rawStart, startSeg) = toRealTime(startActive, isEnd = false)
            val (rawEnd, endSeg) = toRealTime(endActive, isEnd = true)

            val start = maxOf(rawStart, previousEnd)
            // A word must not stay highlighted across an instrumental gap.
            var end = if (endSeg != startSeg) usable[startSeg].last else rawEnd
            end = maxOf(end, start + MIN_WORD_MS)
            timings.add(WordTiming(text = word.display, startTimeMs = start, endTimeMs = end))
            previousEnd = end
        }
        return timings
    }

    /** Root-mean-square level of each [frameMs] window of [pcm]. */
    internal fun frameRms(pcm: FloatArray, sampleRate: Int, frameMs: Int): FloatArray {
        val frameSize = (sampleRate * frameMs / 1000).coerceAtLeast(1)
        val frameCount = pcm.size / frameSize
        val rms = FloatArray(frameCount)
        for (f in 0 until frameCount) {
            var sumSquares = 0.0
            val base = f * frameSize
            for (i in 0 until frameSize) {
                val v = pcm[base + i].toDouble()
                sumSquares += v * v
            }
            rms[f] = Math.sqrt(sumSquares / frameSize).toFloat()
        }
        return rms
    }

    /**
     * Marks frames that are not near-silent. The threshold is relative to the track's own loud
     * level (about 22 dB below the 95th-percentile frame), so it adapts to quiet and loud
     * recordings alike and does not depend on how much silence the track happens to contain.
     * Short quiet gaps are then bridged and very short blips dropped so segments are stable.
     * This detects sound, not singing: instrumental passages count as active.
     */
    internal fun activeFrameMask(rms: FloatArray): BooleanArray {
        if (rms.isEmpty()) return BooleanArray(0)
        val sorted = rms.sortedArray()
        val peak = sorted[((sorted.size - 1) * 0.95).toInt()]
        if (peak < 1e-4f) return BooleanArray(rms.size) // effectively silent
        val threshold = peak * 0.08f
        val mask = BooleanArray(rms.size) { rms[it] > threshold }
        fillShortRuns(mask, value = false, maxLength = 6)  // bridge gaps up to 300 ms
        fillShortRuns(mask, value = true, maxLength = 2)   // drop blips up to 100 ms
        return mask
    }

    /** Flips runs of [value] that are at most [maxLength] long and sit between opposite values. */
    private fun fillShortRuns(mask: BooleanArray, value: Boolean, maxLength: Int) {
        var i = 0
        while (i < mask.size) {
            if (mask[i] != value) { i++; continue }
            var j = i
            while (j < mask.size && mask[j] == value) j++
            val length = j - i
            val enclosed = i > 0 && j < mask.size
            if (enclosed && length <= maxLength) {
                for (k in i until j) mask[k] = !value
            }
            i = j
        }
    }

    /** Converts an active-frame mask into ms ranges; falls back to the full track if none found. */
    internal fun activeSegmentsMs(mask: BooleanArray, frameMs: Int, audioDurationMs: Long): List<LongRange> {
        val segments = mutableListOf<LongRange>()
        var i = 0
        while (i < mask.size) {
            if (!mask[i]) { i++; continue }
            var j = i
            while (j < mask.size && mask[j]) j++
            segments.add((i * frameMs).toLong()..(j * frameMs).toLong())
            i = j
        }
        val totalMs = segments.sumOf { it.last - it.first }
        // Too little detected activity to trust: let the caller use its full-track fallback.
        return if (totalMs < 2_000L || audioDurationMs <= 0L) emptyList() else segments
    }

    private fun MediaFormat.intOr(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default
}

/** Averages interleaved multi-channel samples into a single mono channel. */
internal fun downmixToMono(interleaved: FloatArray, channels: Int): FloatArray {
    if (channels <= 1) return interleaved
    val frames = interleaved.size / channels
    val mono = FloatArray(frames)
    for (i in 0 until frames) {
        var sum = 0f
        val base = i * channels
        for (c in 0 until channels) sum += interleaved[base + c]
        mono[i] = sum / channels
    }
    return mono
}

/** Minimal growable float buffer that avoids boxing each sample like `MutableList<Float>`. */
internal class GrowableFloatArray(initialCapacity: Int = 1 shl 16) {
    private var data = FloatArray(initialCapacity.coerceAtLeast(16))
    var size = 0
        private set

    fun add(value: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toFloatArray(): FloatArray = data.copyOf(size)
}

/**
 * Streaming linear-interpolation resampler. State is carried across [process] calls, so audio
 * can be fed chunk by chunk without discontinuities at the chunk boundaries.
 */
internal class StreamingResampler(srcRate: Int, dstRate: Int) {
    private val passthrough = srcRate <= 0 || dstRate <= 0 || srcRate == dstRate
    private val step = if (passthrough) 1.0 else srcRate.toDouble() / dstRate.toDouble()
    private var position = 0.0
    private var previous = 0f
    private var hasPrevious = false

    fun process(input: FloatArray, count: Int, out: GrowableFloatArray) {
        if (count <= 0) return
        if (passthrough) {
            for (i in 0 until count) out.add(input[i])
            return
        }
        if (!hasPrevious) {
            // Index 0 of the virtual stream is a dummy; start at index 1 so the first
            // output sample is exactly input[0].
            previous = input[0]
            position = 1.0
            hasPrevious = true
        }
        // Virtual stream for this call: index 0 is the last sample of the previous chunk.
        val total = count + 1
        while (position + 1 < total) {
            val i = position.toInt()
            val frac = (position - i).toFloat()
            val a = if (i == 0) previous else input[i - 1]
            val b = input[i] // virtual index i + 1
            out.add(a + (b - a) * frac)
            position += step
        }
        val last = total - 1
        previous = input[count - 1]
        position -= last
    }

    /** Emits the final sample held back as the interpolation anchor for the next chunk. */
    fun flush(out: GrowableFloatArray) {
        if (!passthrough && hasPrevious) {
            out.add(previous)
        }
    }
}

/** One-shot convenience wrapper around [StreamingResampler]. */
internal fun resampleLinear(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
    val out = GrowableFloatArray(input.size.coerceAtLeast(16))
    StreamingResampler(srcRate, dstRate).process(input, input.size, out)
    return out.toFloatArray()
}
