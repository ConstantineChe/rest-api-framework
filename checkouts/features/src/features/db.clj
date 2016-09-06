(ns features.db
  (:require [utils.db :as util]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert delete defentity]]
            [schema.core :as s]
            [features.config :as config]))

(def db-connection
  (util/db-connection config/db-connection))

(def migrate (util/migrate (util/load-config db-connection)))

(def rollback (util/rollback (util/load-config db-connection)))

(kdb/defdb db db-connection)

(defentity features
  (kc/pk :ftr_id_pk)
  (kc/database db)
  (kc/table "features_tbl"))

(defentity business-features
  (kc/database db)
  (kc/pk :bft_id_pk)
  (kc/table "busines_features_tbl"))
