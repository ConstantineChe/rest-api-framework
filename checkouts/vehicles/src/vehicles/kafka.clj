(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]
            [schema.core :as s]
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

(defmethod process-request :create-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/create-vehicle vehicle user-id))})))

(defmethod process-request :delete-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data (s/with-fn-validation (db/delete-vehicle vehicle-id user-id))})))

(defmethod process-request :get-users-vehicle [{:keys [message sid]}]
  (let [{:keys [user-id vehicle-id]} (:params message)
        vehicle (s/with-fn-validation (db/get-users-vehicle vehicle-id user-id))]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data {:vehicle vehicle :status (if vehicle "success" "failed")}})))
