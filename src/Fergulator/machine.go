package main

import (
	"fmt"
	"log"
	"runtime"
)

var (
	cpuClockSpeed = 1789773
	running       = true

	cpu      Cpu
	ppu      Ppu
	rom      Mapper

	totalCpuCycles int

	saveStateFile  string
	batteryRamFile string
)

func setResetVector() {
	high, _ := Ram.Read(0xFFFD)
	low, _ := Ram.Read(0xFFFC)

	ProgramCounter = (uint16(high) << 8) + uint16(low)
}

func RunSystem() {
	var cycles int

	for running {
		cycles = cpu.Step()
		totalCpuCycles += cycles

		for i := 0; i < 3*cycles; i++ {
			ppu.Step()
		}
//		log.Println("RUNNN")
	}
}

func RunEmulator(r gamerom) chan []uint32 {

	// Init the hardware, get communication channels
	// from the PPU and APU
	Ram.Init()
	cpu.Init()
	videoTick := ppu.Init()

	var err error
    if rom, err = LoadRom(r.bytes); err != nil {
		log.Printf("!ERROR @ LoadRom([]byte):  %v\n", err)
		return nil
	}

	// Set the game name for save states
	saveStateFile = fmt.Sprintf(".%s.state", r.name)
	batteryRamFile = fmt.Sprintf(".%s.battery", r.name)

	log.Printf("starting emulator: %v\n", batteryRamFile)

	if rom.BatteryBacked() {
		loadBatteryRam()
		defer saveBatteryFile()
	}

	setResetVector()

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go RunSystem()

	// This needs to happen on the main thread for OSX
	runtime.LockOSThread()

	return videoTick
}
