(ns common.kafka
  (:require [clj-kafka.producer :as p]
            [clj-kafka.consumer.zk :as c]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [clojure.core.async :as async :refer [>! <! <!!]]))


(def common-producer (future (p/producer {"metadata.broker.list" "localhost:9091"
                                          "serializer.class" "kafka.serializer.DefaultEncoder"
                                          "partitioner.class" "kafka.producer.DefaultPartitioner"})))

(def users-consumer (future (c/consumer {"zookeeper.connect" "localhost:2181"
                                         "group.id" "common"
                                         "auto.offset.reset" "smallest"
                                         "auto.commit.enable" "false"})))



(def consumer-chan (async/chan))

(defonce consumer-chans (atom {}))


(defn test-msg [msg] (p/send-message @common-producer (p/message "test-topic" (.getBytes "session-key") (.getBytes msg))))

(defn check-chans! [msg]
  (prn msg)
  (let [msg-key (try (keyword (String. (.key msg))) (catch java.lang.Exception e :nil))]
    (if-not (msg-key @consumer-chans)
      (swap! consumer-chan assoc msg-key (async/chan)))
    msg-key))

(async/go
  (with-resource [consumer @users-consumer]
    c/shutdown
    (doseq [msg (c/messages consumer "test-topic")]
      (>! consumer-chan (String. (.value msg))))))


(async/go-loop []
  (prn (<! consumer-chan))
  (recur))
