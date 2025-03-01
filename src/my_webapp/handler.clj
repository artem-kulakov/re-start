(ns my-webapp.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [my-webapp.views :as views]
            [my-webapp.db :as db]
            [clojure.string :as str]
            [ring.adapter.undertow :refer [run-undertow]]
            [ring.adapter.undertow.websocket :as ws]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [redirect]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            [buddy.auth.backends.session :refer [session-backend]]
            [aero.core :as aero]
            [clojure.java.io :as io]
            [my-webapp.migrations :as migrations]
            [my-webapp.mailer :as mailer])
  (:gen-class))

(def config (aero/read-config (io/resource "config.edn")))

(def all-channels (atom {}))

(defn handler
  [list-id user-id]
  {:undertow/websocket
   {:on-open (fn
               [{:keys [channel]}]
               (swap! all-channels assoc-in [list-id channel] user-id))
    :on-message (fn [{:keys [_channel _data]}] (println "message received"))
    :on-close-message (fn
                        [{:keys [channel _message]}]
                        (let [list-channels (get @all-channels list-id)]
                          (swap! all-channels assoc list-id (dissoc list-channels channel))))}})

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
  "Check request email and password against authdata
  email and passwords.

  On successful authentication, set appropriate user
  into the session and redirect to the value of
  (:next (:query-params request)). On failed
  authentication, renders the login page."
  [request]
  (let [email (get-in request [:form-params "email"])
        password (get-in request [:form-params "password"])
        session (:session request)
        found-user (db/query :get-user {:email email})]
    (if (and (:password found-user) (hashers/verify password (:password found-user)))
      (let [next-url (get-in request [:query-params "next"] "/lists")
            updated-session (assoc session :identity (:id found-user))]
        (-> (redirect next-url)
            (assoc :session updated-session)))
      (views/login))))

(defroutes app-routes
  (GET "/echo"
    [id :as {{:keys [identity]} :session}]
    (handler id identity))
  (GET "/"
    []
    (redirect "/lists"))
  (GET "/lists"
    {{:keys [identity]} :session :as request}
    (when (authenticate request)
      (views/lists
       (db/query :get-lists {:user-id identity})
       (:flash request))))
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
        (views/list-page list items (:flash request)))))
  (POST "/lists/:id/add-item"
    [id name :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (db/query :create-item! {:name name :user-id identity :list-id (Integer/parseInt id) })
      (let [channels (keys (filter #(not= (second %) identity) (get @all-channels id)))]
        (doseq [channel channels]
          (ws/send "A new element has just been added." channel)))
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
  (GET "/sign-up"
    [token]
    (if-let [email (when token (:user_2_email (db/query :get-invite {:token token})))]
      (views/sign-up :token token :email email)
      (views/sign-up)))
  (POST "/sign-up"
    [name email password token :as request]
    (if (:exists (db/query :user-exists {:email email}))
      (views/sign-up :message "A user with this email already exists")
      (let [user-2-id (:id (first (db/query :create-user! {:name name :email email :password (hashers/derive password)})))
            updated-session (assoc (:session request) :identity user-2-id)]
        (if-let [invite (when token (db/query :get-invite {:token token}))]
          (do
            (db/query :create-contact! {:user-id (:user_id invite) :user-2-id user-2-id})
            (let [list-id (Integer/parseInt (second (clojure.string/split (:record invite) #":")))]
              (db/query :create-user-list! {:user-id user-2-id
                                            :list-id list-id})
              (db/query :accept-invite! {:token token})
              (-> (redirect (str "/lists/" list-id))
                  (assoc :session updated-session :flash "You have successfully signed up"))))
          (-> (redirect "/lists")
              (assoc :session updated-session :flash "You have successfully signed up"))))))
  (GET "/forgot-password"
    []
    (views/forgot-password))
  (POST "/forgot-password"
    [email :as request]
    (let [id (:id (db/query :get-user {:email email}))
          token (str (random-uuid))]
      (if id
       (do
         (db/query :store-token! {:id id :token token})
         (mailer/send-message email "Reset your password" (views/reset-password-message (get-in request [:headers "host"]) token))
         (views/forgot-password "Please check your inbox."))
       (views/forgot-password "There is no user with this email address."))))
  (GET "/reset-password"
    [token]
    (let [id (:id (db/query :get-user-by-token {:token token}))]
      (if id
        (views/reset-password token)
        (redirect "/login"))))
  (POST "/reset-password"
    [password token :as request]
    (let [id (:id (db/query :get-user-by-token {:token token}))]
      (if id
        (do
          (db/query :update-password! {:id id :password (hashers/derive password)})
          (-> (redirect "/lists")
              (assoc :session (assoc (:session request) :identity id))))
        (redirect "/login"))))
  (GET "/contacts/new"
    [list-id :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (views/new-contact (db/query :get-list {:user-id identity :id (Integer/parseInt list-id)}))))
  (POST "/contacts"
    [email list-id :as {{:keys [identity]} :session} :as request]
    (when (authenticate request)
      (let [token (str (random-uuid))]
        (db/query :create-invite! {:user-2-email email :user-id identity
                                   :record (str "lists:" list-id) :token token})
        (mailer/send-message
          email
          "User shared a list with you"
          (views/share-list-message (name (:scheme request)) (get-in request [:headers "host"]) token))
        (-> (redirect (str "/lists/" list-id))
            (assoc :flash (str "An email was sent to " email))))))
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

(defonce app
  (as-> #'app-routes $
  (wrap-authorization $ auth-backend)
  (wrap-authentication $ auth-backend)
  (wrap-defaults $ site-defaults)
  (wrap-session $ {:cookie-attrs {:max-age (* 3600 24 7)}})
  (wrap-flash $)
  (wrap-reload $)))

(defn -main
  [& [arg]]
  (if (= arg "migrate")
    (migrations/migrate)
    (run-undertow #'app {:port (parse-long (:port config))})))

(comment
  ;; evaluate this def form to start the webapp via the REPL:
  ;; :join? false runs the web server in the background!
  (def server (run-undertow #'app {:port 3000 :join? false}))
  ;; evaluate this form to stop the webapp via the the REPL:
  (.stop server)
  (get {1 "a" 2 "b"} 3)
  (conj [1 2 3] 4)
  (vec (remove #{1} [1 2 3]))
  (type '("1"))
  (println '(1 2 3))
  (doseq [item [1 2 3]] (println item)))
