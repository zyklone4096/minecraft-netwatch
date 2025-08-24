package route

import (
	"encoding/json"
	"log"
	"net/http"
	"server/db"
)

// contains api version and username
type checkResponse struct {
	ApiVersion int
	User       string
}

func routeCheck(writer http.ResponseWriter, request *http.Request, user *db.User) {
	if request.Method == "GET" {
		resp, err := json.Marshal(checkResponse{ApiVersion: 1, User: user.Name})
		if err == nil {
			writer.WriteHeader(http.StatusOK)
			_, _ = writer.Write(resp)
		} else {
			writer.WriteHeader(http.StatusInternalServerError)
			log.Printf("Error marshling check response: %v", err)
		}
	} else if request.Method == "HEAD" {
		writer.WriteHeader(http.StatusOK)
	} else {
		writer.WriteHeader(http.StatusMethodNotAllowed)
	}
}
