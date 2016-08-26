(ns utils.kafka-test
  (:require [utils.test :refer :all]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [utils.kafka-service :as k :refer :all]
            [environ.core :refer [env]]
            [schema.test]
            [clojure.core.async :as async :refer [<!! >!!]]
))

(defmulti process-request (comp :operation :message))

(def kafka-instance (kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "kafka_test"
                                             :auto.offset.reset       :latest
                                             :enable.auto.commit      true}
                           :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                            :client.id "kafka_test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :kafka_test :partition 0}]
                            :handler process-request}))

(defmethod process-request :test [{:keys [message uid]}])

(defmethod process-request :default [msg]
  (>!! (get-chan! (:uid msg)) ;(str "Invalid request operation: " (pr-str (-> msg :message :operation)))
       ::k/nothing)
  )

(use-fixtures :once schema.test/validate-schemas)

(facts "Abuot Kafka service"
       (against-background [(before :facts (.start kafka-instance))]
         (fact "Message with unknown operation id goes to default handler method"
               (let [[uid chan] (create-chan!)]
                 (send-msg! kafka-instance uid "kafka_test" {:type :request
                                                             :from "kafka_test"
                                                             :params {:p1 :v1
                                                                      :p2 :v2}})
                 (get-response! uid)
                 ) => ::k/nothing)))
