#include "audio_android.h"


// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;
static SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLAndroidSimpleBufferQueueState bpPlayerBufferQueueState;
static SLEffectSendItf bqPlayerEffectSend;
static SLVolumeItf bqPlayerVolume;

// aux effect on the output mix, used by the buffer queue player
static const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

// buffers
short *outputBuffer;
short *playBuffer;
circular_buffer *outrb;
int outBufSamples = 2048;

int numQ = 0;

// send SLmillibel[outBufSamples] to the circular buffer queue
void playSamples(SLmillibel buffer[]) {
    if (0 == numQ++) {
       (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, buffer, outBufSamples);
    } else {
        write_circular_buffer_bytes(outrb, (char *) buffer, outBufSamples);
    }
}

// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    numQ %= 4;
    if (--numQ > 0) {
        read_circular_buffer_bytes(outrb, (char *) playBuffer, outBufSamples);
        (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, playBuffer, outBufSamples);
    }
}

SLresult startAudio() {
    SLresult result = createAudioEngine();

    if (SL_RESULT_SUCCESS == result)
        result = createBufferQueueAudioPlayer();

//    if (result == 0) playTest();  // use SL_SAMPLINGRATE_8

    outrb = create_circular_buffer(outBufSamples * sizeof(short)*4);
    outputBuffer = (short *) calloc(outBufSamples, sizeof(short));
    playBuffer = (short *) calloc(outBufSamples, sizeof(short));

    return result;
}

void playTest() {
    __android_log_print(ANDROID_LOG_INFO, "AUDIO", "Play Test......");
    (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, (short *) android, sizeof(android));
//    playSamples((short *) android);
}

static SLDataFormat_PCM format_pcm = {
    SL_DATAFORMAT_PCM,
    1,
    SL_SAMPLINGRATE_44_1,
    SL_PCMSAMPLEFORMAT_FIXED_16,
    SL_PCMSAMPLEFORMAT_FIXED_16,
    SL_SPEAKER_FRONT_CENTER,
    SL_BYTEORDER_LITTLEENDIAN
};

// create the engine and output mix objects
SLresult createAudioEngine() {
    SLresult result;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the engine interface, which is needed in order to create other objects
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // create output mix, with environmental reverb specified as a non-required interface
    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the environmental reverb interface
    // this could fail if the environmental reverb effect is not available,
    // either because the feature is not present, excessive CPU load, or
    // the required MODIFY_AUDIO_SETTINGS permission was not requested and granted
    SLresult resultReverb = (*outputMixObject)->GetInterface(outputMixObject,
            SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == resultReverb) {
        resultReverb = (*outputMixEnvironmentalReverb)->
            SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &reverbSettings);
        (void)resultReverb;
    }
    // ignore unsuccessful result codes for environmental reverb, as it is optional for this example

    return result;
}

// create buffer queue audio player
SLresult createBufferQueueAudioPlayer() {
    SLresult result;

    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    // create audio player
    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject,
                                                &audioSrc, &audioSnk, 3, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the player
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the play interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the buffer queue interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // register callback on the buffer queue
    result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the effect send interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_EFFECTSEND, &bqPlayerEffectSend);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the volume interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME, &bqPlayerVolume);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    return result;
}

SLVolumeItf getVolume()
{
   (*bqPlayerVolume)->SetVolumeLevel(bqPlayerVolume, (SLmillibel) 0x7FFF);
   return bqPlayerVolume;
}

SLAndroidSimpleBufferQueueItf* getAudioQueue()
{
    return &bqPlayerBufferQueue;
}

void shutdownAudio() {

    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (bqPlayerObject != NULL) {
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = NULL;
        bqPlayerPlay = NULL;
        bqPlayerBufferQueue = NULL;
        bqPlayerEffectSend = NULL;
        bqPlayerVolume = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        outputMixEnvironmentalReverb = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }

    // free_circular_buffer
    if (outrb != NULL) {
        free(outrb->buffer);
        free(outrb);
    }
}

circular_buffer* create_circular_buffer(int bytes){
  circular_buffer *p;
  if ((p = calloc(1, sizeof(circular_buffer))) == NULL) {
    return NULL;
  }
  p->size = bytes;
  p->wp = p->rp = 0;

  if ((p->buffer = calloc(bytes, sizeof(char))) == NULL) {
    free (p);
    return NULL;
  }
  return p;
}

int checkspace_circular_buffer(circular_buffer *p, int writeCheck){
  int wp = p->wp, rp = p->rp, size = p->size;
  if(writeCheck){
    if (wp > rp) return rp - wp + size - 1;
    else if (wp < rp) return rp - wp - 1;
    else return size - 1;
  }
  else {
    if (wp > rp) return wp - rp;
    else if (wp < rp) return wp - rp + size;
    else return 0;
  }
}

int read_circular_buffer_bytes(circular_buffer *p, char *out, int bytes){
  int remaining;
  int bytesread, size = p->size;
  int i=0, rp = p->rp;
  char *buffer = p->buffer;
  if ((remaining = checkspace_circular_buffer(p, 0)) == 0) {
    return 0;
  }
  bytesread = bytes > remaining ? remaining : bytes;
  for(i=0; i < bytesread; i++){
    out[i] = buffer[rp++];
    if(rp == size) rp = 0;
  }
  p->rp = rp;
  return bytesread;
}

int write_circular_buffer_bytes(circular_buffer *p, const char *in, int bytes){
  int remaining;
  int byteswrite, size = p->size;
  int i=0, wp = p->wp;
  char *buffer = p->buffer;
  if ((remaining = checkspace_circular_buffer(p, 1)) == 0) {
    return 0;
  }
  byteswrite = bytes > remaining ? remaining : bytes;
  for(i=0; i < byteswrite; i++){
    buffer[wp++] = in[i];
    if(wp == size) wp = 0;
  }
  p->wp = wp;
  return byteswrite;
}


