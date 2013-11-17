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
	"github.com/scottferg/Fergulator/nes"
)

var SampleSize = 2048

type Audio struct {
	samples     []C.SLmillibel
	sampleIndex int
	mutex       sync.Mutex
	output      chan []C.SLmillibel
}

func NewAudio() *Audio {
	r := Audio{
		samples: make([]C.SLmillibel, SampleSize),
		output:  make(chan []C.SLmillibel),
	}

	go func(c chan []C.SLmillibel) {
		for {
			samples := <-c
			C.playSamples(&samples[0])
		}
	}(r.output)

	return &r
}

func (a *Audio) AppendSample(s int16) {
	a.samples[a.sampleIndex] = C.SLmillibel(s)
	a.sampleIndex++

	if a.sampleIndex == SampleSize {
		a.mutex.Lock()
		a.output <- a.samples
		a.sampleIndex = 0
		a.mutex.Unlock()
	}
}

func (a *Audio) Close() {
	C.shutdownAudio()
}

//export Java_com_ferg_afergulator_Engine_initAudio
func Java_com_ferg_afergulator_Engine_initAudio(env *C.JNIEnv, clazz C.jclass, minBufferSize C.jint) {
	SampleSize = int(minBufferSize)
	r := C.startAudio(env, clazz, C.int(SampleSize))
	nes.AudioEnabled = r == 0
	if r != 0 {
		log.Println("AUDIO NOT ENABLED")
	}
}
