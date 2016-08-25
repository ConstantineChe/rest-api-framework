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
            [users.kafka :as k]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [schema.core :as s]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(def config (dbu/load-config db/db-connection))

(def tables ["users_tbl"])

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

(defn api-request
  ([method route]
   (api-request method route nil {}))
  ([method route body]
   (api-request method route body {}))
  ([method route body headers]
   (case body
     nil (response-for service method route
                   :headers (merge headers {"Content-Type" "application/json"}))
     (response-for service method route
                   :body (json/generate-string body)
                   :headers (merge headers {"Content-Type" "application/json"}))
     )))


;(use-fixtures :once (.start k/kafka-component))

(use-fixtures :once init-db!)
(use-fixtures :each clear-tables!)

(defn create-users []
  (db/create-user {:name "test"
                   :surname "tester"
                   :middlename "T"
                   :email "test@tet.de"
                   :password "pass"
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
                   :gender "male"
                   :phones ["1231232" "9332032030"]
                   :status "basic"
                   :dob "2011-11-11"
                   :enabled true}))


(with-state-changes [(before :facts (repl/migrate config))
                     (after :facts (repl/rollback config (-> config :migrations count)))]
 (facts "about users service"
        (with-state-changes [(before :facts (do (println "clear tables")
                                                (dorun (map #(exec-raw (str "TRUNCATE TABLE " %)) tables))
                                                (create-users)))]
          (fact "/users returns a list of database enties"

                (json/parse-string (:body (api-request :get "/users")) true)
                =>
                {:data [{:email "test@tet.de",
                         :name "test",
                         :surname "tester",
                         :middlename "T",
                         :dob "2011-11-11",
                         :phones ["1231232" "9332032030"],
                         :status "basic",
                         :id 1,
                         :gender "male",
                         :registration_date (f/unparse db/sql-format (t/now))
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
                         :registration_date (f/unparse db/sql-format (t/now))
                         :enabled true}]
                 })

          (fact "user can get access token"

                (json/parse-string (:body (api-request :post "/users/token"
                                                       {:password "passwd" :email "test@test.de"})) true)
                =>
                (partial s/validate {:message (s/enum "success")
                                     :data {:user (s/enum "test@test.de")
                                            :auth-token s/Str
                                            :refresh-token s/Str}}))

          (fact "but only with correct password"

                (json/parse-string (:body (api-request :post "/users/token"
                                                       {:password "incorrect" :email "test@test.de"})) true)
                =>
                {:message "invalid username or password"})

          (fact "user can refresh auth token"

                (let [{:keys [auth-token refresh-token]}
                      (:data (json/parse-string (:body (api-request :post "/users/token"
                                                                    {:password "passwd" :email "test@test.de"})) true))]
                  (json/parse-string
                   (:body (api-request :post "/users/token/refresh"
                                       {:refresh-token refresh-token}
                                       {"Authorization" (str "Bearer " auth-token)}
                                       )) true))
                =>
                (partial s/validate {:data {:token s/Str} :message (s/enum "success")}))
)))
