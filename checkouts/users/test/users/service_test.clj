(ns users.service-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [clojure.java.jdbc :as sql]
            [ragtime.repl :as repl]
            [users.db :as db]
            [utils.db :as dbu]
            [cheshire.core :as json]
            [users.service :as service]
            [environ.core :refer [env]]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(def config (dbu/load-config db/db-connection))

(def tables ["users_tbl" "vehicles_tbl" "vehicle_makes_tbl" "vehicle_models_tbl" "vehicle_modifications_tbl"])

(defn exec-raw [query]
  (sql/with-db-connection [conn db/db-connection]
         (with-open [s (.createStatement (:connection conn))]
           (.executeUpdate s query))))

(defn init-db! [tests]
  (repl/migrate config)
  (try (tests)
       (finally (repl/rollback config (-> config :migrations count)))))

(defn clear-tables! [test]
  (try (test)
       (finally (do (println "clear tables")
                    (dorun (map #(exec-raw (str "TRUNCATE TABLE " %)) tables))))))

(use-fixtures :once init-db!)
(use-fixtures :each clear-tables!)

(with-state-changes [(before :facts (repl/migrate config))
                     (after :facts (repl/rollback config (-> config :migrations count)))]
 (facts "about users service"
        (with-state-changes [(before :facts (do (println "clear tables")
                                                (dorun (map #(exec-raw (str "TRUNCATE TABLE " %)) tables))
                                                (db/create-user {:name "test"
                                                                 :surname "tester"
                                                                 :middlename "T"
                                                                 :email "test@tet.de"
                                                                 :password "pass"
                                                                 :registration_date "2011-11-11"
                                                                 :gender "male"
                                                                 :phones ["1231232" "9332032030"]
                                                                 :status "basic"
                                                                 :dob "2011-11-11"
                                                                 :enabled true})
                                                (db/create-user {:name "test"
                                                                 :surname "tester2"
                                                                 :middlename "T"
                                                                 :email "test@test.de"
                                                                 :password "passwd"
                                                                 :registration_date "2011-11-11"
                                                                 :gender "male"
                                                                 :phones ["1231232" "9332032030"]
                                                                 :status "basic"
                                                                 :dob "2011-11-11"
                                                                 :enabled true})))]
          (fact "/users returns a list of database enties"
                (update (json/parse-string (:body (response-for service :get "/users")) true) :data
                        (fn [users] (map #(dissoc % :password) users))) =>
                {:data [{:email "test@tet.de",
                         :name "test",
                         :surname "tester",
                         :middlename "T",
                         :dob "2011-11-11",
                         :phones ["1231232" "9332032030"],
                         :status "basic",
                         :id 1,
                         :gender "male",
                         :registration_date "2011-11-11",
                         :enabled true}
                        {:email "test@test.de",
                         :name "test",
                         :surname "tester2",
                         :middlename "T",
                         :dob "2011-11-11",
                         :phones ["1231232" "9332032030"],
                         :status "basic",
                         :id 2,
                         :gender "male",
                         :registration_date "2011-11-11",
                         :enabled true}]
                 })
          )))

(facts "about addition"
       (fact "addition is commutable"
             (+ 1 2) => (+ 2 1)))
