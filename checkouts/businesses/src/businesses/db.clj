(ns businesses.db
  (:require [utils.db :as util]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert delete defentity]]
            [schema.core :as s]
            [cheshire.core :as json]
            [businesses.config :as config]))

(def db-connection
  (util/db-connection config/db-connection))

(def migrate (util/migrate (util/load-config db-connection)))

(def rollback (util/rollback (util/load-config db-connection)))

(kdb/defdb db db-connection)

(defn update-value [coll k f & args]
  (if (k coll)
    (apply update coll k f args)
    coll))

(defentity businesses
  (kc/pk :bsn_id_pk)
  (kc/database db)
  (kc/table "businesses_tbl")
  (kc/transform (fn [row]
                  (-> (reduce-kv (fn [row k v]
                                   (merge row {k (case (if (= org.postgresql.util.PGobject (type v)) (.getType v) :default)
                                                   ("json" "jsonb") (json/parse-string (.getValue v) true)
                                                   :default v
                                                   (.getValue v))})) {} row)
                      (update-value :thumbnails #(json/parse-string % true))))))
