;; src/my_webapp/handler.clj
(ns my-webapp.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [my-webapp.views :as views]
            [my-webapp.db :as db]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
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
  (route/resources "/")
  (route/not-found "Not Found"))

(def authdata
  (let [users (db/get-all-users)]
    (into {} (map (juxt :USERS/USERNAME :USERS/PASSWORD) users))))

(defn my-authfn
  [req {:keys [username password]}]
  (when-let [user-password (get authdata username)]
    (when (= password user-password)
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
