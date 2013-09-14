package main

/*
#include <jni.h>
#include "go_jni.h"
*/
import "C"

import (
	"log"
	"fmt"
	"runtime"
	"unsafe"
	"github.com/scottferg/Fergulator/nes"
)

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())
}

type gamerom struct {
	bytes		[]byte
	name		string
	savePath	string
	battPath	string
}

func RunEmulator(rom gamerom) {

	nes.AudioEnabled = false

	videoTick, _, err := nes.Init(rom.bytes, func(i int16) {}, GetKey)
	if err != nil {
		log.Println(err)
	}

	gfx.pixelBuffer = videoTick

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go nes.RunSystem()
}

func GetKey(ev interface{}) int {
	if k, ok := ev.(int); ok {
		return k
	}

	return -1
}

//export Java_com_vokal_afergulator_Engine_loadRom
func Java_com_vokal_afergulator_Engine_loadRom(env *C.JNIEnv, clazz C.jclass, jbytes C.jbyteArray, name C.jstring) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: loadRom: %v\n", err)
		}
	}()
	log.Println("android.go::loadRom()...")

	var rom gamerom

	bytePtr := C.getByteArrayPtr(env, jbytes)
	arrayLen := C.getByteArrayLen(env, jbytes)
	rom.bytes = C.GoBytes(unsafe.Pointer(bytePtr), C.int(arrayLen))

	charPtr := C.getCharPtr(env, name)
	rom.name = C.GoString(charPtr)
	rom.savePath = fmt.Sprintf(".%s.state", rom.name)
	rom.battPath = fmt.Sprintf(".%s.battery", rom.name)

	log.Printf("ROM NAME: %v\n", rom.name)
	log.Printf("ROM SIZE: %v\n", len(rom.bytes))
	log.Printf("ROM TYPE: %v\n", string(rom.bytes[:3]))

	RunEmulator(rom)
}


//export Java_com_vokal_afergulator_Engine_keyEvent
func Java_com_vokal_afergulator_Engine_keyEvent(env *C.JNIEnv, clazz C.jclass, key C.jint, event C.jint, player C.jint) {
//	log.Printf("key [%v] %v\n", key, event)
	if nes.Pads[int(player)] != nil {
		if event == 1 {
			nes.Pads[0].KeyDown(int(key), int(player))
		} else {
			nes.Pads[0].KeyUp(int(key), int(player))
		}
	}
}


