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
	"sync"
)

type Video struct {
	prog          uint
	texture       uint
	fpsmanager    *gfx.FPSmanager
	width, height int
	textureUni    int
	pixelBuffer   chan []int16
	mutex         sync.Mutex
}

var video Video

const vertShaderSrcDef = `
	attribute vec4 vPosition;
	attribute vec2 vTexCoord;
	varying vec2 texCoord;

	void main() {
        texCoord = vec2(vTexCoord.x * .9375 +.03125, -(vTexCoord.y * .875) -.09375);
		gl_Position = vec4((vPosition.xy * 2.0) - 1.0, vPosition.zw);
	}
`

const fragShaderSrcDef = `
	precision mediump float;
	varying vec2 texCoord;
	uniform sampler2D texture;
	uniform ivec3 palette[64];

	void main() {
		vec4 t = texture2D(texture, texCoord);
		int i = int(t.b * 15.0) * 16 + int(t.a * 15.0);
		i = i - ((i / 64) * 64);

		vec3 color = vec3(palette[i]) / 256.0;

		gl_FragColor = vec4(color, 1);
	}
`

func (video *Video) initGL() {
	video.fpsmanager = gfx.NewFramerate()
	video.fpsmanager.SetFramerate(60)

	gl.ClearColor(0.0, 0.0, 0.0, 1.0)
	gl.Enable(gl.CULL_FACE)
	gl.Enable(gl.DEPTH_TEST)

	video.prog = createProgram(vertShaderSrcDef, fragShaderSrcDef)
	posAttrib := attribLocation(video.prog, "vPosition")
	texCoordAttr := attribLocation(video.prog, "vTexCoord")
	paletteLoc := uniformLocation(video.prog, "palette")
	video.textureUni = uniformLocation(video.prog, "texture")

	video.texture = GenTexture()
	gl.ActiveTexture(gl.TEXTURE0)
	gl.BindTexture(gl.TEXTURE_2D, video.texture)

	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST)
	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST)

	gl.UseProgram(video.prog)
	gl.EnableVertexAttribArray(posAttrib)
	gl.EnableVertexAttribArray(texCoordAttr)

	gl.Uniform3iv(paletteLoc, len(nes.ShaderPalette), nes.ShaderPalette)

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

	if video.pixelBuffer != nil {
		gl.TexImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 256, 256, 0,
			gl.RGBA, gl.UNSIGNED_SHORT_4_4_4_4, gl.Void(&(<-video.pixelBuffer)[0]))
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
