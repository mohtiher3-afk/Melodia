// Whisper.cpp Android JNI Bridge
#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>
#include <android/log.h>
#include "third_party/whisper.h"

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct whisper_context *g_whisper_ctx = nullptr;
static std::string g_model_path;

// Helper: escape JSON string
static std::string jsonEscape(const std::string& input) {
    std::ostringstream oss;
    for (char c : input) {
        switch (c) {
            case '"':  oss << "\\\""; break;
            case '\\': oss << "\\\\"; break;
            case '\b': oss << "\\b"; break;
            case '\f': oss << "\\f"; break;
            case '\n': oss << "\\n"; break;
            case '\r': oss << "\\r"; break;
            case '\t': oss << "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    oss << "\\u" << std::hex << std::setw(4) << std::setfill('0') << (int)c;
                } else {
                    oss << c;
                }
        }
    }
    return oss.str();
}

// Helper: build JSON result from whisper context
static std::string buildJsonResult(struct whisper_context *ctx) {
    std::ostringstream json;
    json << "{";

    // Full text
    int n_segments = whisper_full_n_segments(ctx);
    std::string full_text;
    for (int i = 0; i < n_segments; ++i) {
        const char* seg_text = whisper_full_get_segment_text(ctx, i);
        if (seg_text) {
            full_text += seg_text;
        }
    }
    json << "\"text\":\"" << jsonEscape(full_text) << "\",";

    // Segments with word timestamps
    json << "\"segments\":[";
    for (int i = 0; i < n_segments; ++i) {
        if (i > 0) json << ",";

        int64_t t0 = whisper_full_get_segment_t0(ctx, i);  // centiseconds
        int64_t t1 = whisper_full_get_segment_t1(ctx, i);
        const char* seg_text = whisper_full_get_segment_text(ctx, i);
        int n_tokens = whisper_full_n_tokens(ctx, i);

        json << "{";
        json << "\"start\":" << (t0 * 10) << ",";  // Convert to milliseconds
        json << "\"end\":" << (t1 * 10) << ",";
        json << "\"text\":\"" << jsonEscape(seg_text ? seg_text : "") << "\",";

        // Word-level tokens
        json << "\"words\":[";
        for (int j = 0; j < n_tokens; ++j) {
            if (j > 0) json << ",";

            whisper_token_data td = whisper_full_get_token_data(ctx, i, j);
            const char* token_text = whisper_full_get_token_text(ctx, i, j);

            json << "{";
            json << "\"word\":\"" << jsonEscape(token_text ? token_text : "") << "\",";
            json << "\"start\":" << (td.t0 * 10) << ",";  // centiseconds to ms
            json << "\"end\":" << (td.t1 * 10) << ",";
            json << "\"prob\":" << td.p;
            json << "}";
        }
        json << "]";
        json << "}";
    }
    json << "]";

    // Language detection
    int lang_id = whisper_full_lang_id(ctx);
    const char* lang_str = whisper_lang_str(lang_id);
    json << ",\"language\":\"" << (lang_str ? lang_str : "auto") << "\"";

    json << "}";
    return json.str();
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeInit(JNIEnv *env, jobject thiz, jstring modelPath) {
    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }
    g_model_path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading whisper model: %s", g_model_path.c_str());

    // Enable DTW token timestamps for word-level timing
    whisper_context_params params = whisper_context_default_params();
    params.dtw_token_timestamps = true;
    params.dtw_aheads_preset = WHISPER_AHEADS_BASE;

    g_whisper_ctx = whisper_init_from_file_with_params(g_model_path.c_str(), params);
    if (g_whisper_ctx == nullptr) {
        LOGE("Failed to load whisper model");
        env->ReleaseStringUTFChars(modelPath, g_model_path.c_str());
        return env->NewStringUTF("Failed to load model");
    }
    int n_len = whisper_n_len(g_whisper_ctx);
    int n_vocab = whisper_n_vocab(g_whisper_ctx);
    int n_audio = whisper_n_audio_ctx(g_whisper_ctx);
    int n_text = whisper_n_text_ctx(g_whisper_ctx);
    int n_ctx = whisper_n_text_ctx(g_whisper_ctx);
    LOGI("Model info: len=%d vocab=%d audio=%d text=%d ctx=%d",
         n_len, n_vocab, n_audio, n_text, n_ctx);
    env->ReleaseStringUTFChars(modelPath, g_model_path.c_str());
    return env->NewStringUTF("Model loaded successfully");
}

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeTranscribePcm(JNIEnv *env, jobject thiz, jfloatArray pcmData, jint sampleRate) {
    if (g_whisper_ctx == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "Whisper model not initialized - call init first");
        return nullptr;
    }

    if (sampleRate != 16000) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Only 16000 Hz sample rate is supported");
        return nullptr;
    }

    jsize n_samples = env->GetArrayLength(pcmData);
    if (n_samples == 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Empty audio data");
        return nullptr;
    }

    // Validate audio duration (whisper expects at least 100ms)
    const float min_duration_ms = 100.0f;
    const float audio_duration_ms = (n_samples * 1000.0f) / sampleRate;
    if (audio_duration_ms < min_duration_ms) {
        LOGE("Audio too short: %.1f ms (min %.1f ms)", audio_duration_ms, min_duration_ms);
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "Audio too short for transcription (minimum 100ms)");
        return nullptr;
    }

    // Get PCM data
    jfloat* pcm_elements = env->GetFloatArrayElements(pcmData, nullptr);
    if (pcm_elements == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"),
                      "Failed to access audio data");
        return nullptr;
    }

    LOGI("Starting transcription: %d samples (%.1f ms) at %d Hz", n_samples, audio_duration_ms, sampleRate);

    // Configure transcription parameters
    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.token_timestamps = true;
    wparams.detect_language = true;
    wparams.language = "auto";
    wparams.n_threads = 4;
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.suppress_blank = true;
    wparams.suppress_nst = true;
    wparams.temperature = 0.0f;
    wparams.max_initial_ts = 1.0f;

    // Run transcription
    int result = whisper_full(g_whisper_ctx, wparams, pcm_elements, n_samples);

    env->ReleaseFloatArrayElements(pcmData, pcm_elements, JNI_ABORT);

    if (result != 0) {
        LOGE("whisper_full failed with code: %d", result);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                      "Transcription failed");
        return nullptr;
    }

    // Build JSON result
    std::string json_result = buildJsonResult(g_whisper_ctx);
    LOGI("Transcription complete, result length: %zu", json_result.length());

    return env->NewStringUTF(json_result.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeTranscribe(JNIEnv *env, jobject thiz, jstring audioPath) {
    // Kept for backward compatibility - throws exception
    if (g_whisper_ctx == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "Whisper model not initialized - call init first");
        return nullptr;
    }
    env->ThrowNew(env->FindClass("java/lang/UnsupportedOperationException"),
                  "Use nativeTranscribePcm for PCM input instead of file path");
    return nullptr;
}

JNIEXPORT void JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeShutdown(JNIEnv *env, jobject thiz) {
    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
        g_model_path.clear();
        LOGI("Whisper context freed");
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeGetVersion(JNIEnv *env, jobject thiz) {
    const char *version = whisper_version();
    return env->NewStringUTF(version ? version : "unknown");
}

} // extern "C"
