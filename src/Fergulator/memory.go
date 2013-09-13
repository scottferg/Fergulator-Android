package main

import (
	"fmt"
)

type Word uint8

type Memory [0x10000]Word

type MemoryError struct {
	ErrorText string
}

func (e MemoryError) Error() string {
	return e.ErrorText
}

var (
	Ram Memory
)

func fitAddressSize(addr interface{}) (v int, e error) {
	switch a := addr.(type) {
	case Word:
		v = int(a)
	case int:
		v = int(a)
	case uint16:
		v = int(a)
	default:
		e = MemoryError{ErrorText: "Invalid type used"}
	}

	return
}

func (m *Memory) Init() {
	for index, _ := range m {
		m[index] = 0x00
	}
}

func (m *Memory) ReadMirroredRam(a int) Word {
	offset := a % 0x8
	return m[0x2000+offset]
}

func (m *Memory) WriteMirroredRam(v Word, a int) {
	offset := a % 0x8
	m[0x2000+offset] = v
}

func (m *Memory) Write(address interface{}, val Word) error {
	if a, err := fitAddressSize(address); err == nil {
		if a >= 0x2008 && a < 0x4000 {
			fmt.Printf("Address write: 0x%X\n", a)
		}

		if a >= 0x2000 && a <= 0x2007 {
			ppu.PpuRegWrite(val, a)
			// m.WriteMirroredRam(val, a)
		} else if a == 0x4014 {
			ppu.PpuRegWrite(val, a)
			m[a] = val
		} else if a == 0x4016 {
			g.ctrl.Write(val)
			m[a] = val
		} else if a == 0x4017 {
			g.ctrl.Write(val)
			m[a] = val
		} else if a >= 0x8000 && a <= 0xFFFF {
			// MMC1
			rom.Write(val, a)
			return nil
		} else if a >= 0x6000 && a < 0x8000 {
			m[a] = val
		} else {
			m[a] = val
		}

		return nil
	}

	return MemoryError{ErrorText: "Invalid address used"}
}

func (m *Memory) Read(address interface{}) (Word, error) {
	a, _ := fitAddressSize(address)

	switch {
	case a >= 0x2008 && a < 0x4000:
		offset := a % 0x8
		return ppu.PpuRegRead(0x2000 + offset)
	case a <= 0x2007 && a >= 0x2000:
		//ppu.Run(cpu.Timestamp)
		return ppu.PpuRegRead(a)
	case a == 0x4016:
		return g.ctrl.Read(), nil
	case a == 0x4017:
		return g.ctrl.Read(), nil
	}

	return m[a], nil
}
