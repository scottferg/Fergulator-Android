#include "audio_android.h"


int outBufSamples = 1024;

static JavaVM* jvm;
static jclass jclazz;
static jmethodID jplaySamples;
static jshortArray jsampleBuffer;


int startAudio(JNIEnv* env, jclass clazz, int minBufferSize) {
    outBufSamples = minBufferSize;

    (*env)->GetJavaVM(env, &jvm);
    jclazz = (*env)->NewGlobalRef(env, clazz);
    jplaySamples = (*env)->GetStaticMethodID(env, jclazz, "playSamples", "([S)V");
    jsampleBuffer = (*env)->NewShortArray(env, outBufSamples);
    jsampleBuffer = (*env)->NewGlobalRef(env, jsampleBuffer);

    if (jplaySamples > 0 && jsampleBuffer > 0) {
        __android_log_print(ANDROID_LOG_INFO, "AUDIO", "startAudio ( %d ) OK", minBufferSize);
        return 0;
    }
    return -1;
}

void playSamples(SLmillibel buffer[]) {
    JNIEnv* env;
    (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
    (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    (*env)->SetShortArrayRegion(env, jsampleBuffer, 0, outBufSamples, buffer);
    (*env)->CallStaticVoidMethod(env, jclazz, jplaySamples, jsampleBuffer);
    // detach thread necessary?
}

void shutdownAudio() {
    // TODO: release JNI global references
}
