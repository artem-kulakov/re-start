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
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            [buddy.auth.backends.session :refer [session-backend]]
            ))

(defn authenticate-request
  [request response]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (do
      (let [route (:compojure/route request)]
        (println (str (clojure.string/upper-case (str (clojure.core/name (first route)))) " " (last route) " authenticated")))
      response)))

(defn logout
  [request]
  (-> (redirect "/login")
      (assoc :session {})))

(defn get-user-password-hash
  [username]
  (:users/password (db/get-user-password-hash username)))

(defn login-authenticate
  "Check request username and password against authdata
  username and passwords.

  On successful authentication, set appropriate user
  into the session and redirect to the value of
  (:next (:query-params request)). On failed
  authentication, renders the login page."
  [request]
  (let [username (get-in request [:form-params "username"])
        password (get-in request [:form-params "password"])
        session (:session request)
        found-password-hash (get-user-password-hash username)]
    (if (and found-password-hash (hashers/verify password found-password-hash))
      (let [next-url (get-in request [:query-params "next"] "/app")
            updated-session (assoc session :identity (keyword username))]
        (-> (redirect next-url)
            (assoc :session updated-session)))
      (views/login))))

(defn home
  [req]
  (authenticate-request req (views/home-page)))

(defn add-location
  [req]
  (authenticate-request req (views/add-location-results-page (:params req))))

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
    request
    (authenticate-request request (views/app)))
  (POST "/add-item"
    [name :as request]
    (db/add-item-sql name)
    (authenticate-request request (redirect "/app")))
  (POST "/toggle-item-complete"
    [id complete :as request]
    (db/update-item-complete (Integer/parseInt id) (= complete "false"))
    (authenticate-request request (redirect "/app")))
  (POST "/sort-items"
    request
    (db/sort-items)
    (authenticate-request request (redirect "/app")))

  (GET "/login" [] (views/login))
  (POST "/login" [] login-authenticate)
  (GET "/logout" [] logout)

  (route/resources "/")
  (route/not-found "Not Found"))

(defn unauthorized-handler
  [request metadata]
  (cond
    ;; If request is authenticated, raise 403 instead
    ;; of 401 (because user is authenticated but permission
    ;; denied is raised).
    (authenticated? request)
    (-> (views/error)
        (assoc :status 403))
    ;; In other cases, redirect the user to login page.
    :else
    (let [current-url (:uri request)]
      (redirect (format "/login?next=%s" current-url)))))

(def auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))

(defn -main
  [& args]
  (as-> app-routes $
    (wrap-authorization $ auth-backend)
    (wrap-authentication $ auth-backend)
    (wrap-defaults $ site-defaults)
    (wrap-reload $)
    (jetty/run-jetty $ {:port 3000})))