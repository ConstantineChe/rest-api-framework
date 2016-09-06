(ns features.kafka
  (:require [utils.kafka-service :as service]
            [features.db :as db]
            [environ.core :refer [env]]
            [features.config :as config]
            [features.model :as model]
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

(defmethod process-request :include-features [{:keys [message uid] :as msg}]
  (let [params (:params message)
        features (.GET model/features kafka-component
                              {:query-params params :session-id uid}
                              (:with-includes? params))]
    (service/response! kafka-component msg features)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
