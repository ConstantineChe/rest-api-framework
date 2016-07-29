(ns users.db
  (:require [utils.db :as db]
            [korma.db :as kdb]
            [korma.core :as kc :refer [select insert defentity]]
            [environ.core :refer [env]]
            [schema.core :as s]
            [cheshire.core :as json]
            [utils.schema.users :as user-schema]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str]
            [taoensso.carmine :as car :refer [wcar]]
            [clojure.set :as set]
            [buddy.hashers :refer [encrypt]]))

(def db-connection
  (db/db-connection {:db (or (:db env) (:users-db env) "carbook_users")
                     :username (:db-user env)
                     :password (:db-password env)}))

(def migrate (db/migrate (db/load-config db-connection)))

(def rollback (db/rollback (db/load-config db-connection)))

(kdb/defdb db db-connection)

(def sql-format (f/formatters :year-month-day))

(defn transform-date [date]
  (let [[year month day] (map #(Integer. %) (str/split (str date) #"-"))]
         (f/unparse sql-format (t/to-time-zone (t/date-time year month day)
                                               (t/time-zone-for-offset 2)))))

(defn reset-tags [& tags]
  (let [keys (for [tag tags]
               (into #{} (wcar {} (car/keys (str "*:" tag ":*")))))]
    (doseq [key (apply set/intersection keys)]
      (wcar {} (car/del key)))))

(reset-tags "vehicles-count" "tt")

(defentity users
  (kc/table "users_tbl"))

(defentity vehicles
  (kc/table "vehicles_tbl"))

(defentity vehicle_makes
  (kc/table "vehicle_makes_tbl"))

(defentity vehicle_models
  (kc/table "vehicle_models_tbl"))

(defentity vehicle_modifications_tbl
  (kc/table "vehicle_modifications_tbl"))

(def users-from-db-transducer
  (map (fn [user] (-> (update user :phones #(json/parse-string (.getValue %) true))
                     (update :registration_date transform-date)
                     (update :dob transform-date)
                     (update :status #(.getValue %))
                     (update :gender #(.getValue %))))))

(s/defn create-user :- s/Int
  [user :- user-schema/User]
  (:id (insert users
               (kc/values (-> (update user :password encrypt)
                              (update :registration_date db/cast-type "date")
                              (update :gender db/cast-type "user_gender")
                              (update :phones #(db/cast-type (json/generate-string %) "json"))
                              (update :status db/cast-type "user_status")
                              (update :dob db/cast-type "date"))))))

(s/defn get-user-by-email :- (s/maybe user-schema/UserWithId)
  [email :- s/Str]
  (first (sequence users-from-db-transducer
                   (select users
                           (kc/where {:email email})))))

(s/defn get-users :- (s/maybe [user-schema/UserWithId]) []
  (sequence users-from-db-transducer (select users)))

(s/defn get-users-vehicles :- s/Any
  [user :- s/Str])


(s/defn create-vehicle :- s/Int
  [vehicle :- s/Any user :- s/Str]
  (let [new-vehicle (insert vehicles vehicle)]
    (reset-tags "vehicles-count" (str user))))

(s/defn create-vehicle-make :- s/Int
  [vehicle-make :- s/Any]
  (:id (insert vehicle-makes vehicle-make)))

(s/defn create-vehicle-model :- s/Int
  [vehicle-model :- s/Any]
  (:id (insert vehicle-models vehicle-model)))

(s/defn create-vehicle-modification :- s/Int
  [vehicle-modification :- s/Any]
  (:id (insert vehicle_modifications vehicle_modification)))
