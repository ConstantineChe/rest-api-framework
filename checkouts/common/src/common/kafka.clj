(ns common.kafka
  (:require [utils.kafka-service :as service]
            [common.db :as db]
            [common.config :as config]
            [clojure.core.async :as async]))




(defmulti process-request (comp :operation :message))

(def kafka-component
  (service/map->Kafka (merge (select-keys config/kafka
                                  [:producer-config
                                   :consumer-config
                                   :subscriptions])
                     {:producer-chan (async/chan)
                      :handler process-request})))

(def produce! (partial send-msg! kafka-component))

(defmethod process-request :settings [{:keys [message sid]}]
  (produce! sid (:from message) {:type :response
                             :data (db/get-settings)}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
