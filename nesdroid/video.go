package nesdroid

import (
	"encoding/binary"
	"log"
	"sync"

	"github.com/scottferg/Fergulator/nes"
	"github.com/scottferg/Go-SDL/gfx"
	"golang.org/x/mobile/exp/f32"
	"golang.org/x/mobile/gl"
)

type Video struct {
	prog          gl.Program
	texture       gl.Texture
	fpsmanager    *gfx.FPSmanager
	width, height int
	textureUni    gl.Uniform
	pixelBuffer   chan []uint32
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
	precision highp float;
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
	log.Print("Initing")
	video.fpsmanager = gfx.NewFramerate()
	video.fpsmanager.SetFramerate(60)

	gl.ClearColor(0.0, 0.0, 0.0, 1.0)
	gl.Enable(gl.CULL_FACE)
	gl.Enable(gl.DEPTH_TEST)

	log.Print("Creating program")
	video.prog = createProgram(vertShaderSrcDef, fragShaderSrcDef)
	log.Print("Attrib loc 1")
	posAttrib := attribLocation(video.prog, "vPosition")
	log.Print("Attrib loc 2")
	texCoordAttr := attribLocation(video.prog, "vTexCoord")
	log.Print("Uniform loc 1")
	paletteLoc := uniformLocation(video.prog, "palette")
	log.Print("Uniform loc 2")
	video.textureUni = uniformLocation(video.prog, "texture")

	log.Print("Gen Texture")
	video.texture = genTexture()
	gl.ActiveTexture(gl.TEXTURE0)
	gl.BindTexture(gl.TEXTURE_2D, video.texture)

	log.Print("TexParam")
	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST)
	gl.TexParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST)

	gl.UseProgram(video.prog)
	gl.EnableVertexAttribArray(posAttrib)
	gl.EnableVertexAttribArray(texCoordAttr)

	gl.Uniform3iv(paletteLoc, nes.SPaletteRgb)

	log.Print("VertBO")
	vertVBO := genBuffer()
	checkGLError()
	gl.BindBuffer(gl.ARRAY_BUFFER, vertVBO)
	verts := f32.Bytes(binary.LittleEndian, -1.0, 1.0, -1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0)
	gl.BufferData(gl.ARRAY_BUFFER, verts, gl.STATIC_DRAW)

	textCoorBuf := genBuffer()
	checkGLError()
	gl.BindBuffer(gl.ARRAY_BUFFER, textCoorBuf)
	texVerts := f32.Bytes(binary.LittleEndian, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0)
	gl.BufferData(gl.ARRAY_BUFFER, texVerts, gl.STATIC_DRAW)

	gl.VertexAttribPointer(posAttrib, 2, gl.FLOAT, false, 0, 0)
	gl.VertexAttribPointer(texCoordAttr, 2, gl.FLOAT, false, 0, 0)
	log.Print("Started")
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

	log.Print("Reading")
	frame := <-video.pixelBuffer
	buf := make([]float32, len(frame))
	log.Print("Read frame")

	for k, v := range frame {
		buf[k] = float32(v)
	}

	log.Print("Writing")
	if video.pixelBuffer != nil {
		gl.TexImage2D(gl.TEXTURE_2D, 0, 256, 256, gl.RGBA,
			gl.UNSIGNED_SHORT_4_4_4_4, f32.Bytes(binary.LittleEndian, buf...))
	}
	log.Print("Wrote")

	gl.DrawArrays(gl.TRIANGLES, 0, 6)
	video.fpsmanager.FramerateDelay()
}

func createProgram(vertShaderSrc string, fragShaderSrc string) gl.Program {
	vertShader := loadShader(gl.VERTEX_SHADER, vertShaderSrc)
	fragShader := loadShader(gl.FRAGMENT_SHADER, fragShaderSrc)

	prog := gl.CreateProgram()

	gl.AttachShader(prog, vertShader)
	gl.AttachShader(prog, fragShader)
	gl.LinkProgram(prog)

	// TODO: Was GetProgramiv
	gl.GetProgrami(prog, gl.LINK_STATUS)

	return prog
}

func loadShader(shaderType gl.Enum, source string) gl.Shader {
	log.Print("Creating shader")
	handle := gl.CreateShader(shaderType)

	log.Print("Created shader")
	gl.ShaderSource(handle, source)
	gl.CompileShader(handle)

	log.Print("Compiled shader")
	log.Print(handle)
	// TODO: Was GetShaderiv
	gl.GetShaderi(handle, gl.COMPILE_STATUS)

	log.Print("Got shader")
	return handle
}

func attribLocation(prog gl.Program, name string) gl.Attrib {
	attrib := gl.GetAttribLocation(prog, name)

	return attrib
}

func uniformLocation(prog gl.Program, name string) gl.Uniform {
	attrib := gl.GetUniformLocation(prog, name)

	return attrib
}

func genBuffer() gl.Buffer {
	return gl.CreateBuffer()
}

func genTexture() gl.Texture {
	return gl.CreateTexture()
}

func checkGLError() {
	if glErr := gl.GetError(); glErr != gl.NO_ERROR {
		log.Fatalf("gl. error: %v", glErr)
	}
}

func DrawFrame() {
	video.drawFrame()
}

func Start() {
	log.Print("Starting")
	video.initGL()
}

func Resize(width, height int) {
	video.resize(width, height)
}
