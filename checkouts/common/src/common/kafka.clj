(ns common.kafka
  (:require [clj-kafka.producer :as p]
            [clj-kafka.consumer.zk :as c]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [clojure.edn :as edn]
            [common.db :as db]
            [clojure.core.async :as async :refer [>! <! <!! >!!]]))


(def producer (future (p/producer {"metadata.broker.list" "localhost:9091"
                                   "serializer.class" "kafka.serializer.DefaultEncoder"
                                   "partitioner.class" "kafka.producer.DefaultPartitioner"})))

(def consumer (c/consumer {"zookeeper.connect" "localhost:2181"
                                    "group.id" "users"
                                    "auto.offset.reset" "largest"
                                    "auto.commit.enable" "false"}))



(def consumer-chan (async/chan))

(defonce sid-chans (atom {}))

(def from-kafka (comp edn/read-string #(String. %) #(.value %)))

(def to-kafka (comp #(.getBytes %) pr-str))

(def session-key (comp keyword #(String. %) #(.key %)))

(defn send-msg! [session topic msg]
  (prn "send " session topic msg)
  (p/send-message @producer (p/message topic (.getBytes session) (to-kafka msg))))

(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))

(defmulti process-request (comp :operation :message))

(defmethod process-request :settings [{:keys [message sid]}]
  (send-msg! (name sid) "common" {:type :response
                                  :data (db/get-settings)}))

(defn process-message [{:keys [message sid] :as msg}]
  (prn "from users: " msg)
  (case (:type message)
    :request (process-request msg)
    :response (>!! (sid @sid-chans) (:data message))
    nil))

(async/go
  (with-resource [cons consumer]
    c/shutdown
    (doseq [msg (c/messages cons "users")]
      (prn msg)
      (>! consumer-chan {:message (from-kafka msg)
                         :sid (session-key msg)}))))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))
