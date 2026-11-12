// Whisper.cpp Android JNI Bridge
#include <jni.h>
#include <string>
#include <android/log.h>
#include "third_party/whisper.h"

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct whisper_context *g_whisper_ctx = nullptr;
static std::string g_model_path;

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
    int n_ctx = whisper_n_text_ctx(g_whisper_ctx);
    LOGI("Model info: len=%d vocab=%d audio=%d text=%d ctx=%d",
         n_len, n_vocab, n_audio, n_text, n_ctx);
    env->ReleaseStringUTFChars(modelPath, g_model_path.c_str());
    return env->NewStringUTF("Model loaded successfully");
}

JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeTranscribe(JNIEnv *env, jobject thiz, jstring audioPath) {
    // Whisper inference is not implemented in this bridge. It must fail loudly
    // instead of returning a fake success string that callers could mistake
    // for a real transcript.
    if (g_whisper_ctx == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "Whisper model not initialized - call init first");
        return nullptr;
    }
    env->ThrowNew(env->FindClass("java/lang/UnsupportedOperationException"),
                  "Whisper transcription is not implemented in this build");
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
