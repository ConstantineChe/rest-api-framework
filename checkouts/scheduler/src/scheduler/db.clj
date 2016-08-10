(ns scheduler.db
  (:require [utils.db :as util]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert delete defentity]]
            [schema.core :as s]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str]
            [scheduler.config :as config]
            [cheshire.core :as json]))

(def db-connection
  (util/db-connection config/db-connection))

(def migrate (util/migrate (util/load-config db-connection)))

(def rollback (util/rollback (util/load-config db-connection)))

(kdb/defdb db db-connection)

(defentity schedule
  (kc/database db)
  (kc/table "schedule_tbl"))

(defn schedule-job [time data]
  (insert schedule (kc/values {:scheduled_to (util/cast-type time "timestamp")
                               :data (util/cast-type (json/generate-string data) "json")
                               :executed false})))

(defn get-jobs []
  (select schedule (kc/where {:scheduled_to [<= (kc/sqlfn now)]
                              :executed false})))

(defn mark-as-executed [id]
  (kc/update schedule
          (kc/set-fields {:executed true
                          :executed_at (kc/sqlfn now)})
          (kc/where {:id id})))

;(schedule-job "2016-08-08 18:05:00" {:test "test" :do :do-sou :mek 18})
