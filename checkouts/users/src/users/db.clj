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
            [clojure.set :as set]
            [users.config :as config]
            [utils.cache :as cache]
            [buddy.hashers :refer [encrypt]]))

(def db-connection
  (db/db-connection config/db-connection))

(def migrate (db/migrate (db/load-config db-connection)))

(def rollback (db/rollback (db/load-config db-connection)))

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


(defentity users
  (kc/table "users_tbl"))

(def users-from-db-transducer
  (map (fn [user] (-> (update-value user :phones #(json/parse-string (.getValue %) true))
                     (update-value :registration_date transform-date)
                     (update-value :dob transform-date)
                     (update-value :status #(.getValue %))
                     (update-value :gender #(.getValue %))))))

(s/defn create-user :- user-schema/User
  [user :- user-schema/InputUser]
  (first (into [] users-from-db-transducer
               (insert users
                       (kc/values (-> (update user :password encrypt)
                                      (update-value :dob db/cast-type "date")
                                      (update-value :gender db/cast-type "user_gender")
                                      (update-value :phones (fn [phones] (db/cast-type (json/generate-string phones) "json")))
                                      (assoc :status (db/cast-type "basic" "user_status"))
                                      (assoc :registration_date (db/cast-type (new java.util.Date) "date"))
                                      (assoc :enabled true)))))))


(s/defn get-user-by-email :- (s/maybe user-schema/User)
  [email :- s/Str]
  (first (sequence  users-from-db-transducer
                   (select users
                           (kc/where {:email email})))))

(s/defn get-users :- (s/maybe [user-schema/User]) []
  (sequence (comp (map #(dissoc % :password))
                  users-from-db-transducer)
            (select users)))
