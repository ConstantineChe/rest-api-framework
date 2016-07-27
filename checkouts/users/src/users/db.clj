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

(defentity users
  (kc/table "users_tbl"))

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

;(s/with-fn-validation (get-users))

(comment (s/with-fn-validation
           (get-user-by-email "test@tet.de"))

         (s/with-fn-validation (create-user {:name "test"
                                             :surname "tester"
                                             :middlename "T"
                                             :email "test@tet.de"
                                             :password "pass"
                                             :registration_date "2011-11-11"
                                             :gender "male"
                                             :phones ["1231232" "9332032030"]
                                             :status "basic"
                                             :dob "2011-11-11"
                                             :enabled true})))
