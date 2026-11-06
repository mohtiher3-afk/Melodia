package com.example.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages download, storage, and status of local AI models.
 * Each model can be downloaded with a single button press from the UI.
 */
class LocalModelManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalModelManager"

        // Model definitions with download URLs (HuggingFace / GitHub releases)
        data class ModelInfo(
            val id: String,
            val name: String,
            val description: String,
            val downloadUrl: String,
            val expectedSizeBytes: Long,
            val localPath: String, // relative to filesDir
            val version: String = "1.0"
        )

        // Free, open-source models available for download
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "gemma3_1b",
                name = "Gemma 3 1B",
                description = "Google's lightweight LLM for on-device text generation (1B params, ~500MB). Runs via LiteRT-LM.",
                downloadUrl = "https://huggingface.co/google/gemma-3-1b-it-litertlm/resolve/main/gemma-3-1b-it.litertlm",
                expectedSizeBytes = 520_000_000L, // ~520 MB
                localPath = "models/gemma-3-1b-it.litertlm"
            ),
            ModelInfo(
                id = "whisper_tiny",
                name = "Whisper Tiny",
                description = "OpenAI's tiny speech-to-text model (~75MB). Fast, good for short audio. MIT license.",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                expectedSizeBytes = 75_000_000L,
                localPath = "models/whisper/ggml-tiny.bin"
            ),
            ModelInfo(
                id = "whisper_base",
                name = "Whisper Base",
                description = "OpenAI's base speech-to-text model (~142MB). Better accuracy, still fast. MIT license.",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                expectedSizeBytes = 142_000_000L,
                localPath = "models/whisper/ggml-base.bin"
            ),
            ModelInfo(
                id = "stable_diffusion_v15",
                name = "Stable Diffusion v1.5",
                description = "Image generation model for cover art (~2GB). Runs via MediaPipe. Requires conversion to MediaPipe format.",
                downloadUrl = "https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5/resolve/main/v1-5-pruned.ckpt",
                expectedSizeBytes = 2_100_000_000L, // ~2.1 GB
                localPath = "models/stable_diffusion/v1-5-pruned.ckpt"
            )
        )

        fun getModel(id: String): ModelInfo? = AVAILABLE_MODELS.firstOrNull { it.id == id }

        /** Format bytes for display */
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_000_000_000L -> String.format("%.1f GB", bytes / 1_000_000_000.0)
                bytes >= 1_000_000L -> String.format("%.1f MB", bytes / 1_000_000.0)
                bytes >= 1_000L -> String.format("%.1f KB", bytes / 1_000.0)
                else -> "$bytes B"
            }
        }
    }

    // State for UI observation
    private val _downloadStates = mutableMapOf<String, MediatorLiveData<DownloadState>>()
    val downloadStates: Map<String, LiveData<DownloadState>>
        get() = _downloadStates

    sealed interface DownloadState {
        data class Idle(val isModelPresent: Boolean) : DownloadState
        data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
        data class Completed(val modelPath: String) : DownloadState
        data class Failed(val error: String) : DownloadState
        data class Paused(val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    }

    init {
        // Initialize states for all models
        for (model in AVAILABLE_MODELS) {
            _downloadStates[model.id] = MediatorLiveData<DownloadState>(DownloadState.Idle(isModelPresent = isModelDownloaded(model)))
        }
    }

    /** Check if model file exists and has expected size */
    fun isModelDownloaded(model: ModelInfo): Boolean {
        val file = File(context.filesDir, model.localPath)
        return file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10 // allow 10% tolerance
    }

    /** Get absolute path to downloaded model file */
    fun getModelPath(model: ModelInfo): String? {
        val file = File(context.filesDir, model.localPath)
        return if (file.exists()) file.absolutePath else null
    }

    /** Download a model with progress updates */
    suspend fun downloadModel(model: ModelInfo, onProgress: ((Float, Long, Long) -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        val state = _downloadStates[model.id] ?: MediatorLiveData<DownloadState>(DownloadState.Idle(false)).also { _downloadStates[model.id] = it }
        val file = File(context.filesDir, model.localPath)
        file.parentFile?.mkdirs()

        // Check if already downloaded
        if (isModelDownloaded(model)) {
            state.postValue(DownloadState.Completed(file.absolutePath))
            return@withContext Result.success(file.absolutePath)
        }

        state.postValue(DownloadState.Downloading(0f, 0L, model.expectedSizeBytes))

        try {
            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30_000
            connection.readTimeout = 300_000

            val totalSize = connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(8192)
            var downloaded = 0L
            var lastUpdate = 0L

            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                outputStream.write(buffer, 0, read)
                downloaded += read

                // Throttle progress updates to ~10/sec
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 100) {
                    val progress = if (totalSize > 0) downloaded.toFloat() / totalSize else 0f
                    state.postValue(DownloadState.Downloading(progress, downloaded, totalSize))
                    onProgress?.invoke(progress, downloaded, totalSize)
                    lastUpdate = now
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Verify download
            if (file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10) {
                state.postValue(DownloadState.Completed(file.absolutePath))
                Log.i(TAG, "Model ${model.id} downloaded successfully to ${file.absolutePath}")
                Result.success(file.absolutePath)
            } else {
                file.delete()
                val error = "Downloaded file size mismatch"
                state.postValue(DownloadState.Failed(error))
                Result.failure(IllegalStateException(error))
            }
        } catch (e: Exception) {
            file.delete()
            val error = "Download failed: ${e.message}"
            Log.e(TAG, error, e)
            state.postValue(DownloadState.Failed(error))
            Result.failure(e)
        }
    }

    /** Cancel an ongoing download */
    fun cancelDownload(model: ModelInfo) {
        val state = _downloadStates[model.id]
        val current = state?.value
        if (current is DownloadState.Downloading) {
            state?.postValue(DownloadState.Paused(current.downloadedBytes, current.totalBytes))
        }
    }

    /** Delete a downloaded model */
    fun deleteModel(model: ModelInfo): Boolean {
        val file = File(context.filesDir, model.localPath)
        val deleted = file.delete()
        if (deleted) {
            _downloadStates[model.id]?.postValue(DownloadState.Idle(false))
        }
        return deleted
    }

    /** Get total storage used by downloaded models */
    fun getTotalStorageUsed(): Long {
        return AVAILABLE_MODELS.sumOf { model ->
            val file = File(context.filesDir, model.localPath)
            if (file.exists()) file.length() else 0L
        }
    }
}