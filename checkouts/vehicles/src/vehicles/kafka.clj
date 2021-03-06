(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]
            [vehicles.model :as model]
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

(def produce! (partial service/send-msg! kafka-component))

(defmethod process-request :create-vehicle [{:keys [message uid]}]
  (let [{:keys [user-id vehicle]} (:params message)]
    (produce! uid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/create-vehicle vehicle user-id))})))

(defmethod process-request :delete-vehicle [{:keys [message uid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)]
    (produce! uid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/delete-vehicle vehicle-id user-id))})))

(defmethod process-request :get-users-vehicle [{:keys [message uid] :as msg}]
  (let [{:keys [user-id vehicle-id]} (:params message)
        vehicle (s/with-fn-validation (db/get-users-vehicle vehicle-id user-id))]
    (service/response! kafka-component msg {:vehicle vehicle :status (if vehicle "success" "failed")})))

(defmethod process-request :include-modifications [{:keys [message uid] :as msg}]
  (let [params (:params message)
        modifications (.GET model/vehicle-modifications kafka-component
                                   {:query-params params :session-id uid}
                                   (:with-includes? params))]
    (service/response! kafka-component msg  modifications)))

(defmethod process-request :include-makes [{:keys [message uid] :as msg}]
  (let [params (:params message)
        makes (.GET model/vehicle-makes kafka-component
                           {:query-params params :session-id uid}
                           (:with-includes? params))]
    (service/response! kafka-component msg makes)))

(defmethod process-request :include-models [{:keys [message uid] :as msg}]
  (let [params (:params message)
        models (.GET model/vehicle-models kafka-component
                            {:query-params params :session-id uid}
                            (:with-includes? params))]
    (service/response! kafka-component msg models)))

(defmethod process-request :include-vehicles [{:keys [message uid] :as msg}]
  (let [params (:params message)
        vehicles (.GET model/vehicles kafka-component
                              {:query-params params :session-id uid}
                              (:with-includes? params))]
    (service/response! kafka-component msg vehicles)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
