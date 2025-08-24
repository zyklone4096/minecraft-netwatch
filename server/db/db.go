package db

import (
	"database/sql"
	"errors"
	"log"
	"os"
	"time"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var database *gorm.DB

func Setup() error {
	conn := os.Getenv("NETWATCH_DB")
	if conn == "" { // bad environment
		return errors.New("no NETWATCH_DB environment variable")
	}

	log.Println("Connecting to database")
	sqlDB := sql.DB{}
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(100)
	sqlDB.SetConnMaxLifetime(time.Hour)

	db, err := gorm.Open(mysql.Open(conn), &gorm.Config{
		ConnPool: &sqlDB,
		Logger:   logger.Default.LogMode(logger.Silent),
	})
	if err != nil {
		return err
	}
	database = db

	err = db.AutoMigrate(User{}, Record{})
	if err != nil {
		return err
	}

	// success
	return nil
}
