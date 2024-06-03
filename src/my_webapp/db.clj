(ns my-webapp.db
  (:require [next.jdbc.sql :as sql]))

;; (require '[next.jdbc :as jdbc] '[next.jdbc.sql :as sql])

(def db-spec {:dbtype "h2" :dbname "./my-db"})

;; (jdbc/execute-one! db-spec ["
;; CREATE TABLE users (
;;   id bigint primary key auto_increment,
;;   username varchar(30),
;;   password varchar(30)
;; )
;; "])

;; (sql/insert! db-spec :users {:username "admin" :password "secret"})

(defn add-location-to-db
  [x y]
  (let [results (sql/insert! db-spec :locations {:x x :y y})]
    (assert (and (map? results) (:LOCATIONS/ID results)))
    results))

(defn get-xy
  [loc-id]
  (let [results (sql/query db-spec
                           ["select x, y from locations where id = ?" loc-id])]
    (assert (= (count results) 1))
    (first results)))

(defn get-all-locations
  []
  (sql/query db-spec ["select id, x, y from locations"]))

(comment
  (get-all-locations)
  ;; => [#:LOCATIONS{:ID 1, :X 8, :Y 9}]
  (get-xy 1)
  ;; => #:LOCATIONS{:X 8, :Y 9}
  )