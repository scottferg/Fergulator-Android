#include <stdlib.h>
#include <jni.h>

extern jbyte* getByteArrayPtr(JNIEnv* env, jbyteArray array);
extern jsize getByteArrayLen(JNIEnv* env, jbyteArray array);
extern char* getCharPtr(JNIEnv* env, jstring string);