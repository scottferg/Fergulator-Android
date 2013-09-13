package main

import (
	"fmt"
	"github.com/scottferg/Fergulator/nes"
)

var (
	running = true
)

func RunEmulator(r gamerom) chan []uint32 {
	videoTick, _, err := nes.Init(r.bytes, func(i int16) {}, func(ev interface{}) int {
		if k, ok := ev.(int); ok {
			return k
		}

		return -1
	})
	if err != nil {
		fmt.Println(err)
	}

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go nes.RunSystem()

	return videoTick
}
