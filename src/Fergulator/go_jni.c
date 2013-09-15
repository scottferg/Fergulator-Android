#include "go_jni.h"

jbyte* getByteArrayPtr(JNIEnv* env, jbyteArray array) {
    return (*env)->GetByteArrayElements(env, array, JNI_FALSE);
}

jsize getByteArrayLen(JNIEnv* env, jbyteArray array) {
    return (*env)->GetArrayLength(env, array);
}

char* getCharPtr(JNIEnv* env, jstring string) {
    return (char*) (*env)->GetStringUTFChars(env, string, 0);
}
