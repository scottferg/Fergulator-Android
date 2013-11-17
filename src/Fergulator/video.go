package main

/*
#include <stdlib.h>
#include <jni.h>
#include <android/input.h>
*/
import "C"

import (
	"github.com/scottferg/Fergulator/nes"
	"github.com/scottferg/Go-SDL/gfx"
	gl "github.com/scottferg/egles/es2"
	"log"
)

type Video struct {
	prog          uint
	texture       uint
	fpsmanager    *gfx.FPSmanager
	width, height int
	textureUni    int
	pixelBuffer   chan []uint32
	blank         [240 * 224]uint32
}

var video Video

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

func (video *Video) initGL() {
	log.Printf("GL_VERSION: %v GL_RENDERER: %v GL_VENDOR %v\n",
		gl.GetString(gl.VERSION), gl.GetString(gl.RENDERER), gl.GetString(gl.VENDOR))
	log.Printf("GL_EXTENSIONS: %v\n", gl.GetString(gl.EXTENSIONS))

	video.fpsmanager = gfx.NewFramerate()
	video.fpsmanager.SetFramerate(60)

	gl.ClearColor(0.0, 0.0, 0.0, 1.0)
	gl.Enable(gl.CULL_FACE)
	gl.Enable(gl.DEPTH_TEST)

	video.prog = createProgram(vertShaderSrcDef, fragShaderSrcDef)
	posAttrib := attribLocation(video.prog, "vPosition")
	texCoordAttr := attribLocation(video.prog, "vTexCoord")
	video.textureUni = uniformLocation(video.prog, "texture")

	video.texture = GenTexture()
	gl.ActiveTexture(gl.TEXTURE0)
	gl.BindTexture(gl.TEXTURE_2D, video.texture)

	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST)
	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST)

	gl.UseProgram(video.prog)
	gl.EnableVertexAttribArray(posAttrib)
	gl.EnableVertexAttribArray(texCoordAttr)

	vertVBO := GenBuffer()
	checkGLError()
	gl.BindBuffer(gl.ARRAY_BUFFER, vertVBO)
	verts := []float32{-1.0, 1.0, -1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0}
	gl.BufferData(gl.ARRAY_BUFFER, len(verts)*4, gl.Void(&verts[0]), gl.STATIC_DRAW)

	textCoorBuf := GenBuffer()
	checkGLError()
	gl.BindBuffer(gl.ARRAY_BUFFER, textCoorBuf)
	texVerts := []float32{0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0}
	gl.BufferData(gl.ARRAY_BUFFER, len(texVerts)*4, gl.Void(&texVerts[0]), gl.STATIC_DRAW)

	gl.VertexAttribPointer(posAttrib, 2, gl.FLOAT, false, 0, 0)
	gl.VertexAttribPointer(texCoordAttr, 2, gl.FLOAT, false, 0, 0)

}

func (video *Video) resize(width, height int) {
	video.width = width
	video.height = height

	gl.Viewport(0, 0, width, height)
}

func (video *Video) drawFrame() {
	gl.Clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	gl.UseProgram(video.prog)
	gl.ActiveTexture(gl.TEXTURE0)
	gl.BindTexture(gl.TEXTURE_2D, video.texture)

	if nes.Running && video.pixelBuffer != nil {
		if bmp := <-video.pixelBuffer; bmp != nil {
			gl.TexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 240, 224, 0,
				gl.RGBA, gl.UNSIGNED_BYTE, gl.Void(&bmp[0]))
		}
	} else {
		gl.TexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 240, 224, 0,
			gl.RGBA, gl.UNSIGNED_BYTE, gl.Void(&video.blank[0]))
	}

	gl.DrawArrays(gl.TRIANGLES, 0, 6)
	video.fpsmanager.FramerateDelay()
}

func createProgram(vertShaderSrc string, fragShaderSrc string) uint {
	vertShader := loadShader(gl.VERTEX_SHADER, vertShaderSrc)
	fragShader := loadShader(gl.FRAGMENT_SHADER, fragShaderSrc)

	prog := gl.CreateProgram()
	if prog == 0 {
		panic("Failed to create shader program")
	}

	gl.AttachShader(prog, vertShader)
	gl.AttachShader(prog, fragShader)
	gl.LinkProgram(prog)

	gl.GetProgramiv(prog, gl.LINK_STATUS, make([]int32, 1))

	return prog
}

func loadShader(shaderType uint, source string) uint {
	handle := gl.CreateShader(shaderType)
	if handle == 0 {
		log.Fatalf("Failed to create shader of type %v", shaderType)
	}

	gl.ShaderSource(handle, source)
	gl.CompileShader(handle)

	gl.GetShaderiv(handle, gl.COMPILE_STATUS, make([]int32, 1))

	return handle
}

func attribLocation(prog uint, name string) uint {
	attrib := gl.GetAttribLocation(prog, name)
	checkGLError()

	if attrib == -1 {
		log.Fatalf("Failed to find attrib position for %v", name)
	}

	return uint(attrib)
}

func uniformLocation(prog uint, name string) int {
	attrib := gl.GetUniformLocation(prog, name)
	checkGLError()

	if attrib == -1 {
		log.Fatalf("Failed to find attrib position for %v", name)
	}

	return attrib
}

func GenBuffer() uint {
	var buf uint
	gl.GenBuffers(1, gl.Void(&buf))
	return buf
}

func GenTexture() uint {
	var tex uint
	gl.GenBuffers(1, gl.Void(&tex))
	return tex
}

func checkGLError() {
	if glErr := gl.GetError(); glErr != gl.NO_ERROR {
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
