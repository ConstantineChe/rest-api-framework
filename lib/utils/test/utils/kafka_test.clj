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

(defmulti process-request2 (comp :operation :message))

(def kafka-instance (kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                              :group.id                "kafka_test"
                                              :auto.offset.reset       :latest
                                              :enable.auto.commit      true}
                            :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                              :client.id "kafka_test"}
                            :producer-chan (async/chan)
                            :subscriptions [{:topic :kafka_test :partition 0}]
                            :handler process-request}))

(def kafka-instance2 (kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                              :group.id                "kafka_test2"
                                              :auto.offset.reset       :latest
                                              :enable.auto.commit      true}
                            :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                              :client.id "kafka_test2"}
                            :producer-chan (async/chan)
                            :subscriptions [{:topic :kafka_test2 :partition 0}]
                            :handler process-request2}))

(defmethod process-request :test [{:keys [message uid]}]
  (>!! (get-chan! uid) {:test (:params message)}))

(defmethod process-request :default [msg]
  (>!! (get-chan! (:uid msg)) ;(str "Invalid request operation: " (pr-str (-> msg :message :operation)))
       :nothing)
  )

(defmethod process-request2 :test [{:keys [message uid] :as msg}]
  (>!! (get-chan! uid) {:test (:params message)}))

(defmethod process-request2 :default [msg]
  (>!! (get-chan! (:uid msg)) ;(str "Invalid request operation: " (pr-str (-> msg :message :operation)))
       :nothing)
  )

(use-fixtures :once schema.test/validate-schemas)

(facts "Abuot Kafka service"
       (against-background [(before :facts (.start kafka-instance))
                            (after :facts (.stop kafka-instance))]
         (fact "Kafka request will be handled by :opperation keyword from message"
               (let [[uid chan] (create-chan!)]
                 (send-msg! kafka-instance uid "kafka_test" {:type :request
                                                             :from "kafka_test"
                                                             :params {:p1 :v1
                                                                      :p2 :v2}})
                 (get-response! uid))
               => :nothing
               (let [[uid chan] (create-chan!)]
                 (send-msg! kafka-instance uid "kafka_test" {:type :request
                                                             :from "kafka_test"
                                                             :operation :test
                                                             :params {:p1 :v1
                                                                      :p2 :v2}})
                 (get-response! uid))
               => {:test {:p1 :v1 :p2 :v2}}

               (let [uid (request! kafka-instance "kafka_test" :none {})]
                 (get-response! uid))
               => :nothing

               (let [uid (request! kafka-instance "kafka_test" :test [1 2 4])]
                 (get-response! uid))
               => {:test [1 2 4]})
        ))
