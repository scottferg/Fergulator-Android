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
	"io/ioutil"
	"github.com/scottferg/Fergulator/nes"
)

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())
}

type gamerom struct {
	bytes		[]byte
	name		string
	filePath	string
}

var rom gamerom

func RunEmulator() {

	nes.AudioEnabled = false

	videoTick, _, err := nes.Init(rom.bytes, func(i int16) {}, GetKey)
	if err != nil {
		log.Println(err)
	}

	gfx.pixelBuffer = videoTick

	if len(rom.filePath) > 0 {
		savePath := fmt.Sprintf("%s/.%s.state", rom.filePath, rom.name)
		log.Printf("save file: %v\n", savePath)
		if err := ioutil.WriteFile(savePath, rom.bytes[:3], 0644); err != nil {
			log.Printf("WRITE ERROR: %v\n", err.Error())
		}
		state, err := ioutil.ReadFile(savePath)
		if err != nil {
			log.Printf("READ ERROR: %v\n", err.Error())
			return
		}
		log.Printf("save read: %v\n", string(state))
	} else {
		log.Println("FILE PATH NOT SET!")
	}

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

	bytePtr := C.getByteArrayPtr(env, jbytes)
	arrayLen := C.getByteArrayLen(env, jbytes)
	rom.bytes = C.GoBytes(unsafe.Pointer(bytePtr), C.int(arrayLen))

	charPtr := C.getCharPtr(env, name)
	rom.name = C.GoString(charPtr)

	log.Printf("ROM NAME: %v\n", rom.name)
	log.Printf("ROM SIZE: %v\n", len(rom.bytes))
	log.Printf("ROM TYPE: %v\n", string(rom.bytes[:3]))

	RunEmulator()
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

//export Java_com_vokal_afergulator_Engine_setRunning
func Java_com_vokal_afergulator_Engine_setRunning(env *C.JNIEnv, clazz C.jclass, running C.jboolean) {
	log.Printf("SET RUNNING: %v\n", running)
	// TODO functions on gamestate for pausing emulator
}

//export Java_com_vokal_afergulator_Engine_setFilePath
func Java_com_vokal_afergulator_Engine_setFilePath(env *C.JNIEnv, clazz C.jclass, path C.jstring) {
	if (path != nil) {
		rom.filePath = C.GoString(C.getCharPtr(env, path))
		log.Printf("FILE PATH: %v\n", rom.filePath)
	}
}



