package db

import (
	"encoding/binary"

	"github.com/google/uuid"
)

func UuidGetMostSign(id uuid.UUID) uint64 {
	return binary.BigEndian.Uint64(id[:8])
}

func UuidGetLeastSign(id uuid.UUID) uint64 {
	return binary.BigEndian.Uint64(id[8:])
}
