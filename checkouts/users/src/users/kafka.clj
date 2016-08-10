(ns users.kafka
  (:require [utils.kafka-service :as service :refer [map->Kafka]]
            [users.session :as session]
            [environ.core :refer [env]]
            [users.config :as config]
            [clojure.core.async :as async]))


(defmulti process-request (comp :operation :message))

(def kafka-component
  (map->Kafka (merge (select-keys config/kafka
                                  [:producer-config
                                   :consumer-config
                                   :subscriptions])
                     {:producer-chan (async/chan)
                      :handler process-request})))

(def produce! (partial send-msg! kafka-component))

(defmethod process-request :token [{:keys [message sid]}]
  (let [{:keys [client token]} (:params message)]
    (produce! sid (:from message) {:type :response
                                            :data (session/unsign-token client token)})))

(defmethod process-request :refresh-token [{:keys [message sid]}]
  (let [{:keys [client auth-token refresh-token]} (:params message)]
    (produce! sid (:from message) {:type :response
                                            :data (session/refresh-token client refresh-token auth-token)})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
