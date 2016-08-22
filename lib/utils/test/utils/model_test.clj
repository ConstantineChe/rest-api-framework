(ns utils.model-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [utils.model :refer :all]
            [clojure.core.async :as async]
            [utils.kafka-service :as service]
            [environ.core :refer [env]]))

(defmulti process-request (comp :operation :message))

(def kafka (service/kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "test"
                                             :auto.offset.reset       :earliest
                                             :enable.auto.commit      true}
                           :prducer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                            :client.id "test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :topic1 :partition 0}]
                           :handler process-request}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))

(use-fixtures (.start kafka))

(facts "TODO"
       (fact "TODO 1"
        (+ 1 1) => 2))
