(ns my-webapp.db
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [buddy.hashers :as hashers]))

;; (require '[next.jdbc :as jdbc] '[next.jdbc.sql :as sql] '[buddy.hashers :as hashers])

;; You need to create "mywebapp" database

(def db-spec {:dbtype "postgresql" :dbname "mywebapp"})

;; Users

;; (jdbc/execute-one! db-spec ["
;; CREATE TABLE users (
;;   id serial primary key,
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
  (first (sql/query db-spec ["select password from users where username = ?" username])))

;; Items

;; (jdbc/execute-one! db-spec ["
;; CREATE TABLE items (
;;   id serial primary key,
;;   name varchar(30) not null,
;;   complete boolean default false,
;;   sort real
;; )
;; "])

;; (sql/insert! db-spec :items {:name "Bread"})

(defn get-all-items
  []
  (sql/query db-spec ["select id, name, complete from items order by sort desc"]))

(defn add-item-sql
  [name]
  (jdbc/execute-one! db-spec [
  "INSERT INTO items (name, sort)
  VALUES (
   ?,
   (SELECT COUNT (*) FROM items) + 1
  )"
  name]))

;; (defn add-item
;;   [name]
;;   (let [results (sql/insert! db-spec :items {:name name})]
;;     (assert (and (map? results) (:ITEMS/ID results)))
;;     results))

(defn update-item-complete
  [id complete]
  (let [result (sql/update! db-spec :items {:complete complete} {:id id})]
    (assert (= (:next.jdbc/update-count result) 1))))

(defn sort-items
  []
  (jdbc/execute-one! db-spec [
    "WITH sorted_items AS (
      SELECT id, ROW_NUMBER() OVER (ORDER BY complete DESC, sort ASC) AS new_sort FROM items
    )
    UPDATE items
    SET sort = sorted_items.new_sort
    FROM sorted_items
    WHERE sorted_items.id = items.id"
  ]))

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