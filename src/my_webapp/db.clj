(ns my-webapp.db
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [buddy.hashers :as hashers]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(def config (aero/read-config (io/resource "config.edn")))

;; you need to create a database "mywebapp"

;; creates functions based on the queries.sql file
(hugsql/def-db-fns "my_webapp/queries.sql" {:adapter (next-adapter/hugsql-adapter-next-jdbc)})

(def db-spec (:jdbc-database-url config))

;; wrapper for the functions created by def-db-fns
(defn query
  [fn-name params]
  (println (str (java.time.LocalDateTime/now) " query " fn-name))
  ((resolve (symbol "my-webapp.db" (name fn-name))) db-spec params))

(comment
  ;; create a user with an encrypted password
  (query :create-user! {:name "admin" :password (hashers/derive "secret")})
  )
