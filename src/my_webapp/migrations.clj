(ns my-webapp.migrations
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(def app-config (aero/read-config (io/resource "config.edn")))

(def config
  {:datastore  (jdbc/sql-database {:connection-uri (:jdbc-database-url app-config)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  []
  (repl/migrate config))

(defn create
  "Creates migration files"
  [name]
  (let [time (.format (java.text.SimpleDateFormat. "yyyyMMddhhmmss") (new java.util.Date))]
    (map #(spit (str "resources/migrations/" time "-" name "." % ".sql") "") ["up" "down"])))

(comment
  (repl/migrate config)
  (repl/rollback config)
  (create "add-token-to-users"))
