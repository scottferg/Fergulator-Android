#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include <android/log.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

SLresult startAudio();
SLVolumeItf getVolume();
void playSamples(SLmillibel []);
void shutdownAudio();

SLresult createAudioEngine();
SLresult createBufferQueueAudioPlayer();
SLAndroidSimpleBufferQueueItf* getAudioQueue();
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

//----------------------------------------------------------------------
// thread Locks
// to ensure synchronisation between callbacks and processing code
void* createThreadLock(void);
int waitThreadLock(void *lock);
void notifyThreadLock(void *lock);
void destroyThreadLock(void *lock);