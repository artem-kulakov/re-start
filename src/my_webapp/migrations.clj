(ns my-webapp.migrations
  (:require [migratus.core :as migratus]))

(def config {:store                :database
             :migration-dir        "migrations/"
             :init-script          "init.sql" ;script should be located in the :migration-dir path
             ;defaults to true, some databases do not support
             ;schema initialization in a transaction
             :init-in-transaction? false
             :migration-table-name "migrations"
             :db {:dbtype "postgresql" :dbname "mywebapp"}})

;initialize the database using the 'init.sql' script
(migratus/init config)

; create a migration
(migratus/create config "create-users")

;apply pending migrations
(migratus/migrate config)

;rollback the migration with the latest timestamp
(migratus/rollback config)

;bring up migrations matching the ids
(migratus/up config 20111206154000)

;bring down migrations matching the ids
(migratus/down config 20111206154000)