(ns utils.model-test
  (:require [clojure.test :refer :all]
            [utils.test :refer :all]
            [midje.sweet :refer :all]
            [utils.model :refer :all]
            [utils.db :as db]
            [clojure.core.async :as async]
            [utils.kafka-service :as service]
            [schema.test]
            [environ.core :refer [env]]))


(def connection (db/db-connection {:db (str (or (:db env) (:users-db env) "carbook") "_test")
                                   :username (:db-user env)
                                   :password (:db-password env)})))

(defmulti process-request (comp :operation :message))

(def kafka (service/kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "model_test"
                                             :auto.offset.reset       :earliest
                                             :enable.auto.commit      true}
                           :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                            :client.id "model_test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :model_test :partition 0}]
                           :handler process-request}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(use-fixtures :once schema.test/validate-schemas)
(use-fixtures :once (fn [_] (.start kafka)))


(facts "TODO"
       (fact "TODO 1"
        (+ 1 1) => 2))
