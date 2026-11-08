// Whisper.cpp Android JNI Bridge
#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "third_party/whisper.h"

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct whisper_context *g_whisper_ctx = nullptr;
static std::string g_model_path;

// JSON helper: builds [{"text":"...","t0":1234,"t1":5678}, ...] from whisper segments.
// Times returned by whisper are in centiseconds; we convert to milliseconds.
static std::string buildSegmentsJson(struct whisper_context *ctx) {
    if (ctx == nullptr) return "[]";
    const int n_seg = whisper_full_n_segments(ctx);
    if (n_seg <= 0) return "[]";

    std::string out;
    out.reserve(8192);
    out.push_back('[');
    for (int i = 0; i < n_seg; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        const int64_t t0 = whisper_full_get_segment_t0(ctx, i); // centiseconds
        const int64_t t1 = whisper_full_get_segment_t1(ctx, i); // centiseconds

        if (i > 0) out.push_back(',');

        // Escape quotes/backslashes in the segment text so the JSON stays valid.
        std::string escaped;
        if (text) {
            for (const char *p = text; *p; ++p) {
                if (*p == '\\' || *p == '"') out.push_back('\\');
                out.push_back(*p);
            }
        }

        out += "{\"text\":\"";
        out += escaped;
        out += "\",\"t0\":";
        out += std::to_string(t0 * 10); // cs -> ms
        out += ",\"t1\":";
        out += std::to_string(t1 * 10); // cs -> ms
        out += '}';
    }
    out.push_back(']');
    return out;
}

static std::string runWhisper(const float *samples, int n_samples) {
    if (g_whisper_ctx == nullptr || samples == nullptr || n_samples <= 0) {
        return "[]";
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_special = false;
    params.print_timestamps = false;
    params.translate = false;
    params.no_context = true;
    params.no_timestamps = false;
    params.token_timestamps = true;
    params.thold_pt = 0.01f;
    params.thold_ptsum = 0.01f;
    params.max_len = 0;
    params.split_on_word = true;
    params.max_tokens = 0;
    params.n_threads = 4;
    params.offset_ms = 0;
    params.duration_ms = 0;

    if (whisper_full(g_whisper_ctx, params, samples, n_samples) != 0) {
        LOGE("whisper_full failed");
        return "[]";
    }
    return buildSegmentsJson(g_whisper_ctx);
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
    whisper_context_params params = whisper_context_default_params();
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
    LOGI("Model info: len=%d vocab=%d audio=%d text=%d",
         n_len, n_vocab, n_audio, n_text);
    env->ReleaseStringUTFChars(modelPath, g_model_path.c_str());
    return env->NewStringUTF("Model loaded successfully");
}

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeTranscribeFloat(JNIEnv *env, jobject thiz,
                                                                    jfloatArray pcm, jarray pcmView,
                                                                    jint n_samples) {
    if (g_whisper_ctx == nullptr) {
        return env->NewStringUTF("[]");
    }
    jfloat *samples = env->GetFloatArrayElements(pcm, nullptr);
    if (samples == nullptr) {
        return env->NewStringUTF("[]");
    }
    const std::string json = runWhisper(samples, n_samples);
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);
    return env->NewStringUTF(json.c_str());
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