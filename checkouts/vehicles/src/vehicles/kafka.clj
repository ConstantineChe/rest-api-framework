(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]))


(defn producer []
  (service/producer (:producer-config config/kafka)))

(defn consumer []
  (service/consumer (:consumer-config config/kafka)))



(defmulti process-request (comp :operation :message))

(defmethod process-request :users-vehicles [{:keys [message sid]}]
  (let [{:keys [user-id]} (:params message)]
    (service/send-msg! sid (:from message) {:type :response
                                            :data (db/get-user-vehicles user-id)})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer))
