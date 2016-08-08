(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]
            [schema.core :as s]
            [clojure.core.async :as async]))


(defn producer []
  (service/producer (:producer-config config/kafka)))

(defn consumer []
  (service/consumer (:consumer-config config/kafka)))

(def producer-chan (async/chan))

(def produce! (partial service/send-msg! producer-chan))

(defmulti process-request (comp :operation :message))

(defmethod process-request :create-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (db/create-vehicle vehicle user-id)})))

(defmethod process-request :delete-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (db/delete-vehicle vehicle-id user-id)})))

(defmethod process-request :get-users-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)
        vehicle (db/get-users-vehicle vehicle-id user-id)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data {:vehicle vehicle :status (if vehicle "success" "failed")}})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer) producer-chan)
