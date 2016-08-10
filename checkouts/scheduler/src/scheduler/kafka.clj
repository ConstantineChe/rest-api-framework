(ns scheduler.kafka
  (:require [utils.kafka-service :as service :refer [map->Kafka]]
            [scheduler.db :as db]
            [environ.core :refer [env]]
            [scheduler.config :as config]
            [schema.core :as s]
            [clojure.core.async :as async]
            [io.pedestal.log :as log])
)


(defmulti process-request (comp :operation :message))

(def kafka-component
  (map->Kafka (merge (select-keys config/kafka
                                  [:producer-config
                                   :consumer-config
                                   :subscriptions])
                     {:producer-chan (async/chan)
                      :handler process-request})))

(defmethod process-request :schedule [{:keys [message sid]}]
  (let [{:keys [time data]} (:params message)]
    (db/schedule time data)))

(defmethod process-request :test  [{:keys [message sid]}]
  (.send-msg! kafka-component "test" "test" "test"))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
