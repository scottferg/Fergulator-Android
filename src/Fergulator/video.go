package main

/*
#include <stdlib.h>
#include <jni.h>
#include <android/input.h>
#include <GLES/gl.h>
#cgo android LDFLAGS: -lGLESv1_CM
*/
import "C"

import (
	"github.com/scottferg/Go-SDL/gfx"
	"log"
	"unsafe"
)

type Video struct {
	texture       C.GLuint
	fpsmanager    *gfx.FPSmanager
	width, height int
	pixelBuffer   chan []uint32
}

var video Video

func (video *Video) initGL() {
	video.fpsmanager = gfx.NewFramerate()
	video.fpsmanager.SetFramerate(60)

	C.glGenTextures(1, &video.texture)
}

func (video *Video) resize(width, height int) {
	video.width = width
	video.height = height

	C.glViewport(C.GLint(0), C.GLint(0), C.GLsizei(width), C.GLsizei(height))
	checkGLError()
	C.glEnable(C.GL_TEXTURE_2D)
	checkGLError()
	C.glDisable(C.GL_CULL_FACE)
	checkGLError()
}

func (video *Video) drawFrame() {

	C.glClear(C.GL_COLOR_BUFFER_BIT | C.GL_DEPTH_BUFFER_BIT)

	C.glEnableClientState(C.GL_VERTEX_ARRAY)
	C.glEnableClientState(C.GL_TEXTURE_COORD_ARRAY)

	C.glBindTexture(C.GL_TEXTURE_2D, video.texture)

	if video.pixelBuffer != nil {
		if bmp := <-video.pixelBuffer; bmp != nil {
			C.glTexImage2D(C.GL_TEXTURE_2D, 0, 3, 240, 224, 0, C.GL_RGBA,
				C.GL_UNSIGNED_SHORT_4_4_4_4, unsafe.Pointer(&bmp[0]))
		}
	}

	box := []float32{-1.0, -1.0, 0.0, 1.0, -1.0, 0.0, 1.0, 1.0, 0.0, -1.0, 1.0, 0.0}
	tex := []float32{0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0}

	C.glVertexPointer(3, C.GL_FLOAT, 0, unsafe.Pointer(&box[0]))
	C.glTexCoordPointer(2, C.GL_FLOAT, 0, unsafe.Pointer(&tex[0]))

	C.glDrawArrays(C.GL_TRIANGLE_FAN, 0, 4)

	C.glDisableClientState(C.GL_VERTEX_ARRAY)
	C.glDisableClientState(C.GL_TEXTURE_COORD_ARRAY)

	video.fpsmanager.FramerateDelay()
}

func checkGLError() {
	if glErr := C.glGetError(); glErr != C.GL_NO_ERROR {
		log.Fatalf("gl. error: %v", glErr)
	}
}

//export Java_com_ferg_afergulator_Engine_drawFrame
func Java_com_ferg_afergulator_Engine_drawFrame(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: drawFrame: %v\n", err)
		}
	}()
	video.drawFrame()
}

//export Java_com_ferg_afergulator_Engine_init
func Java_com_ferg_afergulator_Engine_init(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: init: %v\n", err)
		}
	}()
	video.initGL()
}

//export Java_com_ferg_afergulator_Engine_resize
func Java_com_ferg_afergulator_Engine_resize(env *C.JNIEnv, clazz C.jclass, width, height C.jint) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: resize: %v\n", err)
		}
	}()
	video.resize(int(width), int(height))
}
