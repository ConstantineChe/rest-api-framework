(ns users.kafka
  (:require [utils.kafka-service :as service :refer [kafka]]
            [users.session :as session]
            [environ.core :refer [env]]
            [users.config :as config]
            [clojure.core.async :as async]))


(defmulti process-request (comp :operation :message))

(def kafka-component
  (kafka (merge (select-keys config/kafka
                                  [:producer-config
                                   :consumer-config
                                   :subscriptions])
                     {:producer-chan (async/chan)
                      :handler process-request})))

(def produce! (partial service/send-msg! kafka-component))

(defmethod process-request :token [{:keys [message uid] :as msg}]
  (let [{:keys [client token]} (:params message)]
    (service/response! kafka-component msg (session/unsign-token client token))))

(defmethod process-request :refresh-token [{:keys [message uid] :as msg}]
  (let [{:keys [client auth-token refresh-token]} (:params message)]
    (service/response! kafka-component msg (session/refresh-token client refresh-token auth-token))))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
