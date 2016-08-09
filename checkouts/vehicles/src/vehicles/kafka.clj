(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]
            [schema.core :as s]
            [clojure.core.async :as async]))




(defmethod process-request :create-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/create-vehicle vehicle user-id))})))

(defmethod process-request :delete-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/delete-vehicle vehicle-id user-id))})))

(defmethod process-request :get-users-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)
        vehicle (s/with-fn-validation (db/get-users-vehicle vehicle-id user-id))]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data {:vehicle vehicle :status (if vehicle "success" "failed")}})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer) producer-chan)
