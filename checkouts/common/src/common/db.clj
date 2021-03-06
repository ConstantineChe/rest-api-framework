(ns common.db
  (:require [utils.db :as db]
            [korma.db :as kdb]
            [korma.core :as kc]
            [environ.core :refer [env]]))

(def db-connection
  (db/db-connection {:db (or (:db env) (:common-db env))
                     :username (:db-user env)
                     :password (:db-password env)}))

(def migrate (db/migrate (db/load-config db-connection)))

(def rollback (db/rollback (db/load-config db-connection)))

(defn get-settings
  ([] {:hello "Hello2"})
  ([name] {:hello "Hello2"
           :name name}))
