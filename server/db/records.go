package db

import (
	"context"
	"database/sql"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

type Record struct {
	Id     uint64 `gorm:"primaryKey"`
	UserM  uint64
	UserL  uint64
	Author int
	Reason sql.NullString `gorm:"nullable"`
}

func (Record) TableName() string {
	return "netwatch_records"
}

func RecordCountByPlayer(ctx context.Context, id uuid.UUID) (int64, error) {
	return gorm.G[Record](database).Where("user_m = ? AND user_l = ?", UuidGetMostSign(id), UuidGetLeastSign(id)).Count(ctx, "*")
}

func RecordPut(ctx context.Context, player uuid.UUID, user int, reason *string) error {
	var ns sql.NullString
	if reason == nil {
		ns = sql.NullString{}
	} else {
		ns = sql.NullString{String: *reason, Valid: true}
	}

	var model = Record{
		UserM:  UuidGetMostSign(player),
		UserL:  UuidGetLeastSign(player),
		Author: user,
		Reason: ns,
	}
	return gorm.G[Record](database).Create(ctx, &model)
}

func RecordCheck(ctx context.Context, id uuid.UUID, user int) (bool, error) {
	count, err := gorm.G[Record](database).Where("user_m = ? AND user_l = ? AND author = ?", UuidGetMostSign(id), UuidGetLeastSign(id), user).Count(ctx, "*")
	if err != nil {
		return false, err
	} else {
		return count > 0, nil
	}
}
