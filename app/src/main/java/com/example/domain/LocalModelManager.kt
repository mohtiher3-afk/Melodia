package com.example.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * Manages download, storage, and status of local AI models.
 * Each model can be downloaded with a single button press from the UI.
 */
class LocalModelManager(private val appContext: Context) {

    companion object {
        private const val TAG = "LocalModelManager"

        @Volatile
        private var instance: LocalModelManager? = null

        /**
         * Process-wide singleton. Download progress must be observable from every
         * screen, so multiple instances are forbidden (they would each keep their
         * own LiveData map and the UI would watch the wrong one).
         */
        fun getInstance(context: Context): LocalModelManager =
            instance ?: synchronized(this) {
                instance ?: LocalModelManager(context.applicationContext).also { instance = it }
            }

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

        // Free, open-source models available for download.
        // Only models that shipped features actually consume are listed here.
        // (stable_diffusion_v15 was removed: nothing in the app consumes it,
        // so offering a 2.1 GB download was dishonest.)
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "gemma3_1b",
                name = "Gemma 3 1B",
                description = "Google's lightweight LLM for on-device text generation (1B params, ~500MB). Runs via LiteRT-LM.",
                downloadUrl = "https://huggingface.co/aoiandroid/Gemma3-1B-IT-litert-lm/resolve/main/gemma3-1b-it-int4.litertlm",
                expectedSizeBytes = 600_000_000L, // ~584 MB
                localPath = "models/gemma-3-1b-it-int4.litertlm"
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
            )
        )

        fun getModel(id: String): ModelInfo? = AVAILABLE_MODELS.firstOrNull { it.id == id }

        /** Format bytes for display */
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_000_000_000L -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
                bytes >= 1_000_000L -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
                bytes >= 1_000L -> String.format(Locale.US, "%.1f KB", bytes / 1_000.0)
                else -> "$bytes B"
            }
        }

        /** Alias for formatBytes for backward compatibility */
        fun formatSize(bytes: Long): String = formatBytes(bytes)
    }

    sealed interface DownloadState {
        data class Idle(val isModelPresent: Boolean) : DownloadState
        data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
        data class Completed(val modelPath: String) : DownloadState
        data class Failed(val error: String) : DownloadState
        data class Paused(val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    }

    // State for UI observation
    private val _downloadStates = mutableMapOf<String, MediatorLiveData<DownloadState>>()
    val downloadStates: Map<String, LiveData<DownloadState>>
        get() = _downloadStates

    init {
        // Initialize states for all models
        for (model in AVAILABLE_MODELS) {
            _downloadStates[model.id] = MediatorLiveData<DownloadState>(DownloadState.Idle(isModelPresent = isModelDownloaded(model)))
        }
    }

    /** Check if model file exists and has expected size */
    fun isModelDownloaded(model: ModelInfo): Boolean {
        val file = File(appContext.filesDir, model.localPath)
        return file.exists() && file.length() >= model.expectedSizeBytes * 9 / 10 // allow 10% tolerance
    }

    /** Absolute path of the in-progress partial download for a model. */
    internal fun partFile(model: ModelInfo): File = File(appContext.filesDir, model.localPath + ".part")

    /** Bytes already stored in the partial file (0 when absent). Visible for resume logic/tests. */
    fun partialBytes(model: ModelInfo): Long = partFile(model).takeIf { it.exists() }?.length() ?: 0L

    /** Get absolute path to downloaded model file */
    fun getModelPath(model: ModelInfo): String? {
        val file = File(appContext.filesDir, model.localPath)
        return if (file.exists()) file.absolutePath else null
    }

    /** Per-model cancellation flags; checked inside the transfer loop. */
    private val cancelFlags = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicBoolean>()

    private fun cancelFlag(modelId: String) =
        cancelFlags.getOrPut(modelId) { java.util.concurrent.atomic.AtomicBoolean(false) }

    /**
     * Download a model with progress updates, real cancellation and resume.
     *
     * The transfer writes to `<localPath>.part` and only renames to the final
     * path after the size check passes, so a partial download can never be
     * reported as READY. When a `.part` file exists, an HTTP Range request
     * continues from the stored bytes (falling back to a full restart when the
     * server ignores the range).
     */
    suspend fun downloadModel(model: ModelInfo, onProgress: ((Float, Long, Long) -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        val state = _downloadStates[model.id] ?: MediatorLiveData<DownloadState>(DownloadState.Idle(false)).also { _downloadStates[model.id] = it }
        val finalFile = File(appContext.filesDir, model.localPath)
        finalFile.parentFile?.mkdirs()

        // Check if already downloaded
        if (isModelDownloaded(model)) {
            state.postValue(DownloadState.Completed(finalFile.absolutePath))
            return@withContext Result.success(finalFile.absolutePath)
        }

        val flag = cancelFlag(model.id)
        flag.set(false)

        // Transient network errors ("Software caused connection abort", socket
        // resets, 5xx responses) are common on mobile. Retry with exponential
        // backoff so a flaky connection doesn't leave the model permanently
        // failed; the partial file is preserved across attempts and resumed.
        val maxAttempts = 3
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < maxAttempts) {
            attempt++
            val result = downloadAttempt(model, state, flag, onProgress)
            if (result.isSuccess) {
                return@withContext result
            }
            lastError = result.exceptionOrNull() as? Exception
            if (attempt < maxAttempts && !flag.get()) {
                val delayMs = 1000L * (1 shl (attempt - 1)) // 1s, 2s, 4s
                Log.w(TAG, "Download attempt $attempt failed for ${model.id}; retrying in ${delayMs}ms", lastError)
                state.postValue(DownloadState.Idle(isModelDownloaded(model)))
                try {
                    kotlinx.coroutines.delay(delayMs)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Cancelled during backoff: keep partial data, report paused.
                    state.postValue(DownloadState.Paused(partialBytes(model), model.expectedSizeBytes))
                    throw e
                }
            }
        }
        val error = "Download failed after $maxAttempts attempts: ${lastError?.message}"
        Log.e(TAG, error, lastError)
        state.postValue(DownloadState.Failed(error))
        Result.failure(lastError ?: IllegalStateException(error))
    }

    /**
     * Single download attempt. On failure the partial file is kept so the next
     * attempt can resume via HTTP Range; only a successful size check promotes
     * the partial file to the final path.
     */
    private suspend fun downloadAttempt(
        model: ModelInfo,
        state: MediatorLiveData<DownloadState>,
        flag: java.util.concurrent.atomic.AtomicBoolean,
        onProgress: ((Float, Long, Long) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        val partFile = partFile(model)
        val finalFile = File(appContext.filesDir, model.localPath)
        var startOffset = partialBytes(model)
        val fileIsPartial = startOffset > 0 && !isModelDownloaded(model)

        state.postValue(DownloadState.Downloading(0f, startOffset, model.expectedSizeBytes))

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val url = URL(model.downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 300_000
                if (fileIsPartial) {
                    // Ask the server to continue from the bytes we already have.
                    setRequestProperty("Range", "bytes=$startOffset-")
                }
            }

            val statusCode = connection.responseCode
            val resumed = fileIsPartial && statusCode == 206
            if (fileIsPartial && !resumed) {
                // Server ignored the Range header; discard the partial data.
                startOffset = 0
                partFile.delete()
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(partFile, /* append = */ resumed)

            val totalSize = when {
                resumed -> startOffset + connection.contentLengthLong
                connection.contentLengthLong > 0 -> connection.contentLengthLong
                else -> model.expectedSizeBytes
            }

            val buffer = ByteArray(8192)
            var downloaded = startOffset
            var lastUpdate = 0L

            while (true) {
                if (flag.get()) {
                    // Real pause: keep the partial file so the next attempt resumes.
                    state.postValue(DownloadState.Paused(downloaded, totalSize))
                    return@withContext Result.failure(
                        kotlinx.coroutines.CancellationException("Download paused by user")
                    )
                }
                ensureActive()

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

            // Verify download before promoting the partial file to the final path
            if (partFile.exists() && partFile.length() >= model.expectedSizeBytes * 9 / 10) {
                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    val error = "Could not finalize downloaded file"
                    state.postValue(DownloadState.Failed(error))
                    return@withContext Result.failure(IllegalStateException(error))
                }
                state.postValue(DownloadState.Completed(finalFile.absolutePath))
                Log.i(TAG, "Model ${model.id} downloaded successfully to ${finalFile.absolutePath}")
                Result.success(finalFile.absolutePath)
            } else {
                partFile.delete()
                val error = "Downloaded file size mismatch"
                state.postValue(DownloadState.Failed(error))
                Result.failure(IllegalStateException(error))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Coroutine scope cancelled: keep partial data for a later resume.
            state.postValue(DownloadState.Paused(startOffset + 0L, model.expectedSizeBytes))
            throw e
        } catch (e: Exception) {
            val error = "Download failed: ${e.message}"
            Log.e(TAG, error, e)
            state.postValue(DownloadState.Failed(error))
            Result.failure(e)
        } finally {
            runCatching { inputStream?.close() }
            runCatching { outputStream?.close() }
            connection?.disconnect()
        }
    }

    /**
     * Request cancellation of an ongoing download. The transfer loop observes
     * this flag on every read, so the network stops within milliseconds and
     * the partial bytes are preserved for resume.
     */
    fun cancelDownload(model: ModelInfo) {
        cancelFlag(model.id).set(true)
    }

    /** Delete a downloaded model (and any partial download data) */
    fun deleteModel(model: ModelInfo): Boolean {
        val file = File(appContext.filesDir, model.localPath)
        partFile(model).delete()
        cancelFlag(model.id).set(true)
        val deleted = file.delete()
        if (deleted) {
            _downloadStates[model.id]?.postValue(DownloadState.Idle(false))
        }
        return deleted
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