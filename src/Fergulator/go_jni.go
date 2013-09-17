package main

/*
#include <jni.h>
#include "go_jni.h"
 */
import "C"

import (
	"unsafe"
)

func GetJavaString(env *C.JNIEnv, jstring C.jstring) string {
	charPtr := C.getCharPtr(env, jstring)
	return C.GoString(charPtr)
}

func GetJavaByteArray(env *C.JNIEnv, jbyteArray C.jbyteArray) []byte {
	bytePtr := C.getByteArrayPtr(env, jbyteArray)
	arrayLen := C.getByteArrayLen(env, jbyteArray)
	return C.GoBytes(unsafe.Pointer(bytePtr), C.int(arrayLen))
}
