(ns utils.db
  (:require
   [korma.db :as kdb]
   [korma.core :as kc]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]
   [clj-time.format :as f]
   [clj-time.core :as t]))

(defn db-connection [config]
  (kdb/postgres config))

(defn load-config
  "Configure migrations connection and folder."
  [connection]
  {:datastore  (jdbc/sql-database connection)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  "This function performs all unfinished migrations."
  [config]
  (fn [& args] (repl/migrate config)))

(defn rollback
  "This funtion preforms rollback by one migration from current state."
  [config]
  (fn [& args] (repl/rollback config)))

(def sql-date (f/formatters :year-month-day))

(def sql-date-time (f/formatters :mysql))


(t/from-time-zone (t/now) (t/time-zone-for-id "Europe/Kiev"))

(defn cast-type [value type]
  (kc/raw (str "'" value "'::" type)))
