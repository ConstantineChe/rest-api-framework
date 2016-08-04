(ns common.kafka
  (:require [utils.kafka-service :as service]
            [common.db :as db]
            [common.config :as config]))

(defn producer []
  (service/producer (:producer-config config/kafka)))

(defn consumer []
  (service/consumer (:consumer-config config/kafka)))

(defmulti process-request (comp :operation :message))

(defmethod process-request :settings [{:keys [message sid]}]
  (service/send-msg! sid (:from message) {:type :response
                             :data (db/get-settings)}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer))
