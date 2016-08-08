(ns scheduler.db
  (:require [utils.db :as util]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert delete defentity]]
            [schema.core :as s]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str]
            [scheduler.config :as config]))

(def db-connection
  (util/db-connection config/db-connection))

(def migrate (util/migrate (util/load-config db-connection)))

(def rollback (util/rollback (util/load-config db-connection)))

(kdb/defdb db db-connection)

(defentity schedule
  (kc/database db)
  (kc/table "schedule_tbl"))
