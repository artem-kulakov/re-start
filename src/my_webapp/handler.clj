(ns my-webapp.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [my-webapp.views :as views]
            [my-webapp.db :as db]
            [clojure.string :as str]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [redirect]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            [buddy.auth.backends.session :refer [session-backend]]
            [aero.core :as aero]
            [clojure.java.io :as io]
            [my-webapp.migrations :as migrations])
  (:gen-class))

(def config (aero/read-config (io/resource "config.edn")))

(defn authenticate
  [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (do
      (let [route (:compojure/route request)]
        (println (str
                  (java.time.LocalDateTime/now)
                  " "
                  (str/upper-case (str (clojure.core/name (first route))))
                  " "
                  (last route)
                  " authenticated")))
      true)))

(defn logout
  [_]
  (-> (redirect "/login")
      (assoc :session {})))

(defn login-authenticate
  "Check request username and password against authdata
  username and passwords.

  On successful authentication, set appropriate user
  into the session and redirect to the value of
  (:next (:query-params request)). On failed
  authentication, renders the login page."
  [request]
  (let [name (get-in request [:form-params "name"])
        password (get-in request [:form-params "password"])
        session (:session request)
        found-user (db/query :get-user {:name name})]
    (if (and (:password found-user) (hashers/verify password (:password found-user)))
      (let [next-url (get-in request [:query-params "next"] "/lists")
            updated-session (assoc session :identity (:id found-user))]
        (-> (redirect next-url)
            (assoc :session updated-session)))
      (views/login))))

(defroutes app-routes
  (GET "/"
    []
    (redirect "/lists"))
  (GET "/lists"
    {{:keys [identity]} :session :as request}
    (when (authenticate request)
      (views/lists (db/query :get-lists {:user-id identity}))))
  (POST "/add-list"
    [name :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (db/query :create-list! {:name name :user-id identity})
      (redirect "/lists")))
  (GET "/lists/:id"
    [id :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (let [list (db/query :get-list {:user-id identity :id (Integer/parseInt id)})
            items (db/query :get-list-items {:id (Integer/parseInt id)})]
        (views/list-page list items))))
  (POST "/lists/:id/add-item"
    [id name :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (db/query :create-item! {:name name :user-id identity :list-id (Integer/parseInt id) })
      (redirect (str "/lists/" id))))
  (POST "/toggle-item-complete"
    [id complete :as request]
    (when (authenticate request)
      (db/query :update-item-complete! {:id (Integer/parseInt id) :complete (= complete "false")})
      (redirect (get-in request [:headers "referer"]))))
  (POST "/lists/:id/sort-items"
    [id :as request]
    (when (authenticate request)
      (db/query :sort-list-items! {:id (Integer/parseInt id)})
      (redirect (str "/lists/" id))))
  (GET "/login" [] (views/login))
  (POST "/login" [] login-authenticate)
  (GET "/logout" [] logout)

  (route/resources "/")
  (route/not-found "Not Found"))

(defn unauthorized-handler
  [request _metadata]
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

(def app
  (as-> #'app-routes $
  (wrap-authorization $ auth-backend)
  (wrap-authentication $ auth-backend)
  (wrap-defaults $ site-defaults)
  (wrap-reload $)))

(defn -main
  [& _args]
  (jetty/run-jetty #'app {:port (parse-long (:port config))}))

(comment
  ;; evaluate this def form to start the webapp via the REPL:
  ;; :join? false runs the web server in the background!
  (def server (jetty/run-jetty #'app {:port 3000 :join? false}))
  ;; evaluate this form to stop the webapp via the the REPL:
  (.stop server)
  )
