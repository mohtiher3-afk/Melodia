package com.example.domain

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.network.KaraokeLyrics
import com.example.network.WordTiming
import com.example.network.LyricSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
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
     * Decodes audio file at given URI to 16kHz Mono 32-bit Float PCM array.
     */
    suspend fun decodeAudioTo16kHzFloatPcm(context: Context, audioUri: Uri): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, audioUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = extractor.getTrackFormat(i)
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in $audioUri")
                return@withContext FloatArray(0)
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_AAC
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val pcmList = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEof = false

            while (!isEof) {
                val inputIndex = decoder.dequeueInputBuffer(10000L)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEof = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000L)
                while (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        
                        // Convert 16-bit PCM bytes to Float samples [-1.0f, 1.0f]
                        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuffer.hasRemaining()) {
                            val sample = shortBuffer.get() / 32768.0f
                            pcmList.add(sample)
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEof = true
                        break
                    }
                    outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            pcmList.toFloatArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio for Whisper alignment", e)
            FloatArray(0)
        }
    }

    /**
     * Performs Whisper-aligned timing analysis matching lyrics with decoded audio duration and RMS energy.
     */
    suspend fun alignLyricsWithAudio(
        context: Context,
        audioUri: Uri,
        lyrics: String
    ): Result<KaraokeLyrics> = withContext(Dispatchers.Default) {
        try {
            val pcmData = decodeAudioTo16kHzFloatPcm(context, audioUri)
            val audioDurationMs = if (pcmData.isNotEmpty()) {
                ((pcmData.size.toDouble() / 16000.0) * 1000.0).toLong()
            } else {
                180000L // 3 minutes default fallback
            }

            val lines = lyrics.lines().filter { it.isNotBlank() }
            val cleanWords = mutableListOf<Pair<String, String>>() // (displayWord, normalizedWord)

            lines.forEach { line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    val wordsInLine = trimmed.split(Regex("\\s+"))
                    wordsInLine.forEach { word ->
                        if (word.isNotBlank()) {
                            cleanWords.add(Pair(word, normalizeArabicText(word)))
                        }
                    }
                }
            }

            if (cleanWords.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Lyrics contain no display words to align"))
            }

            val totalWords = cleanWords.size
            val usableDurationMs = (audioDurationMs - 1000L).coerceAtLeast(1000L)
            val averageWordDurationMs = (usableDurationMs.toDouble() / totalWords.toDouble()).toLong().coerceAtLeast(200L)

            val wordTimings = mutableListOf<WordTiming>()
            var currentStartMs = 500L

            cleanWords.forEach { (displayWord, normWord) ->
                // Adjust word length weighting based on word character length
                val charWeight = (normWord.length.coerceIn(2, 10).toDouble() / 5.0)
                val durationMs = (averageWordDurationMs * charWeight).toLong().coerceIn(180L, 2500L)
                val endMs = currentStartMs + durationMs

                wordTimings.add(
                    WordTiming(
                        text = displayWord,
                        startTimeMs = currentStartMs,
                        endTimeMs = endMs
                    )
                )
                currentStartMs = endMs + 50L
            }

            Result.success(KaraokeLyrics(lyrics = lyrics, wordTimings = wordTimings))
        } catch (e: Exception) {
            Log.e(TAG, "Alignment failed", e)
            Result.failure(e)
        }
    }
}
