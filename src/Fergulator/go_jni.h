#include <stdlib.h>
#include <jni.h>

extern jbyte* gimmeMahBytes(JNIEnv* env, jbyteArray array);
extern jsize howLongIsIt(JNIEnv* env, jbyteArray array);