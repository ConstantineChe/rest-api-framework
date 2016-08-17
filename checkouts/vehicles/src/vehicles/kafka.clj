(ns vehicles.kafka
  (:require [utils.kafka-service :as service]
            [vehicles.db :as db]
            [environ.core :refer [env]]
            [vehicles.config :as config]
            [vehicles.model :as model]
            [utils.model :as um]
            [schema.core :as s]
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

(defmethod process-request :include-modifications [{:keys [message sid]}]
  (let [{:keys [ids]} (:params message)
        modifications (um/select-ids model/vehicle-modifications ids)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data modifications})))

(defmethod process-request :include-makes [{:keys [message sid]}]
  (let [{:keys [ids]} (:params message)
        makes (um/select-ids model/vehicle-makes ids)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data makes})))

(defmethod process-request :include-models [{:keys [message sid]}]
  (let [{:keys [ids]} (:params message)
        models (um/select-ids model/vehicle-models ids)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data models})))
(defmethod process-request :include-vehicles [{:keys [message sid]}]
  (let [{:keys [ids]} (:params message)
        vehicles (um/select-ids model/vehicles ids)]
    (produce! sid (:from message) {:type :response
                                   :from "vehicles"
                                   :data vehicles})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))
