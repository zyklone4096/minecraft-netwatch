package cli

import (
	"context"
	"log"
	"server/db"
	"strconv"
	"strings"
)

// updates user privilege
func userPrivilege(args []string) {
	if len(args) != 3 {
		log.Printf("Usage: user-privilege <username> <privilege> <state>")
		return
	}

	username := args[0]
	privilege := args[1]
	state, err := strconv.ParseBool(args[2])

	if err != nil {
		log.Printf("Invalid privilege value: %s", args[2])
		return
	}

	switch strings.ToUpper(privilege) {
	case "SUBMIT":
		_, err = db.UserSetSubmit(context.Background(), username, state)
	default:
		log.Printf("Invalid privilege value: %s", privilege)
		return
	}

	if err == nil {
		log.Printf("Update completed")
	} else {
		log.Printf("Update failed: %s", err)
	}
}
