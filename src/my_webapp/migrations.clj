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

(comment
  (repl/migrate config)
  (repl/rollback config))
