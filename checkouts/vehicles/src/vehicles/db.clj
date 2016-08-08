(ns vehicles.db
  (:require [utils.db :as util]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert delete defentity]]
            [schema.core :as s]
            [cheshire.core :as json]
            [utils.schema.vehicles :as vehicles-schema]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str]
            [clojure.set :as set]
            [vehicles.config :as config]
            [utils.cache :as cache]))

(def db-connection
  (util/db-connection config/db-connection))

(def migrate (util/migrate (util/load-config db-connection)))

(def rollback (util/rollback (util/load-config db-connection)))

(kdb/defdb db db-connection)

(def sql-format (f/formatters :year-month-day))

(defn transform-date [date]
  (let [[year month day] (map #(Integer. %) (str/split (str date) #"-"))]
         (f/unparse sql-format (t/to-time-zone (t/date-time year month day)
                                               (t/time-zone-for-offset 2)))))

(defn update-value [coll k f & args]
  (if (k coll)
    (apply update coll k f args)
    coll))


(defentity vehicles
  (kc/database db)
  (kc/table "vehicles_tbl"))

(defentity vehicle-makes
  (kc/database db)
  (kc/table "vehicle_makes_tbl"))

(defentity vehicle-models
  (kc/database db)
  (kc/table "vehicle_models_tbl"))

(defentity vehicle-modifications
  (kc/database db)
  (kc/table "vehicle_modifications_tbl"))

(s/defn get-user-vehicles :- [vehicles-schema/Vehicle]
  [user :- s/Int]
  (select vehicles (kc/where {:user_id user})))


(s/defn create-vehicle :- vehicles-schema/Vehicle
  [vehicle :- vehicles-schema/InputVehicle user :- s/Int]
  (let [new-vehicle (insert vehicles (kc/values (-> (assoc vehicle :enabled true)
                                                    (assoc :user_id user))))]
    (cache/reset-tags "vehicles-count" (str user))
    new-vehicle))

(s/defn delete-vehicle :- {:message :- s/Str :id :- s/Int}
  [vehicle-id :- s/Int user-id :- s/Int]
  (if (= 1 (delete vehicles (kc/where {:id vehicle-id :user_id user-id})))
    {:message "deleted" :id vehicle-id :status "success"}
    {:message (str "User has no vehicle with id " vehicle-id) :id vehicle-id :status "failed"}))

(s/defn get-users-vehicle :- (s/maybe vehicles-schema/Vehicle)
  [vehicle-id :- s/Int user-id :- s/Int]
  (first (select vehicles (kc/where {:id vehicle-id :user_id user-id}))))

(s/defn create-vehicle-make :- vehicles-schema/VehicleMake
  [vehicle-make :- vehicles-schema/InputVehicleMake]
  (insert vehicle-makes (kc/values (assoc vehicle-make :enabled true))))

(s/defn create-vehicle-model :- vehicles-schema/VehicleModel
  [vehicle-model :- vehicles-schema/InputVehicleModel]
  (insert vehicle-models (kc/values (assoc vehicle-model :enabled true))))

(s/defn create-vehicle-modification :- vehicles-schema/VehicleModification
  [vehicle-modification :- vehicles-schema/InputVehicleModification]
  (-> (insert vehicle-modifications
              (kc/values (-> (assoc vehicle-modification :enabled true)
                             (update :made_from util/cast-type "date")
                             (update :made_until util/cast-type "date"))))
      (update :made_from transform-date)
      (update :made_until transform-date)))
