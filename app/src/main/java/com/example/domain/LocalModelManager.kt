package com.example.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages download, storage, and status of local AI models.
 * Each model can be downloaded with a single button press from the UI.
 */
class LocalModelManager(private val appContext: Context) {

    companion object {
        private const val TAG = "LocalModelManager"

        // Model definitions with download URLs (HuggingFace / GitHub releases)
        data class ModelInfo(
            val id: String,
            val name: String,
            val description: String,
            val downloadUrl: String,
            val expectedSizeBytes: Long,
            val localPath: String,
            val version: String = "1.0"
        )

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "gemma3_1b",
                name = "Gemma 3 1B",
                description = "Google's lightweight 1B parameter LLM optimized for on-device chat and text generation.",
                downloadUrl = "https://huggingface.co/aoiandroid/Gemma3-1B-IT-litert-lm/resolve/main/gemma3-1b-it-int4.litertlm",
                expectedSizeBytes = 600_000_000L,
                localPath = "models/gemma-3-1b-it-int4.litertlm"
            ),
            ModelInfo(
                id = "whisper_tiny",
                name = "Whisper Tiny",
                description = "Ultra-fast speech recognition (~32x realtime). Best for short phrases on slower devices.",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                expectedSizeBytes = 75_000_000L,
                localPath = "models/whisper/ggml-tiny.bin"
            ),
            ModelInfo(
                id = "whisper_base",
                name = "Whisper Base",
                description = "Balanced speech recognition (~16x realtime). Good accuracy for general dictation.",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                expectedSizeBytes = 145_000_000L,
                localPath = "models/whisper/ggml-base.bin"
            ),
            ModelInfo(
                id = "stable_diffusion",
                name = "Stable Diffusion v1.5",
                description = "Text-to-image generation model. Generates 512x512 images from text prompts.",
                downloadUrl = "https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5/resolve/main/v1-5-pruned.ckpt",
                expectedSizeBytes = 2_100_000_000L,
                localPath = "models/stable_diffusion/v1-5-pruned.ckpt"
            )
        )

        fun getModel(id: String): ModelInfo? = AVAILABLE_MODELS.find { it.id == id }

        fun formatSize(bytes: Long): String = when {
            bytes >= 1_000_000_000L -> String.format("%.1f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> String.format("%.1f MB", bytes / 1_000_000.0)
            bytes >= 1_000L -> String.format("%.1f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }

        /** Alias for formatSize for backward compatibility */
        fun formatBytes(bytes: Long): String = formatSize(bytes)
    }

    sealed class DownloadState {
        data class Idle(val downloaded: Boolean) : DownloadState()
        data class Downloading(
            val progress: Float,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState()
        data class Paused(
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState()
        data class Complete(val path: String) : DownloadState()
        data class Failed(val error: String) : DownloadState()
    }

    // Per-model download state tracking
    private val _downloadStates = mutableMapOf<String, MutableLiveData<DownloadState>>()
    val downloadStates: Map<String, LiveData<DownloadState>>
        get() = _downloadStates

    private fun getDownloadState(modelId: String): MutableLiveData<DownloadState> {
        return _downloadStates.getOrPut(modelId) { MutableLiveData<DownloadState>().apply {
            value = DownloadState.Idle(isModelDownloaded(getModel(modelId)!!))
        } }
    }

    fun isModelDownloaded(model: ModelInfo): Boolean {
        val file = File(appContext.filesDir, model.localPath)
        return file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10
    }

    fun getModelPath(model: ModelInfo): String? {
        val file = File(appContext.filesDir, model.localPath)
        return if (isModelDownloaded(model)) file.absolutePath else null
    }

    suspend fun downloadModel(model: ModelInfo): Result<String> = withContext(Dispatchers.IO) {
        val file = File(appContext.filesDir, model.localPath)
        file.parentFile?.mkdirs()

        val downloadState = getDownloadState(model.id)

        if (file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10) {
            downloadState.value = DownloadState.Complete(file.absolutePath)
            return@withContext Result.success(file.absolutePath)
        }

        val totalSize = model.expectedSizeBytes
        downloadState.value = DownloadState.Downloading(0f, 0L, totalSize)
        try {
            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.doInput = true
            connection.connect()

            val actualTotalSize = connection.contentLengthLong.takeIf { it > 0 } ?: model.expectedSizeBytes
            connection.getInputStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var lastUpdate = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        // Throttle progress updates to ~10/sec
                        if (now - lastUpdate > 100) {
                            val progress = if (actualTotalSize > 0) downloaded.toFloat() / actualTotalSize else 0f
                            downloadState.value = DownloadState.Downloading(progress.coerceIn(0f, 1f), downloaded, actualTotalSize)
                            lastUpdate = now
                        }
                    }
                    output.flush()
                }
            }

            if (file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10) {
                downloadState.value = DownloadState.Complete(file.absolutePath)
                Result.success(file.absolutePath)
            } else {
                val error = "Downloaded file size mismatch"
                downloadState.value = DownloadState.Failed(error)
                Result.failure(IllegalStateException(error))
            }
        } catch (e: Exception) {
            val error = "Download failed: ${e.message}"
            Log.e(TAG, error, e)
            downloadState.value = DownloadState.Failed(error)
            Result.failure(e)
        }
    }

    /** Delete a downloaded model */
    fun deleteModel(model: ModelInfo): Boolean {
        val file = File(appContext.filesDir, model.localPath)
        val deleted = file.delete()
        if (deleted) {
            getDownloadState(model.id).value = DownloadState.Idle(false)
        }
        return deleted
    }

    /** Cancel an ongoing download */
    fun cancelDownload(model: ModelInfo): Boolean {
        val downloadState = _downloadStates[model.id]
        if (downloadState != null) {
            val currentState = downloadState.value
            if (currentState is DownloadState.Downloading) {
                downloadState.value = DownloadState.Paused(
                    downloadedBytes = currentState.downloadedBytes,
                    totalBytes = currentState.totalBytes
                )
                return true
            }
        }
        return false
    }

    /** Get total storage used by downloaded models */
    fun getTotalStorageUsed(): Long {
        return AVAILABLE_MODELS.sumOf { model ->
            val file = File(appContext.filesDir, model.localPath)
            if (file.exists()) file.length() else 0L
        }
    }

    /** Verify a downloaded model against an optional SHA-256 checksum */
    fun verifyModel(model: ModelInfo, expectedSha256: String? = null): Boolean {
        val file = File(appContext.filesDir, model.localPath)
        if (!file.exists()) return false
        if (expectedSha256 == null) return isModelDownloaded(model)
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                var n: Int
                while (input.read(buf).also { n = it } > 0) {
                    digest.update(buf, 0, n)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == expectedSha256
        } catch (e: Exception) {
            Log.w(TAG, "Checksum verification failed for ${model.id}", e)
            false
        }
    }
}