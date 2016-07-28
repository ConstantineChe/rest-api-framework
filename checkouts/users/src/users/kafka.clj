(ns users.kafka
  (:require [utils.kafka-service :as service]
            [users.session :as session]
            [environ.core :refer [env]]))


(def bootstrap-servers (if-let [serv (:kafka-server env)] serv "localhost:9091"))

(defn producer []
  (service/producer {:bootstrap.servers [bootstrap-servers]
                     :client.id "users"}))

(defn consumer []
  (service/consumer {:bootstrap.servers       [bootstrap-servers]
                     :group.id                "users"
                     :auto.offset.reset       :earliest
                     :enable.auto.commit      true}))



(defmulti process-request (comp :operation :message))

(defmethod process-request :token [{:keys [message sid]}]
  (let [{:keys [client token]} (:params message)]
    (service/send-msg! sid "users" {:type :response
                                    :data (session/unsign-token client token)})))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(service/start-consumer! (consumer) [{:topic :common :partition 0}] process-request)
(service/start-producer! (producer))
