(ns vehicles.service-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [clojure.java.jdbc :as sql]
            [ragtime.repl :as repl]
            [vehicles.db :as db]
            [utils.db :as dbu]
            [cheshire.core :as json]
            [vehicles.service :as service]
            [environ.core :refer [env]]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(def config (dbu/load-config db/db-connection))

(def tables ["vehicles_tbl" "vehicle_makes_tbl" "vehicle_models_tbl" "vehicle_modifications_tbl"])

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

(use-fixtures :once init-db!)
(use-fixtures :each clear-tables!)



(with-state-changes [(before :facts (repl/migrate config))
                     (after :facts (repl/rollback config (-> config :migrations count)))]
  (facts "about vehicles service"

         (fact "api user can create vehicle makes"

              (json/parse-string
               (:body (api-request :post "/vehicles/makes" {:name "Test"}))
               true)
              =>
              {:message "Vehicle make created"})

         (fact "api user can create vehicle models"

              (json/parse-string
               (:body (api-request :post "/vehicles/models" {:name "Test"
                                                             :make_id 0}))
               true)
              =>
              {:message "Vehicle model created"})

         (fact "api user can create vehicle modifications"

              (json/parse-string
               (:body (api-request :post "/vehicles/modifications" {:name "Test"
                                                                    :model_id 0
                                                                    :made_from "18-12-1999"
                                                                    :made_until "12-12-2012"}))
               true)
              =>
              {:message "Vehicle modification created"})

         (fact "api user can create vehicles"

               (json/parse-string
                (:body (api-request :post "/vehicles" {:vehicle {:make_id 0
                                                                 :model_id 0
                                                                 :modification_id 0
                                                                 :year 1999
                                                                 :registration_number "test-rgn"
                                                                 :vin_code "test-vincd"}
                                                       :user 0}))
               true)
              =>
              {:message "Vehicle created"})
         ))
