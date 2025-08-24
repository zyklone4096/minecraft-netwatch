package db

import (
	"encoding/binary"
	"math/rand"
	"time"

	"github.com/google/uuid"
)

func UuidGetMostSign(id uuid.UUID) uint64 {
	return binary.BigEndian.Uint64(id[:8])
}

func UuidGetLeastSign(id uuid.UUID) uint64 {
	return binary.BigEndian.Uint64(id[8:])
}

func RandomString(length int) string {
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	seededRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[seededRand.Intn(len(charset))]
	}
	return string(b)
}
