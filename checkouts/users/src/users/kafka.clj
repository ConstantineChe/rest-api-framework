(ns users.kafka
  (:require [utils.kafka-service :as service]
            [users.session :as session]
            [environ.core :refer [env]]
            [users.config :as config]
            [clojure.core.async :as async]))


(defn producer []
  (service/producer (:producer-config config/kafka)))

(defn consumer []
  (service/consumer (:consumer-config config/kafka)))

(def producer-chan (async/chan))

(def produce! (partial service/send-msg! producer-chan))

(defmulti process-request (comp :operation :message))

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



(service/start-consumer! (consumer) (:consumer-subscriptions config/kafka) process-request)
(service/start-producer! (producer) producer-chan)
