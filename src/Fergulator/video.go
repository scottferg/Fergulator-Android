package main

/*
#include <stdlib.h>
#include <jni.h>
#include <android/input.h>
#include <GLES2/gl2.h>
#cgo android LDFLAGS: -lGLESv2
*/
import "C"

import (
	"fmt"
	"log"
	"math"
	"unsafe"
)

type Video struct {
	prog          C.GLuint
	texture       C.GLuint
	width, height int
	textureUni    int
	pixelBuffer   chan []uint32
}

var gfx Video

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
		gl_FragColor = vec4(c.a, c.b, c.g, 1.0);
	}
`

func (game *Video) initGL() {
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

	C.glTexParameteri(C.GL_TEXTURE_2D, C.GL_TEXTURE_MIN_FILTER, C.GL_NEAREST)
	C.glTexParameteri(C.GL_TEXTURE_2D, C.GL_TEXTURE_MAG_FILTER, C.GL_NEAREST)

	C.glUseProgram(game.prog)
	C.glEnableVertexAttribArray(C.GLuint(posAttrib))
	C.glEnableVertexAttribArray(C.GLuint(texCoordAttr))

	vertVBO := GenBuffer()
	checkGLError()
	C.glBindBuffer(C.GL_ARRAY_BUFFER, vertVBO)
	verts := []float32{-1.0, 1.0, -1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0}
	C.glBufferData(C.GL_ARRAY_BUFFER, C.GLsizeiptr(len(verts)*int(unsafe.Sizeof(verts[0]))), unsafe.Pointer(&verts[0]), C.GL_STATIC_DRAW)

	textCoorBuf := GenBuffer()
	checkGLError()
	C.glBindBuffer(C.GL_ARRAY_BUFFER, textCoorBuf)
	texVerts := []float32{0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0}
	C.glBufferData(C.GL_ARRAY_BUFFER, C.GLsizeiptr(len(texVerts)*int(unsafe.Sizeof(texVerts[0]))), unsafe.Pointer(&texVerts[0]), C.GL_STATIC_DRAW)

	C.glVertexAttribPointer(C.GLuint(posAttrib), 2, C.GL_FLOAT, C.GL_FALSE, 0, unsafe.Pointer(uintptr(0)))
	C.glVertexAttribPointer(C.GLuint(texCoordAttr), 2, C.GL_FLOAT, C.GL_FALSE, 0, unsafe.Pointer(uintptr(0)))

}

func (game *Video) resize(width, height int) {
	x_offset := 0
	y_offset := 0

	r := ((float64)(height)) / ((float64)(width))

	if r > 0.9375 { // Height taller than ratio
		h := (int)(math.Floor((float64)(0.9375 * (float64)(width))))
		y_offset = (height - h) / 2
		height = h
	} else if r < 0.9375 { // Width wider
		w := (int)(math.Floor((float64)((256.0 / 240.0) * (float64)(height))))
		x_offset = (width - w) / 2
		width = w
	}

	game.width = width
	game.height = height

	C.glViewport(C.GLint(x_offset), C.GLint(y_offset), C.GLsizei(width), C.GLsizei(height))
}

func (game *Video) drawFrame() {
	C.glClear(C.GL_COLOR_BUFFER_BIT | C.GL_DEPTH_BUFFER_BIT)

	C.glUseProgram(game.prog)

	C.glActiveTexture(C.GL_TEXTURE0)
	C.glBindTexture(C.GL_TEXTURE_2D, game.texture)

	if gfx.pixelBuffer != nil {
		if bmp := <-gfx.pixelBuffer; bmp != nil {
			C.glTexImage2D(C.GL_TEXTURE_2D, 0, C.GL_RGBA, 240, 224, 0, C.GL_RGBA, C.GL_UNSIGNED_BYTE, unsafe.Pointer(&bmp[0]))
		}
	}

	C.glDrawArrays(C.GL_TRIANGLES, 0, 6)
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

func GetString(name C.GLenum) string {
	val := C.glGetString(C.GLenum(name))
	return C.GoString((*C.char)(unsafe.Pointer(val)))
}

func checkGLError() {
	if glErr := C.glGetError(); glErr != C.GL_NO_ERROR {
		panic(fmt.Errorf("C.gl error: %v", glErr))
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
	gfx.drawFrame()
}

//export Java_com_vokal_afergulator_Engine_init
func Java_com_vokal_afergulator_Engine_init(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: init: %v\n", err)
		}
	}()
	gfx.initGL()
}

//export Java_com_vokal_afergulator_Engine_resize
func Java_com_vokal_afergulator_Engine_resize(env *C.JNIEnv, clazz C.jclass, width, height C.jint) {
	defer func() {
		if err := recover(); err != nil {
			log.Fatalf("panic: resize: %v\n", err)
		}
	}()
	gfx.resize(int(width), int(height))
}
