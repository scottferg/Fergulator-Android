#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include <android/log.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

typedef struct _circular_buffer {
  SLmillibel *buffer;
  int  wp;
  int rp;
  int size;
} circular_buffer;

typedef struct threadLock_{
  pthread_mutex_t m;
  pthread_cond_t  c;
  unsigned char   s;
} threadLock;

// 8 kHz mono 16-bit signed little endian
static const char android[] =
#include "android_clip.h"
;

SLresult startAudio(int);
void playSamples(SLmillibel []);
void shutdownAudio();

SLmillibel getVolume();
SLresult setVolume(SLmillibel);
SLmillibel getMaxVolume();

SLresult createAudioEngine();
SLresult createBufferQueueAudioPlayer();
SLAndroidSimpleBufferQueueItf* getAudioQueue();
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

circular_buffer* create_circular_buffer(int size);
int read_circular_buffer_bytes(circular_buffer *p, SLmillibel *out, int count);
int write_circular_buffer_bytes(circular_buffer *p, const SLmillibel *in, int count);

//----------------------------------------------------------------------
// thread Locks
// to ensure synchronisation between callbacks and processing code
void* createThreadLock(void);
int waitThreadLock(void *lock);
void notifyThreadLock(void *lock);
void destroyThreadLock(void *lock);
