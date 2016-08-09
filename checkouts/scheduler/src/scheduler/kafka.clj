(ns scheduler.kafka
  (:require [utils.kafka-service :as service]
            [scheduler.db :as db]
            [environ.core :refer [env]]
            [scheduler.config :as config]
            [scheduler.core :as scheduler]
            [schema.core :as s]
            [clojure.core.async :as async]))

(defn producer []
  (service/producer (:producer-config config/kafka)))

(defn consumer []
  (service/consumer (:consumer-config config/kafka)))

(def producer-chan (async/chan))

(def produce! (partial service/send-msg! producer-chan))

(defmulti process-request (comp :operation :message))

(defmethod process-request :schedule [{:keys [message sid]}]
  (let [{:keys [time data]} (:params message)]
    (db/schedule-job time data)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer) producer-chan)
