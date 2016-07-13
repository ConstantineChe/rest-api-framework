(ns common.kafka
  (:require [clj-kafka.producer :as p]
            [clj-kafka.consumer.zk :as c]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]))


(def common-producer (p/producer {"metadata.broker.list" "localhost:9091"
                                  "serializer.class" "kafka.serializer.DefaultEncoder"
                                  "partitioner.class" "kafka.producer.DefaultPartitioner"}))

(def users-consumer (c/consumer {"zookeeper.connect" "localhost:2181"
                                  "group.id" "common"
                                  "auto.offset.reset" "smallest"
                                  "auto.commit.enable" "false"}))

(prn (zk/brokers {"zookeeper.connect" "127.0.0.1:2181"}))


(defn test-msg [] (p/send-message common-producer (p/message "test-topic" (.getBytes "TEST"))))

(p/send-message common-producer (p/message "test-topic" (.getBytes "TEST")))

(with-resource [consumer users-consumer]
  c/shutdown
  (prn (first (c/messages  consumer "test-topic"))))
