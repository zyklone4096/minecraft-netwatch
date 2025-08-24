package cli

import (
	"context"
	"log"
	"server/db"
)

// creates new user
func newUser(args []string) {
	if len(args) != 1 {
		log.Printf("Usage: new-user <name>")
		return
	}

	username := args[0]
	if len(username) >= 32 {
		log.Printf("Username too long")
		return
	}

	// create
	go func() {
		usr, err := db.UserNew(context.Background(), username)
		if err != nil {
			log.Printf("Error creating user: %v", err)
		} else {
			log.Printf("User created as %d, API key is: %s", usr.Id, usr.Api)
		}
	}()
}
