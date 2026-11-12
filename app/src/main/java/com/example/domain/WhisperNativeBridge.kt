package com.example.domain

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * JNI bridge for whisper.cpp native library.
 * Loads a ggml Whisper model and runs transcription.
 */
class WhisperNativeBridge(private val context: Context) {

    companion object {
        private const val TAG = "WhisperNativeBridge"
        init {
            System.loadLibrary("melodia_whisper")
        }
    }

    private val _status = MutableLiveData<String>("Not loaded")
    val status: LiveData<String> get() = _status

    private var modelPath: String? = null
    private var initialized = false

    /** Initialize whisper with a model file path */
    fun initModel(path: String): String {
        modelPath = path
        _status.postValue("Initializing: $path")
        val result = nativeInit(path)
        initialized = true
        Log.i(TAG, "initModel: $result")
        _status.postValue(result)
        return result
    }

    /** Run transcription on 16kHz mono float PCM data */
    fun transcribePcm(pcmData: FloatArray, sampleRate: Int = 16000): String {
        if (!initialized) {
            Log.w(TAG, "transcribePcm called before init")
        }
        if (sampleRate != 16000) {
            throw IllegalArgumentException("Only 16000 Hz sample rate is supported")
        }
        if (pcmData.isEmpty()) {
            throw IllegalArgumentException("Empty audio data")
        }
        return nativeTranscribePcm(pcmData, sampleRate)
    }

    /** @deprecated Use transcribePcm with PCM data instead */
    @Deprecated("Use transcribePcm with PCM data", ReplaceWith("transcribePcm(pcmData, sampleRate)"))
    fun transcribe(audioPath: String): String {
        if (!initialized) {
            Log.w(TAG, "transcribe called before init")
        }
        return nativeTranscribe(audioPath)
    }

    /** Shutdown whisper context */
    fun shutdown() {
        nativeShutdown()
        initialized = false
        modelPath = null
        _status.postValue("Shutdown")
    }

    /** Get whisper version string */
    fun getVersion(): String = nativeGetVersion()

    // ---- JNI methods ----
    private external fun nativeInit(modelPath: String): String
    private external fun nativeTranscribePcm(pcmData: FloatArray, sampleRate: Int): String
    private external fun nativeTranscribe(audioPath: String): String
    private external fun nativeShutdown()
    private external fun nativeGetVersion(): String

    /** Get the models directory path for whisper models */
    fun getModelsDir(): String {
        return java.io.File(context.filesDir, "models/whisper").also { it.mkdirs() }.path
    }
}
