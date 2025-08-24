package db

import (
	"context"

	"gorm.io/gorm"
)

type User struct {
	Id     int    `gorm:"primaryKey"`
	Api    string `gorm:"type:varchar(255);uniqueIndex;autoIncrement:false"`
	Name   string `gorm:"type:varchar(32);uniqueIndex"`
	Submit bool   `gorm:"default:false"` // if this user can submit bans
}

func (User) TableName() string {
	return "netwatch_users"
}

func UserGetByApiKey(ctx context.Context, api string) (User, error) {
	return gorm.G[User](database).Select("id", "name", "submit").Where("api = ?", api).First(ctx)
}

func UserNew(ctx context.Context, name string) (*User, error) {
	key := RandomString(32)
	user := User{
		Name: name,
		Api:  key,
	}
	return &user, gorm.G[User](database).Create(ctx, &user)
}

func UserSetSubmit(ctx context.Context, name string, value bool) (int, error) {
	return gorm.G[User](database).Where("name = ?", name).Update(ctx, "submit", value)
}
