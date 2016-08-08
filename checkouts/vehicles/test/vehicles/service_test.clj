(ns vehicles.service-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [utils.test :refer [init-db! clear-tables!]]
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

(use-fixtures :once (init-db! config))
(use-fixtures :each (clear-tables! db/db tables))



(with-state-changes [(before :facts (repl/migrate config))
                     (after :facts (repl/rollback config (-> config :migrations count)))]
  (facts "about vehicles service"

         (fact "api user can create vehicle makes"

               (json/parse-string
                (:body (api-request :post "/vehicles/makes" {:name "Test"}))
                true)
              =>
              {:message "Vehicle make created"
               :data {:id 1
                      :name "Test"
                      :enabled true}})

         (fact "api user can create vehicle models"

               (json/parse-string
                (:body (api-request :post "/vehicles/models" {:name "Test"
                                                              :make_id 1}))
                true)
              =>
              {:message "Vehicle model created"
               :data {:id 1
                      :name "Test"
                      :make_id 1
                      :enabled true}})

         (fact "api user can create vehicle modifications"

               (json/parse-string
                (:body (api-request :post "/vehicles/modifications" {:name "Test"
                                                                     :model_id 1
                                                                     :made_from "1999-11-12"
                                                                     :made_until "2012-12-11"}))
                true)
              =>
              {:message "Vehicle modification created"
               :data {:id 1
                      :name "Test"
                      :model_id 1
                      :made_from "1999-11-12"
                      :made_until "2012-12-11"
                      :enabled true}})

         (fact "api user can create vehicles"

               (json/parse-string
                (:body (api-request :post "/vehicles" {:vehicle {:user_id 1
                                                                 :make_id 1
                                                                 :model_id 1
                                                                 :modification_id 1
                                                                 :year 1999
                                                                 :registration_number "test-rgn"
                                                                 :vin_code "test-vincd"}
                                                       :user 1}))
                true)
              =>
              {:message "Vehicle created"
               :data {:enabled true
                      :id 1
                      :user_id 1
                      :make_id 1
                      :model_id 1
                      :modification_id 1
                      :registration_number "test-rgn"
                      :vin_code "test-vincd"
                      :year 1999}})
         ))
