(ns utils.db
  (:require
   [korma.db :as kdb]
   [korma.core :as kc]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]))

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

(defn cast-type [value type]
  (kc/raw (str "'" value "'::" type)))
