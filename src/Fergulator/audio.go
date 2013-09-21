package main

/*
#include <jni.h>
#include "audio_android.h"
#cgo android LDFLAGS: -lOpenSLES
*/
import "C"

import (
	"C"
	"log"
	"sync"
)

var SampleSize = 2048

type Audio struct {
	samples     []C.SLmillibel
	sampleIndex int
	mutex       sync.Mutex
}

func NewAudio() *Audio {
	return &Audio {
		samples: make([]C.SLmillibel, SampleSize),
	}
}

func (a *Audio) AppendSample(s int16) {
	a.samples[a.sampleIndex] = C.SLmillibel(s)
	a.sampleIndex++

	if a.sampleIndex == SampleSize {
		C.playSamples(&a.samples[0])
		a.sampleIndex = 0
	}
}

func (a *Audio) Close() {
	C.shutdownAudio()
}

//export Java_com_vokal_afergulator_Engine_createAudioEngine
func Java_com_vokal_afergulator_Engine_createAudioEngine() {

	result := C.startAudio()
	if C.SL_RESULT_SUCCESS != result {
		log.Printf("Audio Start Error: %v\n", result)
	}

	log.Printf("Audio Start: OK\n", result)
	log.Printf("Audio Volume: %v", C.getVolume())
}
