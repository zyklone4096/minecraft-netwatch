package route

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"server/db"

	"github.com/google/uuid"
)

type submitBody struct {
	Id     string  `json:"id"`
	Reason *string `json:"reason"`
}

func submitRoute(writer http.ResponseWriter, request *http.Request, user *db.User) {
	if request.Method != http.MethodPost { // bad method
		writer.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	if request.Header.Get("Content-Type") != "application/json" { // invalid body
		writer.WriteHeader(http.StatusUnsupportedMediaType)
		return
	}
	if request.ContentLength > 16383 { // body too largs
		writer.WriteHeader(http.StatusRequestEntityTooLarge)
		return
	}

	if !user.Submit { // no permission
		writer.WriteHeader(http.StatusForbidden)
		return
	}

	body, err := io.ReadAll(request.Body)
	if err != nil { // read failed
		return
	}
	args := submitBody{}
	err = json.Unmarshal(body, &args)
	if err != nil { // invalid body
		writer.WriteHeader(http.StatusBadRequest)
		return
	}

	player, err := uuid.Parse(args.Id)
	if err != nil { // invalid UUID
		writer.WriteHeader(http.StatusBadRequest)
		return
	}

	// check records
	check, err := db.RecordCheck(request.Context(), player, user.Id)
	if err != nil { // internal error
		writer.WriteHeader(http.StatusInternalServerError)
		log.Printf("Error checking records of %s from %s: %v", player, user.Name, err)
		return
	}
	if check { // already exists
		writer.WriteHeader(http.StatusConflict)
		return
	}

	// create record
	err = db.RecordPut(request.Context(), player, user.Id, args.Reason)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		log.Printf("Error putting records of %s by %s: %v", args.Id, user.Name, err)
		return
	} else {
		writer.WriteHeader(http.StatusOK)
		log.Printf("%s: submitted record for player %s: %s", user.Name, args.Id, *args.Reason)
	}
}
