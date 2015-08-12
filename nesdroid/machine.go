package nesdroid

import (
	"fmt"
	"log"

	"github.com/scottferg/Fergulator/nes"
)

var (
	cachePath string
)

func SetFilePath(path string) {
	if path != "" {
		cachePath = path
		log.Printf("FILE PATH: %v\n", cachePath)
	}
}

func LoadRom(jbytes []byte, name string) bool {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: loadRom: %v\n", err)
		}
	}()

	nes.GameName = name
	rom := jbytes

	if len(cachePath) > 0 {
		nes.SaveStateFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)
		nes.BatteryRamFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)

		log.Printf("cache path: %s", cachePath)
	}

	log.Printf("%v ROM: %v (%v kb)\n", string(rom[:3]), nes.GameName, len(rom)/1024)

	videoTick, err := nes.Init(rom, nil, GetKey)
	if err != nil {
		log.Println(err)
		return false
	}

	video.pixelBuffer = videoTick

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go nes.RunSystem()

	return true
}

func PauseEmulator() {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()
}

func SaveBatteryRam() {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	// TODO
	// nes.SaveBatteryFile()
}

func SaveState() {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	nes.SaveGameState()
}

func LoadState() {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	nes.LoadGameState()
}

func KeyEvent(key, event, player int) {
	//	log.Printf("key [%v] %v\n", key, event)
	p := player
	if nes.Pads[player] != nil {
		if event == 1 {
			nes.Pads[player].KeyDown(key, p)
		} else {
			nes.Pads[player].KeyUp(key, p)
		}
	}
}

func GetKey(ev int) int {
	return ev
}
