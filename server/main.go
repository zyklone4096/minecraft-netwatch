package main

import (
	"io"
	"log"
	"net/http"
	"os"
	"server/db"
	"server/route"

	"github.com/joho/godotenv"
	"gopkg.in/natefinch/lumberjack.v2"
)

func main() {
	err := godotenv.Load(".env")
	if err != nil {
		panic(err)
	}

	// setup logger
	log.SetOutput(io.MultiWriter(os.Stdout, &lumberjack.Logger{
		Filename:   "./logs/latest.log",
		MaxSize:    10,
		MaxBackups: 10,
		MaxAge:     30,
		Compress:   true,
	}))

	// setup database
	err = db.Setup()
	if err != nil {
		log.Panic(err)
	}

	// setup routes
	route.InstallRoutes()

	// start HTTP server
	bind := os.Getenv("NETWATCH_BIND")
	if bind == "" {
		bind = ":8080"
	}
	log.Printf("Serving at %s", bind)
	err = http.ListenAndServe(bind, nil)
	if err != nil {
		log.Panic(err)
	}
}
