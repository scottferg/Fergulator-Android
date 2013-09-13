package main

/*
#include <stdlib.h>
#include <jni.h>
#include <android/input.h>
#include <GLES2/gl2.h>
#include "go_jni.h"
#cgo android LDFLAGS: -lGLESv2

*/
import "C"

import (
	"fmt"
	"github.com/scottferg/Fergulator/nes"
	"log"
	"math"
	"runtime"
	"sync"
	"unsafe"
)

var time float64

type gamerom struct {
	name  string
	bytes []byte
}

type game struct {
	prog                C.GLuint
	texture             C.GLuint
	width, height       int
	offsetUni, colorUni int
	textureUni          int

	mu               sync.Mutex // Protects offsetX, offsetY
	offsetX, offsetY float32

	touching       bool
	touchX, touchY float32
}

var g game
var videoStream chan []uint32

const vertShaderSrcDef = `
	attribute vec4 vPosition;
	attribute vec2 vTexCoord;
	varying vec2 texCoord;

	void main() {
		texCoord = vec2(vTexCoord.x, -vTexCoord.y);
		gl_Position = vec4((vPosition.xy * 2.0) - 1.0, vPosition.zw);
	}
`

const fragShaderSrcDef = `
	precision mediump float;
	varying vec2 texCoord;
	uniform sampler2D texture;

	void main() {
		vec4 c = texture2D(texture, texCoord);
		gl_FragColor = vec4(c.b, c.g, c.r, c.a);
	}
`

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())
}

func GetShaderInfoLog(shader C.GLuint) string {
	var logLen C.GLint
	C.glGetShaderiv(shader, C.GL_INFO_LOG_LENGTH, &logLen)
	var c C.GLchar
	logLenBytes := int(logLen) * int(unsafe.Sizeof(c))
	log := C.malloc(C.size_t(logLenBytes))
	if log == nil {
		panic("Failed to allocate shader log buffer")
	}
	defer C.free(log)
	C.glGetShaderInfoLog(C.GLuint(shader), C.GLsizei(logLen), (*C.GLsizei)(unsafe.Pointer(nil)), (*C.GLchar)(log))
	return string(C.GoBytes(log, C.int(logLenBytes)))
}

func GetProgramInfoLog(program C.GLuint) string {
	var logLen C.GLint
	C.glGetProgramiv(program, C.GL_INFO_LOG_LENGTH, &logLen)
	var c C.GLchar
	logLenBytes := int(logLen) * int(unsafe.Sizeof(c))
	log := C.malloc(C.size_t(logLenBytes))
	if log == nil {
		panic("Failed to allocate shader log buffer")
	}
	defer C.free(log)
	C.glGetProgramInfoLog(C.GLuint(program), C.GLsizei(logLen), (*C.GLsizei)(unsafe.Pointer(nil)), (*C.GLchar)(log))
	return string(C.GoBytes(log, C.int(logLenBytes)))
}

func loadShader(shaderType C.GLenum, source string) C.GLuint {
	handle := C.glCreateShader(shaderType)
	if handle == 0 {
		panic(fmt.Errorf("Failed to create shader of type %v", shaderType))
	}
	sourceC := C.CString(source)
	defer C.free(unsafe.Pointer(sourceC))
	C.glShaderSource(handle, 1, (**C.GLchar)(unsafe.Pointer(&sourceC)), (*C.GLint)(unsafe.Pointer(nil)))
	C.glCompileShader(handle)
	var compiled C.GLint
	C.glGetShaderiv(handle, C.GL_COMPILE_STATUS, &compiled)
	if compiled != C.GL_TRUE {
		log := GetShaderInfoLog(handle)
		panic(fmt.Errorf("Failed to compile shader: %v, shader: %v", log, source))
	}
	return handle
}

func GenBuffer() C.GLuint {
	var buf C.GLuint
	C.glGenBuffers(1, &buf)
	return C.GLuint(buf)
}

func GenTexture() C.GLuint {
	var tex C.GLuint
	C.glGenTextures(1, &tex)
	return C.GLuint(tex)
}

func checkGLError() {
	if glErr := C.glGetError(); glErr != C.GL_NO_ERROR {
		panic(fmt.Errorf("C.gl error: %v", glErr))
	}
}

func createProgram(vertShaderSrc string, fragShaderSrc string) C.GLuint {
	vertShader := loadShader(C.GL_VERTEX_SHADER, vertShaderSrc)
	fragShader := loadShader(C.GL_FRAGMENT_SHADER, fragShaderSrc)
	prog := C.glCreateProgram()
	if prog == 0 {
		panic("Failed to create shader program")
	}
	C.glAttachShader(prog, vertShader)
	checkGLError()
	C.glAttachShader(prog, fragShader)
	checkGLError()
	C.glLinkProgram(prog)
	var linkStatus C.GLint
	C.glGetProgramiv(prog, C.GL_LINK_STATUS, &linkStatus)
	if linkStatus != C.GL_TRUE {
		log := GetProgramInfoLog(prog)
		panic(fmt.Errorf("Failed to link program: %v", log))
	}
	return prog
}

func attribLocation(prog C.GLuint, name string) int {
	nameC := C.CString(name)
	defer C.free(unsafe.Pointer(nameC))
	attrib := int(C.glGetAttribLocation(C.GLuint(prog), (*C.GLchar)(unsafe.Pointer(nameC))))
	checkGLError()
	if attrib == -1 {
		panic(fmt.Errorf("Failed to find attrib position for %v", name))
	}
	return attrib
}

func uniformLocation(prog C.GLuint, name string) int {
	nameC := C.CString(name)
	defer C.free(unsafe.Pointer(nameC))
	attrib := int(C.glGetUniformLocation(C.GLuint(prog), (*C.GLchar)(unsafe.Pointer(nameC))))
	checkGLError()
	if attrib == -1 {
		panic(fmt.Errorf("Failed to find attrib position for %v", name))
	}
	return attrib
}

func GetString(name C.GLenum) string {
	val := C.glGetString(C.GLenum(name))
	return C.GoString((*C.char)(unsafe.Pointer(val)))
}

func (game *game) resize(width, height int) {
	game.width = width
	game.height = height
	C.glViewport(0, 0, C.GLsizei(width), C.GLsizei(height))
}

func (game *game) initGL() {
	log.Printf("GL_VERSION: %v GL_RENDERER: %v GL_VENDOR %v\n",
		GetString(C.GL_VERSION), GetString(C.GL_RENDERER), GetString(C.GL_VENDOR))
	log.Printf("GL_EXTENSIONS: %v\n", GetString(C.GL_EXTENSIONS))

	C.glClearColor(0.0, 0.0, 0.0, 1.0)
	C.glEnable(C.GL_CULL_FACE)
	C.glEnable(C.GL_DEPTH_TEST)

	game.prog = createProgram(vertShaderSrcDef, fragShaderSrcDef)
	posAttrib := attribLocation(game.prog, "vPosition")
	texCoordAttr := attribLocation(game.prog, "vTexCoord")
	game.textureUni = uniformLocation(game.prog, "texture")

	game.texture = GenTexture()
	C.glActiveTexture(C.GL_TEXTURE0)
	C.glBindTexture(C.GL_TEXTURE_2D, game.texture)

	checkGLError()

	C.glTexParameteri(C.GL_TEXTURE_2D, C.GL_TEXTURE_MIN_FILTER, C.GL_NEAREST)
	C.glTexParameteri(C.GL_TEXTURE_2D, C.GL_TEXTURE_MAG_FILTER, C.GL_NEAREST)

	checkGLError()

	C.glUseProgram(game.prog)
	C.glEnableVertexAttribArray(C.GLuint(posAttrib))
	C.glEnableVertexAttribArray(C.GLuint(texCoordAttr))

	vertVBO := GenBuffer()
	checkGLError()
	C.glBindBuffer(C.GL_ARRAY_BUFFER, vertVBO)
	verts := []float32{-1.0, 1.0, -1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0}
	C.glBufferData(C.GL_ARRAY_BUFFER, C.GLsizeiptr(len(verts)*int(unsafe.Sizeof(verts[0]))), unsafe.Pointer(&verts[0]), C.GL_STATIC_DRAW)

	textCoorBuf := GenBuffer()
	C.glBindBuffer(C.GL_ARRAY_BUFFER, textCoorBuf)
	texVerts := []float32{0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0}
	C.glBufferData(C.GL_ARRAY_BUFFER, C.GLsizeiptr(len(texVerts)*int(unsafe.Sizeof(texVerts[0]))), unsafe.Pointer(&texVerts[0]), C.GL_STATIC_DRAW)

	C.glVertexAttribPointer(C.GLuint(posAttrib), 2, C.GL_FLOAT, C.GL_FALSE, 0, unsafe.Pointer(uintptr(0)))
	C.glVertexAttribPointer(C.GLuint(texCoordAttr), 2, C.GL_FLOAT, C.GL_FALSE, 0, unsafe.Pointer(uintptr(0)))

}

func (game *game) drawFrame() {
	time += .05
	color := (C.GLclampf(math.Sin(time)) + 1) * .5
	fmt.Print(color)

	C.glClear(C.GL_COLOR_BUFFER_BIT | C.GL_DEPTH_BUFFER_BIT)

	C.glUseProgram(game.prog)

	C.glActiveTexture(C.GL_TEXTURE0)
	C.glBindTexture(C.GL_TEXTURE_2D, game.texture)

	if videoStream != nil {
		select {
		case bmp := <-videoStream:
			C.glTexImage2D(C.GL_TEXTURE_2D, 0, C.GL_RGBA, 240, 224, 0, C.GL_RGBA, C.GL_UNSIGNED_BYTE, unsafe.Pointer(&bmp[0]))
		}
	}

	C.glDrawArrays(C.GL_TRIANGLES, 0, 6)
}

func (game *game) onTouch(action int, x, y float32) {
	switch action {
	case C.AMOTION_EVENT_ACTION_UP:
		game.touching = false
		nes.Pads[0].KeyUp(nes.ButtonStart, 0)
		log.Println("<<<START>>>")
	case C.AMOTION_EVENT_ACTION_DOWN:
		game.touching = true
		game.touchX, game.touchY = x, y
		nes.Pads[0].KeyDown(nes.ButtonStart, 0)
		log.Println(">>>START<<<")

	case C.AMOTION_EVENT_ACTION_MOVE:
		if !game.touching {
			break
		}
		game.mu.Lock()
		game.offsetX += 2 * (x - game.touchX) / float32(game.width)
		game.offsetY += 2 * -(y - game.touchY) / float32(game.height)
		game.mu.Unlock()
		game.touchX, game.touchY = x, y
	}
}

// Use JNI_OnLoad to ensure that the go runtime is initialized at a predictable time,
// namely at System.loadLibrary()
//export JNI_OnLoad
func JNI_OnLoad(vm *C.JavaVM, reserved unsafe.Pointer) C.jint {
	return C.JNI_VERSION_1_6
}

//export Java_com_vokal_afergulator_Engine_drawFrame
func Java_com_vokal_afergulator_Engine_drawFrame(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: drawFrame: %v\n", err)
		}
	}()
	g.drawFrame()
}

//export Java_com_vokal_afergulator_Engine_init
func Java_com_vokal_afergulator_Engine_init(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: init: %v\n", err)
		}
	}()
	g.initGL()
}

//export Java_com_vokal_afergulator_Engine_resize
func Java_com_vokal_afergulator_Engine_resize(env *C.JNIEnv, clazz C.jclass, width, height C.jint) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: resize: %v\n", err)
		}
	}()
	g.resize(int(width), int(height))
}

//export Java_com_vokal_afergulator_Engine_onTouch
func Java_com_vokal_afergulator_Engine_onTouch(env *C.JNIEnv, clazz C.jclass, action C.jint, x, y C.jfloat) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: resize: %v\n", err)
		}
	}()
	g.onTouch(int(action), float32(x), float32(y))
}

//export Java_com_vokal_afergulator_Engine_keyEvent
func Java_com_vokal_afergulator_Engine_keyEvent(env *C.JNIEnv, clazz C.jclass, key C.jint, event C.jint, player C.jint) {
	if event == 1 {
		nes.Pads[0].KeyDown(int(key), int(player))
		log.Printf("key down: %v\n", key)
	} else {
		nes.Pads[0].KeyUp(int(key), int(player))
		log.Printf("key up: %v\n", key)
	}
}

//export Java_com_vokal_afergulator_Engine_loadRom
func Java_com_vokal_afergulator_Engine_loadRom(env *C.JNIEnv, clazz C.jclass, bytes C.jbyteArray, length C.jint) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: loadRom: %v\n", err)
		}
	}()
	log.Println("android.go::loadRom()...")

	bytePtr := C.gimmeMahBytes(env, bytes)
	arrayLen := C.howLongIsIt(env, bytes)

	videoStream = RunEmulator(GetRom(bytePtr, arrayLen))
}

func GetRom(bytes *C.jbyte, length C.jsize) gamerom {
	var gr gamerom

	gr.bytes = C.GoBytes(unsafe.Pointer(bytes), C.int(length))
	gr.name = "super_mario"

	log.Printf("LOADED ROM: %v\n", gr.name)
	log.Printf("ROM SIZE: %v\n", len(gr.bytes))

	log.Println(string(gr.bytes[:3]))

	return gr
}
