package route

import (
	"encoding/json"
	"log"
	"net/http"
	"server/db"

	"github.com/google/uuid"
)

type queryResponse struct {
	Count int64 `json:"count"`
}

func queryRoute(writer http.ResponseWriter, request *http.Request, user *db.User) {
	if request.Method == "GET" {
		id, err := uuid.Parse(request.URL.Query().Get("uuid"))
		if err != nil { // missing or invalid uuid parameter
			writer.WriteHeader(http.StatusBadRequest)
			return
		}

		result, err := db.RecordCountByPlayer(request.Context(), id)
		if err != nil { // database error
			writer.WriteHeader(http.StatusInternalServerError)
			log.Printf("Error counting records of %s for user %s: %v", id.String(), user.Name, err)
		} else {
			resp, err := json.Marshal(queryResponse{
				Count: result,
			})
			if err != nil { // json error???
				writer.WriteHeader(http.StatusInternalServerError)
			} else {
				writer.WriteHeader(http.StatusOK)
				_, _ = writer.Write(resp)
			}
		}
	} else {
		writer.WriteHeader(http.StatusMethodNotAllowed)
	}
}
