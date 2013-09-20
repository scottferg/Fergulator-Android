package main

/*
#include <jni.h>
*/
import "C"

import (
	"log"
	"fmt"
	"runtime"
	"github.com/scottferg/Fergulator/nes"
)

var (
	cachePath	string
	audioOut    *Audio
)

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())
	nes.AudioEnabled = true
}

//export Java_com_vokal_afergulator_Engine_setFilePath
func Java_com_vokal_afergulator_Engine_setFilePath(env *C.JNIEnv, clazz C.jclass, path C.jstring) {
	if (path != nil) {
		cachePath = GetJavaString(env, path)
		log.Printf("FILE PATH: %v\n", cachePath)
	}
}

//export Java_com_vokal_afergulator_Engine_loadRom
func Java_com_vokal_afergulator_Engine_loadRom(env *C.JNIEnv, clazz C.jclass, jbytes C.jbyteArray, name C.jstring) C.jboolean {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: loadRom: %v\n", err)
		}
	}()

	if (len(nes.GameName) > 0) {
		return C.JNI_FALSE
	}

	nes.GameName = GetJavaString(env, name)
	rom := GetJavaByteArray(env, jbytes)

	if len(cachePath) > 0 {
		nes.SaveStateFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)
		nes.BatteryRamFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)
	}

	log.Printf("%v ROM: %v (%v kb)\n", string(rom[:3]), nes.GameName, len(rom) / 1024)

	audioOut = NewAudio()
//	defer audioOut.Close()

	videoTick, err := nes.Init(rom, audioOut.AppendSample, GetKey)
	if err != nil {
		log.Println(err)
		return C.JNI_FALSE
	}

	gfx.pixelBuffer = videoTick

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go nes.RunSystem()

	return C.JNI_TRUE
}

//export Java_com_vokal_afergulator_Engine_pauseEmulator
func Java_com_vokal_afergulator_Engine_pauseEmulator(env *C.JNIEnv, clazz C.jclass, pause C.int) {
	if len(nes.GameName) > 0 {
		if (pause == -1) {
			// TODO: safe pause emulator
		} else if (pause == 1) {
			log.Printf("saving: %v\n", nes.SaveStateFile)
			nes.SaveGameState()
		} else {
			log.Printf("loading: %v\n", nes.SaveStateFile)
			nes.LoadGameState()
		}
	}
}

//export Java_com_vokal_afergulator_Engine_keyEvent
func Java_com_vokal_afergulator_Engine_keyEvent(env *C.JNIEnv, clazz C.jclass, key C.jint, event C.jint, player C.jint) {
//	log.Printf("key [%v] %v\n", key, event)
	p := int(player)
	if nes.Pads[player] != nil {
		if event == 1 {
			nes.Pads[player].KeyDown(int(key), p)
		} else {
			nes.Pads[player].KeyUp(int(key), p)
		}
	}
}

func GetKey(ev interface{}) int {
	if k, ok := ev.(int); ok {
		return k
	}

	return -1
}


