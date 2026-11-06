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
import android.util.Log
import com.example.network.WordTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

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

            // Prepare cover background bitmap
            val coverBitmap: Bitmap? = coverImagePath?.let { path ->
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }

            // Calculate total video duration in milliseconds based on word timings or audio length
            val maxTimingMs = wordTimings.maxOfOrNull { it.endTimeMs } ?: 30_000L
            val totalDurationMs = (maxTimingMs + 3000L).coerceAtLeast(10_000L)
            val totalFrames = ((totalDurationMs / 1000.0) * fps).toInt()

            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

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
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
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
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    videoCodec.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isDone = true
                    }
                }
            }

            onProgress(0.95f, "Finalizing MP4 video container...")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding lyric video", e)
            Result.failure(e)
        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping video codec", e)
            }
            try {
                muxer?.stop()
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

        // Draw 3 lines centered (Previous, Active, Next)
        val startY = height * 0.45f
        val lineSpacing = width * 0.08f

        for (offset in -1..1) {
            val targetLineIdx = activeLineIndex + offset
            if (targetLineIdx in displayLines.indices) {
                val lineText = displayLines[targetLineIdx]
                val lineY = startY + (offset * lineSpacing)

                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = if (offset == 0) width * 0.052f else width * 0.038f
                    typeface = if (offset == 0) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                    color = if (offset == 0) Color.WHITE else Color.parseColor("#80FFFFFF")
                    textAlign = Paint.Align.CENTER
                }

                if (offset == 0 && activeWord != null && lineText.contains(activeWord.text)) {
                    // Draw line with highlighted active word in red accent
                    linePaint.color = Color.parseColor("#E50914")
                    canvas.drawText(lineText, width / 2f, lineY, linePaint)
                } else {
                    canvas.drawText(lineText, width / 2f, lineY, linePaint)
                }
            }
        }

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
}
