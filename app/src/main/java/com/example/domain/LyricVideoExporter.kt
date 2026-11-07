package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.util.Log
import com.example.network.WordTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Computes the presentation timestamp (in nanoseconds) for a video frame rendered at a constant
 * frame rate. Frame 0 is always 0 ns; frame N advances by exactly 1_000_000_000 / fps ns so the
 * produced MP4 has a duration that matches the audio length instead of drifting with wall-clock
 * time.
 *
 * @param frameIndex zero-based frame number.
 * @param fps frames per second; must be positive.
 * @return presentation time in nanoseconds.
 */
fun framePresentationTimeNs(frameIndex: Int, fps: Int): Long {
    require(fps > 0) { "fps must be positive, was $fps" }
    require(frameIndex >= 0) { "frameIndex must be non-negative, was $frameIndex" }
    return (frameIndex.toLong() * 1_000_000_000L) / fps.toLong()
}

enum class VideoAspectRatio(val widthRatio: Int, val heightRatio: Int, val label: String) {
    PORTRAIT_9_16(9, 16, "9:16 (Story / Reel)"),
    LANDSCAPE_16_9(16, 9, "16:9 (YouTube)"),
    SQUARE_1_1(1, 1, "1:1 (Post)")
}

enum class VideoResolution(val label: String) {
    RES_1080P("1080p Full HD"),
    RES_4K("4K Ultra HD")
}

data class ExportProgressState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val outputFilePath: String? = null,
    val error: String? = null
)

object LyricVideoExporter {
    private const val TAG = "LyricVideoExporter"

    fun checkIs4KSupported(aspectRatio: VideoAspectRatio): Boolean {
        val (targetW, targetH) = getDimensions(VideoResolution.RES_4K, aspectRatio)
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            for (type in types) {
                if (type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) {
                    val caps = info.getCapabilitiesForType(type)
                    val videoCaps = caps?.videoCapabilities
                    if (videoCaps != null && videoCaps.isSizeSupported(targetW, targetH)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getDimensions(resolution: VideoResolution, aspectRatio: VideoAspectRatio): Pair<Int, Int> {
        return when (aspectRatio) {
            VideoAspectRatio.PORTRAIT_9_16 -> if (resolution == VideoResolution.RES_4K) Pair(2160, 3840) else Pair(1080, 1920)
            VideoAspectRatio.LANDSCAPE_16_9 -> if (resolution == VideoResolution.RES_4K) Pair(3840, 2160) else Pair(1920, 1080)
            VideoAspectRatio.SQUARE_1_1 -> if (resolution == VideoResolution.RES_4K) Pair(2160, 2160) else Pair(1080, 1080)
        }
    }

    suspend fun exportLyricVideo(
        context: Context,
        songTitle: String,
        lyrics: String,
        wordTimings: List<WordTiming>,
        coverImagePath: String?,
        audioUri: Uri?,
        resolution: VideoResolution,
        aspectRatio: VideoAspectRatio,
        bitrateMbps: Int = 12,
        fps: Int = 30,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var videoCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var audioExtractor: MediaExtractor? = null
        var muxerStarted = false
        try {
            var selectedRes = resolution
            if (resolution == VideoResolution.RES_4K && !checkIs4KSupported(aspectRatio)) {
                Log.w(TAG, "4K resolution requested but unsupported by hardware encoder. Falling back to 1080p.")
                onProgress(0.05f, "4K unsupported by hardware encoder — using 1080p HD fallback")
                selectedRes = VideoResolution.RES_1080P
            }

            val (width, height) = getDimensions(selectedRes, aspectRatio)
            val outputDir = File(context.cacheDir, "exported_videos").apply { mkdirs() }
            val outputFile = File(outputDir, "Melodia_${System.currentTimeMillis()}.mp4")

            onProgress(0.1f, "Initializing encoder ($width x $height @ ${fps}fps)...")

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateMbps * 1_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var audioDurationUs = 0L
            if (audioUri != null) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, audioUri, null)
                    val audioSourceTrack = (0 until extractor.trackCount).firstOrNull { index ->
                        extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                            ?.startsWith("audio/") == true
                    }
                    if (audioSourceTrack != null) {
                        val audioFormat = extractor.getTrackFormat(audioSourceTrack)
                        audioDurationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
                        audioTrackIndex = muxer.addTrack(audioFormat)
                        extractor.selectTrack(audioSourceTrack)
                        audioExtractor = extractor
                    } else {
                        extractor.release()
                        Log.w(TAG, "Imported file has no audio track; exporting silent video")
                    }
                } catch (e: Exception) {
                    extractor.release()
                    throw e
                }
            }

            // Prepare cover background bitmap
            val coverBitmap: Bitmap? = coverImagePath?.let { path ->
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }

            // Calculate total video duration in milliseconds based on word timings or audio length
            val maxTimingMs = wordTimings.maxOfOrNull { it.endTimeMs } ?: 30_000L
            val lyricsDurationMs = (maxTimingMs + 3000L).coerceAtLeast(10_000L)
            val totalDurationMs = maxOf(lyricsDurationMs, audioDurationUs / 1000L)
            val totalFrames = ((totalDurationMs / 1000.0) * fps).toInt()

            var videoTrackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()
            // Tracks the last frame index written to the muxer so the drain loop can keep the
            // frame-rate-locked cadence after the render loop ends.
            var writtenFrameIndex = 0

            val lines = lyrics.lines().filter { it.isNotBlank() }

            // Render frames
            for (frameIndex in 0 until totalFrames) {
                val currentTimeMs = ((frameIndex.toDouble() / fps.toDouble()) * 1000.0).toLong()

                val canvas = inputSurface.lockCanvas(null)
                try {
                    drawVideoFrame(
                        canvas = canvas,
                        width = width,
                        height = height,
                        songTitle = songTitle,
                        lines = lines,
                        wordTimings = wordTimings,
                        currentTimeMs = currentTimeMs,
                        coverBitmap = coverBitmap
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Poll encoder output
                var encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, 0L)
                while (encoderStatus >= 0) {
                    val encodedData = videoCodec.getOutputBuffer(encoderStatus)
                    if (encodedData != null) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                val newFormat = videoCodec.outputFormat
                                videoTrackIndex = muxer.addTrack(newFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            // Frame-rate-locked timestamp: the MP4 duration then matches the audio
                            // length instead of drifting with wall-clock time.
                            bufferInfo.presentationTimeUs =
                                framePresentationTimeNs(frameIndex, fps) / 1000L
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            writtenFrameIndex = frameIndex
                        }
                    }
                    videoCodec.releaseOutputBuffer(encoderStatus, false)
                    encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, 0L)
                }

                if (frameIndex % (fps * 2) == 0 || frameIndex == totalFrames - 1) {
                    val progressRatio = 0.1f + (0.8f * (frameIndex.toFloat() / totalFrames.toFloat()))
                    val currentSec = currentTimeMs / 1000
                    val totalSec = totalDurationMs / 1000
                    onProgress(progressRatio, "Rendering frame $frameIndex/$totalFrames (${currentSec}s / ${totalSec}s)...")
                }
            }

            // Signal end of stream
            videoCodec.signalEndOfInputStream()

            // Drain remaining frames
            var isDone = false
            while (!isDone) {
                val encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    isDone = true
                } else if (encoderStatus >= 0) {
                    val encodedData = videoCodec.getOutputBuffer(encoderStatus)
                    if (encodedData != null && bufferInfo.size > 0) {
                        if (muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            // Drain loop: keep the same frame-rate-locked cadence as the main loop.
                            bufferInfo.presentationTimeUs =
                                framePresentationTimeNs(writtenFrameIndex, fps) / 1000L
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            writtenFrameIndex++
                        }
                    }
                    videoCodec.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isDone = true
                    }
                }
            }

            if (audioExtractor != null && audioTrackIndex >= 0 && muxerStarted) {
                onProgress(0.9f, "Muxing imported audio without re-encoding...")
                val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    audioBuffer.clear()
                    val sampleSize = audioExtractor!!.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break
                    val presentationTimeUs = audioExtractor!!.sampleTime
                    if (presentationTimeUs > totalDurationMs * 1000L) break
                    audioInfo.set(0, sampleSize, presentationTimeUs, 0)
                    audioBuffer.position(0)
                    audioBuffer.limit(sampleSize)
                    muxer.writeSampleData(audioTrackIndex, audioBuffer, audioInfo)
                    audioExtractor!!.advance()
                }
            }

            onProgress(0.95f, "Finalizing MP4 video container...")
            audioExtractor?.release()
            audioExtractor = null
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding lyric video", e)
            Result.failure(e)
        } finally {
            try {
                audioExtractor?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing audio extractor", e)
            }
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping video codec", e)
            }
            try {
                if (muxerStarted) muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping muxer", e)
            }
        }
    }

    private fun drawVideoFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        songTitle: String,
        lines: List<String>,
        wordTimings: List<WordTiming>,
        currentTimeMs: Long,
        coverBitmap: Bitmap?
    ) {
        // Dark background
        canvas.drawColor(Color.parseColor("#121212"))

        // Draw cover background if available
        if (coverBitmap != null) {
            val srcRect = Rect(0, 0, coverBitmap.width, coverBitmap.height)
            val dstRect = Rect(0, 0, width, height)
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 90 // Dark overlay for text readability
            }
            canvas.drawBitmap(coverBitmap, srcRect, dstRect, bitmapPaint)
        } else {
            // Dark gradient
            val gradientPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#1F1F1F"),
                    Color.parseColor("#0A0A0A"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
        }

        // Vignette Overlay
        val vignettePaint = Paint().apply {
            color = Color.argb(160, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

        // Draw Header Title & Branding
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E50914")
            textSize = width * 0.035f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MELODIA STUDIO", width / 2f, height * 0.08f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.045f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(songTitle, width / 2f, height * 0.13f, titlePaint)

        // Find active word timing
        val activeWord = wordTimings.firstOrNull { currentTimeMs in it.startTimeMs..it.endTimeMs }

        // Find active line
        val displayLines = lines.filter { !it.startsWith("[") || !it.endsWith("]") }
        var activeLineIndex = 0
        if (activeWord != null) {
            val foundIdx = displayLines.indexOfFirst { line -> line.contains(activeWord.text) }
            if (foundIdx >= 0) activeLineIndex = foundIdx
        } else {
            // Estimate line by time ratio
            val totalDurationMs = wordTimings.maxOfOrNull { it.endTimeMs } ?: 30_000L
            if (totalDurationMs > 0 && displayLines.isNotEmpty()) {
                val ratio = (currentTimeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                activeLineIndex = (ratio * (displayLines.size - 1)).toInt()
            }
        }

        // Draw 5 lines centered (2 Previous, Active, 2 Next)
        val startY = height * 0.45f
        val lineSpacing = width * 0.08f

        for (offset in -2..2) {
            val targetLineIdx = activeLineIndex + offset
            if (targetLineIdx in displayLines.indices) {
                val lineText = displayLines[targetLineIdx]
                val lineY = startY + (offset * lineSpacing)

                val linePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    when (offset) {
                        0 -> {
                            textSize = width * 0.052f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            color = Color.WHITE
                        }
                        -1, 1 -> {
                            textSize = width * 0.042f
                            typeface = Typeface.DEFAULT
                            color = Color.parseColor("#CCFFFFFF")
                        }
                        -2, 2 -> {
                            textSize = width * 0.034f
                            typeface = Typeface.DEFAULT
                            color = Color.parseColor("#80FFFFFF")
                        }
                    }
                }

                val activeWordStart = if (offset == 0 && activeWord != null) {
                    lineText.indexOf(activeWord.text)
                } else {
                    -1
                }
                val lyricText = SpannableString(lineText)
                if (activeWordStart >= 0 && activeWord != null) {
                    val pulse = (0.5f + 0.5f * kotlin.math.sin(currentTimeMs / 180.0)).toFloat()
                    val gradientPhase = (currentTimeMs % 2400L).toFloat() / 2400f
                    lyricText.setSpan(
                        ActiveKaraokeWordSpan(pulse, gradientPhase),
                        activeWordStart,
                        activeWordStart + activeWord.text.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                val textLayout = android.text.StaticLayout.Builder
                    .obtain(lyricText, 0, lyricText.length, linePaint, (width * 0.82f).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .build()
                canvas.save()
                canvas.translate(width / 2f - textLayout.width / 2f, lineY - textLayout.height / 2f)
                textLayout.draw(canvas)
                canvas.restore()
            }
        }

        val activeWordProgress = activeWord?.let { word ->
            ((currentTimeMs - word.startTimeMs).toFloat() /
                (word.endTimeMs - word.startTimeMs).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        } ?: 0f
        drawWordReactiveWaveform(
            canvas = canvas,
            width = width,
            height = height,
            currentTimeMs = currentTimeMs,
            activeWord = activeWord,
            wordProgress = activeWordProgress
        )

        // Progress Bar at Bottom
        val totalDurationMs = wordTimings.maxOfOrNull { it.endTimeMs } ?: 30_000L
        val progressRatio = (currentTimeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        
        val barHeight = height * 0.008f
        val barY = height * 0.92f
        val barPaintBg = Paint().apply { color = Color.parseColor("#33FFFFFF") }
        val barPaintFg = Paint().apply { color = Color.parseColor("#E50914") }

        canvas.drawRect(width * 0.1f, barY, width * 0.9f, barY + barHeight, barPaintBg)
        canvas.drawRect(width * 0.1f, barY, width * 0.1f + (width * 0.8f * progressRatio), barY + barHeight, barPaintFg)
    }

    private fun drawWordReactiveWaveform(
        canvas: Canvas,
        width: Int,
        height: Int,
        currentTimeMs: Long,
        activeWord: WordTiming?,
        wordProgress: Float
    ) {
        val barCount = 56
        val left = width * 0.1f
        val right = width * 0.9f
        val centerY = height * 0.79f
        val maxAmplitude = height * 0.045f
        val step = (right - left) / (barCount - 1)
        val playheadProgress = if (activeWord != null) {
            wordProgress
        } else {
            (currentTimeMs % 1800L).toFloat() / 1800f
        }
        val playhead = playheadProgress * (barCount - 1)
        val wordSeed = (activeWord?.text?.hashCode() ?: 0).toFloat() * 0.001f
        val activity = if (activeWord != null) 0.55f + 0.45f * kotlin.math.sin(Math.PI * wordProgress).toFloat() else 0.18f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.004f).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
        }

        for (index in 0 until barCount) {
            val distanceFromPlayhead = kotlin.math.abs(index - playhead)
            val playheadGlow = kotlin.math.exp(-distanceFromPlayhead * distanceFromPlayhead / 45f).toFloat()
            val oscillation = kotlin.math.abs(
                kotlin.math.sin(index * 0.71 + currentTimeMs / 105.0 + wordSeed)
            ).toFloat()
            val wordShape = 0.25f + 0.75f * kotlin.math.abs(
                kotlin.math.sin(index * 0.19 + wordSeed * 2.0)
            ).toFloat()
            val amplitude = maxAmplitude * (0.12f + 0.58f * oscillation * wordShape * activity + 0.42f * playheadGlow)
            val alpha = (100 + 155 * (0.35f * activity + 0.65f * playheadGlow)).toInt().coerceIn(0, 255)
            paint.color = if (playheadGlow > 0.48f) {
                Color.argb(alpha, 255, 145, 195)
            } else {
                Color.argb(alpha, 229, 9, 20)
            }
            val x = left + index * step
            canvas.drawLine(x, centerY - amplitude, x, centerY + amplitude, paint)
        }
    }

    private class ActiveKaraokeWordSpan(
        private val pulse: Float,
        private val gradientPhase: Float
    ) : ReplacementSpan() {
        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int = (paint.measureText(text, start, end) + HORIZONTAL_PADDING * 2).toInt()

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val label = text.subSequence(start, end).toString()
            val labelWidth = paint.measureText(label)
            val pill = RectF(
                x,
                y + paint.fontMetrics.ascent - VERTICAL_PADDING,
                x + labelWidth + HORIZONTAL_PADDING * 2,
                y + paint.fontMetrics.descent + VERTICAL_PADDING
            )
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80 + (45 * pulse).toInt(), 229, 9, 20)
                setShadowLayer(10f + 14f * pulse, 0f, 0f, Color.argb(150, 229, 9, 20))
            }
            canvas.drawRoundRect(pill, 14f, 14f, backgroundPaint)
            val textPaint = TextPaint(paint).apply {
                typeface = Typeface.create(paint.typeface, Typeface.BOLD)
                setShadowLayer(4f + 6f * pulse, 0f, 0f, Color.rgb(255, 70, 80))
                shader = LinearGradient(
                    x - labelWidth * gradientPhase,
                    y.toFloat(),
                    x + labelWidth * (1f - gradientPhase),
                    y.toFloat(),
                    intArrayOf(
                        Color.rgb(255, 45, 65),
                        Color.rgb(255, 125, 185),
                        Color.WHITE,
                        Color.rgb(255, 45, 65)
                    ),
                    null,
                    Shader.TileMode.MIRROR
                )
            }
            canvas.drawText(label, x + HORIZONTAL_PADDING, y.toFloat(), textPaint)
        }

        private companion object {
            const val HORIZONTAL_PADDING = 12f
            const val VERTICAL_PADDING = 7f
        }
    }
}
