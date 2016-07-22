(ns common.kafka
  (:require [kafka-service.core :as service]
            [common.db :as db]
            [environ.core :refer [env]]))

(def bootstrap-servers (if-let [serv (:kafka-server env)] serv "localhost:9091"))

(defn producer []
  (service/producer {:bootstrap.servers [bootstrap-servers]
                     :client.id "common"}))

(defn consumer []
  (service/consumer {:bootstrap.servers [bootstrap-servers]
                     :group.id "common"
                     :auto.offset.reset :earliest
                     :enable.auto.commit true}))

(defmulti process-request (comp :operation :message))

(defmethod process-request :settings [{:keys [message sid]}]
  (service/send-msg! sid "common" {:type :response
                           :data (db/get-settings)}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(service/start-consumer! (consumer) [{:topic :users :partition 0}] process-request)
(service/start-producer! (producer))
