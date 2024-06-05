(ns my-webapp.db
  (:require [next.jdbc.sql :as sql]
            [buddy.hashers :as hashers]))

;; (require '[next.jdbc :as jdbc] '[next.jdbc.sql :as sql])

(def db-spec {:dbtype "h2" :dbname "./my-db"})

;; Users

;; (jdbc/execute-one! db-spec ["
;; CREATE TABLE users (
;;   id bigint primary key auto_increment,
;;   username varchar(30),
;;   password varchar(100)
;; )
;; "])

;; (sql/insert! db-spec :users {:username "admin" :password (hashers/derive "secret")})

(defn get-all-users
  []
  (sql/query db-spec ["select username, password from users"]))

(defn get-user-password-hash
  [username]
  (let [results (sql/query db-spec
                           ["select password from users where username = ?" username])]
    (assert (= (count results) 1))
    (first results)))

;; Items

;; (jdbc/execute-one! db-spec ["
;; CREATE TABLE items (
;;   id bigint primary key auto_increment,
;;   name varchar(30) not null,
;;   complete boolean default false,
;;   sort real
;; )
;; "])

;; (sql/insert! db-spec :items {:name "Bread"})

(defn get-all-items
  []
  (sql/query db-spec ["select name, complete from items order by sort desc"]))

;; Locations

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