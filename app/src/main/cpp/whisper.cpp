// Placeholder for whisper.cpp integration (NDK).
// Full implementation requires whisper.cpp source + model loading.
#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_domain_WhisperNativeBridge_nativeVersion(JNIEnv* env, jobject /*thiz*/) {
    return env->NewStringUTF("melodia_whisper stub 0.1");
}