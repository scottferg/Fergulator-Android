#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include <android/log.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

typedef struct _circular_buffer {
  char *buffer;
  int  wp;
  int rp;
  int size;
} circular_buffer;

// 8 kHz mono 16-bit signed little endian
static const char android[] =
#include "android_clip.h"
;

SLresult startAudio();
SLVolumeItf getVolume();
void playSamples(SLmillibel []);
void shutdownAudio();

SLresult createAudioEngine();
SLresult createBufferQueueAudioPlayer();
SLAndroidSimpleBufferQueueItf* getAudioQueue();
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

circular_buffer* create_circular_buffer(int bytes);
int read_circular_buffer_bytes(circular_buffer *p, char *out, int bytes);
int write_circular_buffer_bytes(circular_buffer *p, const char *in, int bytes);