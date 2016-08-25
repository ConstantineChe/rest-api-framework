(ns utils.kafka-test
  (:require [utils.test :refer :all]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [utils.kafka-service :refer :all]
            [environ.core :refer [env]]
            [clojure.core.async :as async]
))

(defmulti process-request (comp :operation :message))

(def component (kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "kafka_test"
                                             :auto.offset.reset       :earliest
                                             :enable.auto.commit      true}
                           :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                            :client.id "kafka_test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :kafka_test :partition 0}]
                           :handler process-request}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))

(use-fixtures :once schema.test/validate-schemas)
(use-fixtures :once (fn [_] (.start kafka)))

(facts "TODO"
       (fact "TODO 1"
        (+ 1 1) => 2))
