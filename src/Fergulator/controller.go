package main

import (
	"log"
)

const (
	ButtonA      = 0
	ButtonB      = 1
	ButtonSelect = 2
	ButtonStart  = 3
	AxisUp       = 4
	AxisDown     = 5
	AxisLeft     = 6
	AxisRight    = 7
)

type Controller struct {
	ButtonState [16]Word
	StrobeState int
	LastWrite   Word
	LastYAxis   [2]int
	LastXAxis   [2]int
}

func (c *Controller) SetButtonState(k int, v Word, offset int) {
	c.ButtonState[k + offset] = v
	log.Printf("key[%v] %v\n", k, v)

}

func (c *Controller) KeyDown(k int, offset int) {
	c.SetButtonState(k, 0x41, offset)
}

func (c *Controller) KeyUp(k int, offset int) {
	c.SetButtonState(k, 0x40, offset)
}

func (c *Controller) Write(v Word) {
	if v == 0 && c.LastWrite == 1 {
		// 0x4016 writes manage strobe state for
		// both controllers. 0x4017 is reserved for
		// APU
//		pads[0].StrobeState = 0
//		pads[1].StrobeState = 0
		g.ctrl.StrobeState = 0
	}

	c.LastWrite = v
}

func (c *Controller) Read() (r Word) {
	if c.StrobeState < 8 {
		r = ((c.ButtonState[c.StrobeState+8] & 1) << 1) | c.ButtonState[c.StrobeState]
	} else if c.StrobeState == 18 {
		r = 0x0
	} else if c.StrobeState == 19 {
		r = 0x0
	} else {
		r = 0x0
	}

	c.StrobeState++

	if c.StrobeState == 24 {
		c.StrobeState = 0
	}

	return
}

func NewController() *Controller {
	c := &Controller{}

	for i := 0; i < len(c.ButtonState); i++ {
		c.ButtonState[i] = 0x40
	}

	return c
}
