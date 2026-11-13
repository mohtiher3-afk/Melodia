package com.example.domain

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class ImportedAudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val localFile: File,
    val fileUriString: String
)

object LocalAudioImporter {
    private const val TAG = "LocalAudioImporter"
    private const val MAX_GEMINI_AUDIO_BYTES = 14 * 1024 * 1024 // 14 MB limit for inline Gemini payload

    suspend fun importAudio(context: Context, sourceUri: Uri): Result<ImportedAudioMetadata> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Resolve original display name if available
                var originalName = "track_${UUID.randomUUID()}"
                var hasDisplayName = false
                var mimeTypeFromResolver: String? = null
                try {
                    context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val resolvedName = cursor.getString(nameIndex)
                            if (!resolvedName.isNullOrBlank()) {
                                originalName = resolvedName
                                hasDisplayName = true
                            }
                        }
                    }
                    mimeTypeFromResolver = context.contentResolver.getType(sourceUri)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed resolving filename from contentResolver", e)
                }

                // 2. Prepare destination directory & file in app internal storage
                val importedDir = File(context.filesDir, "imported_audio").apply { mkdirs() }
                val extension = getExtensionFromNameOrMime(originalName, mimeTypeFromResolver)
                val safeBaseName = originalName.substringBeforeLast(".").take(30).replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                val destinationFile = File(importedDir, "local_${System.currentTimeMillis()}_$safeBaseName.$extension")

                // 3. Stream copy bytes safely
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for audio URI"))

                // 4. Extract audio metadata using MediaMetadataRetriever
                var extractedTitle: String? = null
                var extractedArtist: String? = null
                var extractedAlbum: String? = null
                var extractedDurationMs = 0L
                var extractedMime = mimeTypeFromResolver

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destinationFile.absolutePath)
                    extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    extractedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    extractedDurationMs = durationStr?.toLongOrNull() ?: 0L
                    val metaMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    if (!metaMime.isNullOrBlank()) {
                        extractedMime = metaMime
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaMetadataRetriever failed on imported file", e)
                } finally {
                    try {
                        retriever.release()
                    } catch (ignored: Exception) {}
                }

                val finalTitle = when {
                    !extractedTitle.isNullOrBlank() -> extractedTitle.trim()
                    hasDisplayName -> originalName.substringBeforeLast(".").replace("_", " ").trim()
                    else -> "Local Audio Track"
                }

                val finalArtist = when {
                    !extractedArtist.isNullOrBlank() -> extractedArtist.trim()
                    else -> "Local Artist"
                }

                val finalMime = normalizeMimeType(extractedMime, extension)

                Result.success(
                    ImportedAudioMetadata(
                        title = finalTitle,
                        artist = finalArtist,
                        album = extractedAlbum ?: "Local Storage",
                        durationMs = extractedDurationMs,
                        mimeType = finalMime,
                        localFile = destinationFile,
                        fileUriString = destinationFile.toURI().toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error importing audio file", e)
                Result.failure(e)
            }
        }

    /**
     * Whether the file is small enough for an inline Gemini multimodal payload.
     * Larger files are still imported and usable locally, but transcription is skipped.
     */
    fun isWithinTranscriptionLimit(file: File): Boolean =
        file.exists() && file.length() in 1..MAX_GEMINI_AUDIO_BYTES.toLong()

    /**
     * Reads up to 14MB of audio bytes and returns Base64 encoded payload suitable for Gemini multimodal input.
     * Returns null for oversized files instead of silently sending a truncated (corrupt) payload.
     */
    suspend fun getBase64AudioPayload(file: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null
            if (file.length() > MAX_GEMINI_AUDIO_BYTES) {
                Log.w(TAG, "Audio file is ${file.length()} bytes, over the $MAX_GEMINI_AUDIO_BYTES inline limit")
                return@withContext null
            }
            val bytesToRead = file.length().toInt()
            val buffer = ByteArray(bytesToRead)
            file.inputStream().use { input ->
                var totalRead = 0
                while (totalRead < bytesToRead) {
                    val read = input.read(buffer, totalRead, bytesToRead - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
            }
            Base64.encodeToString(buffer, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading audio bytes for Gemini", e)
            null
        }
    }

    private fun getExtensionFromNameOrMime(name: String, mime: String?): String {
        val nameExt = name.substringAfterLast(".", "").lowercase(Locale.ROOT)
        if (nameExt in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")) {
            return nameExt
        }
        return when (mime?.lowercase(Locale.ROOT)) {
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/mp4", "audio/m4a", "audio/aac" -> "m4a"
            "audio/ogg", "audio/opus" -> "ogg"
            "audio/flac", "audio/x-flac" -> "flac"
            else -> "mp3"
        }
    }

    private fun normalizeMimeType(mime: String?, extension: String): String {
        if (!mime.isNullOrBlank() && mime.startsWith("audio/")) {
            return mime
        }
        return when (extension.lowercase(Locale.ROOT)) {
            "wav" -> "audio/wav"
            "m4a", "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            else -> "audio/mp3"
        }
    }
}
