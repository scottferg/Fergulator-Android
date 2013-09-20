#include <assert.h>
#include <stdlib.h>
#include <string.h>

#include <android/log.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

SLresult startAudio();
SLVolumeItf getVolume();
void playSamples(signed short);
void shutdownAudio();

SLresult createAudioEngine();
SLresult createBufferQueueAudioPlayer();
SLAndroidSimpleBufferQueueItf* getAudioQueue();
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);
