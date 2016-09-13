(ns gallery.kafka
  (:require [utils.kafka-service :as service]
            [gallery.db :as db]
            [environ.core :refer [env]]
            [gallery.config :as config]
            [gallery.model :as model]
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

(defmethod process-request :include-gallery [{:keys [message uid] :as msg}]
  (let [params (:params message)
        gallery (.GET model/gallery kafka-component
                              {:query-params params :session-id uid}
                              (:with-includes? params))]
    (service/response! kafka-component msg gallery)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
