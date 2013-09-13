#include "go_jni.h"

jbyte* gimmeMahBytes(JNIEnv* env, jbyteArray array) {
    return (*env)->GetByteArrayElements(env, array, JNI_FALSE);
}

jsize howLongIsIt(JNIEnv* env, jbyteArray array) {
    return (*env)->GetArrayLength(env, array);
}