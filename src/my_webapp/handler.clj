;; src/my_webapp/handler.clj
(ns my-webapp.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [my-webapp.views :as views]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [compojure.response :refer [render]]
            [clojure.java.io :as io]
            [ring.util.response :refer [response redirect content-type]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            ))

(defn home
  [req]
  (if-not (authenticated? req)
    (throw-unauthorized)
    (views/home-page)))

(defn add-location
  [req]
  (if-not (authenticated? req)
    (throw-unauthorized)
    (views/add-location-results-page (:params req))))

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
  {:admin "secret"
   :test "secret"})

(defn my-authfn
  [req {:keys [username password]}]
  (when-let [user-password (get authdata (keyword username))]
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
