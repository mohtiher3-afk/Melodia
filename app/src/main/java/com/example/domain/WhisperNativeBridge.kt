package com.example.domain

import android.content.Context
import android.util.Log
import java.io.File

/**
 * JNI bridge for whisper.cpp native library.
 * Loads a ggml Whisper model and runs transcription.
 *
 * Transcription accepts a 16 kHz mono float PCM buffer produced by
 * [LocalWhisperEngine.decodeAudioTo16kHzFloatPcm] and returns a JSON array of
 * segments: `[{"text":"...","t0":1234,"t1":5678}, ...]` with times in milliseconds.
 */
class WhisperNativeBridge(private val context: Context) {

    companion object {
        private const val TAG = "WhisperNativeBridge"
        init {
            System.loadLibrary("melodia_whisper")
        }
    }

    var status: String = "Not loaded"
        private set

    private var modelPath: String? = null
    private var initialized = false

    /** Initialize whisper with a model file path */
    fun initModel(path: String): String {
        modelPath = path
        status = "Initializing: $path"
        val result = nativeInit(path)
        initialized = result.startsWith("Model loaded")
        Log.i(TAG, "initModel: $result")
        status = result
        return result
    }

    /**
     * Run transcription on a 16 kHz mono float PCM buffer.
     * Returns a JSON array of segments; empty array `[]` on any failure.
     */
    fun transcribe(pcm: FloatArray): String {
        if (!initialized) {
            Log.w(TAG, "transcribe called before init")
        }
        return if (pcm.isEmpty()) "[]" else nativeTranscribeFloat(pcm, pcm.size)
    }

    /** Shutdown whisper context */
    fun shutdown() {
        nativeShutdown()
        initialized = false
        modelPath = null
        status = "Shutdown"
    }

    /** Get whisper version string */
    fun getVersion(): String = nativeGetVersion()

    /** Is the native context currently loaded? */
    fun isInitialized(): Boolean = initialized && modelPath != null

    /** Get the models directory path for whisper models */
    fun getModelsDir(): String {
        return File(context.filesDir, "models/whisper").also { it.mkdirs() }.path
    }

    // ---- JNI methods ----
    private external fun nativeInit(modelPath: String): String
    private external fun nativeTranscribeFloat(pcm: FloatArray, nSamples: Int): String
    private external fun nativeShutdown()
    private external fun nativeGetVersion(): String
}