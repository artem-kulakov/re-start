;; src/my_webapp/handler.clj
(ns my-webapp.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [my-webapp.views :as views]
            [my-webapp.db :as db]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [redirect]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            ))

(defn auth-request
  [req resp]
  (if-not (authenticated? req)
    (throw-unauthorized)
    resp))

(defn home
  [req]
  (auth-request req (views/home-page)))

(defn add-location
  [req]
  (auth-request req (views/add-location-results-page (:params req))))

(defn add-item
  [req]
  (db/add-item-sql (get-in req [:params :name]))
  (redirect "/app"))

(defroutes app-routes
  (GET "/"
    []
    home)
  (GET "/add-location"
    []
    (views/add-location-page))
  (POST "/add-location"
    []
    add-location)
  (GET "/location/:loc-id"
    [loc-id]
    (views/location-page loc-id))
  (GET "/all-locations"
    []
    (views/all-locations-page))
  (GET "/app"
    []
    (views/app))
  (POST "/add-item"
    []
    add-item)
  (POST "/toggle-item-complete"
    [id complete]
    (db/update-item-complete id (= complete "false"))
    (redirect "/app"))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn get-user-password-hash
  [username]
  (:USERS/PASSWORD (db/get-user-password-hash username)))

(defn my-authfn
  [req {:keys [username password]}]
  (when-let [user-password-hash (get-user-password-hash username)]
    (when (hashers/verify password user-password-hash)
      (keyword username))))

(def auth-backend
  (http-basic-backend {:realm "MyExampleSite"
                       :authfn my-authfn}))
(def app
  (wrap-reload (wrap-defaults #'app-routes site-defaults)))

(defn -main
  [& args]
  (as-> app $
    (wrap-authorization $ auth-backend)
    (wrap-authentication $ auth-backend)
    (jetty/run-jetty $ {:port 3000})))
