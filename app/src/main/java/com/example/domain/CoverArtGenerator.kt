package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object CoverArtGenerator {
    private const val TAG = "CoverArtGenerator"

    /**
     * Generates cover art locally. This deliberately avoids cloud image APIs and their possible
     * charges; until an on-device diffusion model is installed, the procedural design is used.
     *
     * @return Absolute file path to the saved cover image.
     */
    private fun buildMonochromaticPrompt(
        title: String,
        genre: String,
        mood: String,
        lyrics: String
    ): String {
        val genreDesc = when (genre.lowercase()) {
            "electronic", "edm", "techno", "house" -> "futuristic electronic music with synthesizers and beats"
            "ambient", "drone" -> "atmospheric ambient soundscapes with slow evolving textures"
            "rock", "metal", "punk" -> "raw rock music with electric guitars and driving drums"
            "hip hop", "rap", "trap" -> "hip hop production with heavy beats and bass"
            "pop" -> "modern pop music with catchy melodies and polished production"
            "classical", "orchestral" -> "classical orchestral music with strings and piano"
            "jazz", "blues" -> "jazz improvisation with saxophone, piano, and double bass"
            "folk", "acoustic" -> "acoustic folk music with guitar and intimate vocals"
            else -> "music in the $genre genre"
        }

        val moodDesc = when (mood.lowercase()) {
            "dark", "melancholic", "sad", "moody" -> "dark, melancholic atmosphere"
            "energetic", "upbeat", "happy", "bright" -> "energetic, uplifting energy"
            "chill", "relaxed", "calm", "peaceful" -> "calm, peaceful atmosphere"
            "intense", "aggressive", "powerful" -> "intense, powerful energy"
            "dreamy", "ethereal", "surreal" -> "dreamy, ethereal atmosphere"
            "mysterious", "enigmatic" -> "mysterious, enigmatic mood"
            else -> "atmospheric mood"
        }

        return "Monochromatic high-contrast cover art for \"$title\", $genreDesc, $moodDesc. " +
               "Nothing OS aesthetic: pure black background (#050505), white/gray technical glyphs, " +
               "single red accent (#D71921), minimal geometric shapes, rounded rectangles, corner ticks, " +
               "concentric circles, soundwave arcs, matrix dots. No gradients, no photos, no color except red accent. " +
               "Clean technical illustration style, vector graphics, high contrast, 1:1 aspect ratio."
    }

    suspend fun generateCoverArt(
        context: Context,
        title: String,
        lyrics: String,
        genre: String,
        mood: String
    ): String = withContext(Dispatchers.IO) {
        generateProceduralNothingCover(context, title, genre, mood)
    }

    /**
     * Enhanced procedural generator with title-based seed for variety and
     * Nothing OS monochrome high- aesthetic with red accent.
     */
    private fun generateProceduralNothingCover(
        context: Context,
        title: String,
        genre: String,
        mood: String
    ): String {
        Log.d(TAG, "Starting procedural cover generation for: $title")
        try {
            val size = 1024
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // --- Color Palette ---
            val bgColor = Color.parseColor("#050505")  // Even darker pure black
            val lineColor = Color.parseColor("#252525")  // Dark gray lines
            val whiteColor = Color.parseColor("#F0F0F0")  // Off-white glyphs
            val redAccent = Color.parseColor("#D71921")  // Nothing Red

            canvas.drawColor(bgColor)

            val linePaint = Paint().apply {
                color = lineColor
                strokeWidth = 2f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            val whitePaint = Paint().apply {
                color = whiteColor
                isAntiAlias = true
                typeface = Typeface.MONOSPACE
            }

            val redPaint = Paint().apply {
                color = redAccent
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            // --- Derive seed from title + genre for variety ---
            val seed = (title + genre).hashCode().toInt()
            val rng = Random(abs(seed.toLong()) and 0x7FFFFFFF.toLong())
                        var useRedAccent = rng.nextBoolean() || mood.contains("energetic", ignoreCase = true) || mood.contains("bright", ignoreCase = true)

            // --- Dynamic margin based on seed ---
            val margin = 40f + (rng.nextFloat() * 20f).coerceIn(40f, 60f)

            // --- Technical calibration border (rounded rect) ---
            canvas.drawRoundRect(
                margin, margin, size - margin, size - margin,
                8f, 8f, linePaint
            )

            // --- Corner tick marks (dynamic count based on seed) ---
            val tickBase = 16f + (rng.nextFloat() * 20f).coerceIn(16f, 32f)
            val tickCount = 4 + (rng.nextInt(3))  // 4-6 ticks
            for (i in 0 until tickCount) {
                val angle = (i * 90f / tickCount).coerceIn(0f, 90f)
                val cosA = kotlin.math.cos(kotlin.math.PI * angle / 180f)
                val sinA = kotlin.math.sin(kotlin.math.PI * angle / 180f)
                val x1 = (margin + tickBase * cosA).toFloat()
                val y1 = (margin + tickBase * sinA).toFloat()
                val x2 = (size - margin - tickBase * cosA).toFloat()
                val y2 = (size - margin - tickBase * sinA).toFloat()
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }

            // --- Center generative glyph pattern ---
            val cx = size * 0.5f
            val cy = size * 0.48f

            // Dynamic ring count and radius based on seed
            val ringCount = 3 + rng.nextInt(3)  // 3-5 rings
            for (r in 1..ringCount) {
                val baseRadius = 80f + r * 55f
                var radiusVariance = rng.nextFloat() * 30f
                val radius = (baseRadius + radiusVariance).coerceAtMost(300f)
                val ringPaint = Paint().apply {
                    val rColor = if (r == ringCount) redAccent else lineColor
                    color = if (r == 1) lineColor else lineColor
                    strokeWidth = if (r == ringCount) 4f else if (r == 2) 3f else 2f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(cx, cy, radius, ringPaint)
            }

            // --- Concentric soundwave arcs (dynamic count) ---
            val arcCount = 2 + rng.nextInt(3)  // 2-4 arcs
            val arcPaint = Paint().apply {
                            color = whiteColor
                            strokeWidth = 5f
                            style = Paint.Style.STROKE
                            isAntiAlias = true
                            setAlpha(180)
                        }
            val arcRect = RectF(cx - 180f, cy - 180f, cx + 180f, cy + 180f)
            for (i in 0 until arcCount) {
                val startAngle = (i * 90f / arcCount).coerceIn(0f, 90f)
                val sweep = (90f / arcCount) + rng.nextFloat() * 20f
                canvas.drawArc(arcRect, startAngle, sweep, false, arcPaint)
            }

            // --- Nothing Red LED accent (dynamic position/size) ---
            val accentRadius = 12f + rng.nextFloat() * 8f
            canvas.drawCircle(
                cx + rng.nextFloat() * 40f - 20f,
                cy + rng.nextFloat() * 40f - 20f,
                accentRadius,
                redPaint
            )

            // --- Matrix/grid dots in upper quadrant (dynamic density) ---
            val dotCountX = 8 + rng.nextInt(8)  // 8-15 dots
            val dotCountY = 6 + rng.nextInt(6)  // 6-11 dots
            val dotPaint = Paint().apply {
                            color = whiteColor
                            isAntiAlias = true
                            setAlpha(120)
                        }
            for (x in (120 until size step 32).take(dotCountX)) {
                for (y in (120 until 300 step 32).take(dotCountY)) {
                    canvas.drawCircle(
                        x.toFloat(), y.toFloat(),
                        2f + rng.nextFloat() * 2f,
                        dotPaint
                    )
                }
            }

            // --- Bottom Typography & Labeling (High Contrast) ---
            whitePaint.apply {
                textSize = 28f
                color = redAccent
                isFakeBoldText = true
            }
            val displayTitle = if (title.length > 25) title.take(25) + "..." else title
            canvas.drawText(
                displayTitle,
                margin + 30f, size - margin - 40f,
                whitePaint
            )

            whitePaint.apply {
                textSize = 18f
                color = whiteColor
                isFakeBoldText = false
            }
            canvas.drawText(
                "${genre.uppercase()} // ${mood.uppercase()}",
                margin + 30f, size - margin - 10f,
                whitePaint
            )

            // --- Bottom right metadata ---
            whitePaint.apply {
                            textSize = 16f
                            color = whiteColor
                            isFakeBoldText = false
                            setAlpha(200)
                        }
            canvas.drawText(
                "NOTHING // MELODIA • ${genre.capitalize()}",
                size - margin - 280f, margin + 36f,
                whitePaint
            )

            // --- Save bitmap ---
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val cleanName = title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16)
            val file = File(coversDir, "cover_${cleanName}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val path = file.absolutePath
            Log.d(TAG, "Procedural cover generation successful: $path")
            return path
        } catch (e: Exception) {
            Log.e(TAG, "Failed procedural cover generation", e)
            // Last resort: return a placeholder path or rethrow
            throw e
        }
    }

    fun saveCoverBitmap(context: Context, bitmap: Bitmap, title: String): String {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val cleanName = title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16)
        val file = File(coversDir, "cover_${cleanName}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun saveCoverToDeviceGallery(context: Context, sourcePath: String?, title: String): Boolean {
        if (sourcePath.isNullOrBlank()) return false
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return false

        try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return false
            val safeTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(20)
            val fileName = "melodia_cover_${safeTitle}_${System.currentTimeMillis()}.png"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/MelodiaStudio")
                }
                val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri).use { out ->
                        if (out != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    return true
                }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val melodiaDir = File(imagesDir, "MelodiaStudio").apply { mkdirs() }
                val destFile = File(melodiaDir, fileName)
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cover art to device gallery", e)
        }
        return false
    }
}
