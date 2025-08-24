package route

import (
	"errors"
	"log"
	"net/http"
	"server/db"
	"strings"

	"gorm.io/gorm"
)

func InstallRoutes() {
	http.HandleFunc("/check", authorized(routeCheck))
	http.HandleFunc("/query", authorized(queryRoute))
	http.HandleFunc("/submit", authorized(submitRoute))
}

func getUser(req *http.Request) (*db.User, error) {
	auth := req.Header.Get("Authorization")
	if auth == "" || !strings.HasPrefix(auth, "Bearer") { // no authorization header or invalid header
		return nil, nil
	}

	token := auth[7:]
	usr, err := db.UserGetByApiKey(req.Context(), token)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &usr, nil
}

func authorized(handler func(http.ResponseWriter, *http.Request, *db.User)) func(writer http.ResponseWriter, request *http.Request) {
	return func(writer http.ResponseWriter, request *http.Request) {
		usr, err := getUser(request)

		if usr == nil {
			if err == nil {
				writer.WriteHeader(http.StatusUnauthorized)
			} else {
				writer.WriteHeader(http.StatusInternalServerError)
				log.Printf("Error getting user: %v", err)
			}
			return
		}

		handler(writer, request, usr)
	}
}
