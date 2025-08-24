package db

import (
	"context"

	"gorm.io/gorm"
)

type User struct {
	Id     int    `gorm:"primaryKey"`
	Api    string `gorm:"type:varchar(255);uniqueIndex;autoIncrement:false"`
	Name   string
	Submit bool `gorm:"default:false"` // if this user can submit bans
}

func (User) TableName() string {
	return "netwatch_users"
}

func UserGetByApiKey(ctx context.Context, api string) (User, error) {
	return gorm.G[User](database).Select("id", "name", "submit").Where("api = ?", api).First(ctx)
}
