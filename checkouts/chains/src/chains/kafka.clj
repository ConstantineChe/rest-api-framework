(ns chains.kafka
  (:require [utils.kafka-service :as service]
            [chains.db :as db]
            [environ.core :refer [env]]
            [chains.config :as config]
            [chains.model :as model]
            [utils.model :as um]
            [schema.core :as s]
            [io.pedestal.log :as log]
            [clojure.core.async :as async])
)


(defmulti process-request (comp :operation :message))

(def kafka-component
  (service/kafka (merge (select-keys config/kafka
                                          [:producer-config
                                           :consumer-config
                                           :subscriptions])
                             {:producer-chan (async/chan)
                              :handler process-request})))

(defmethod process-request :include-chains [{:keys [message uid] :as msg}]
  (let [params (:params message)
        chains (.GET model/chains kafka-component
                              {:query-params params :session-id uid}
                              (:with-includes? params))]
    (service/response! kafka-component msg chains)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
