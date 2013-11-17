#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>

#include <android/log.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>


int startAudio(JNIEnv*, jclass, int);
void playSamples(SLmillibel []);
void shutdownAudio();
