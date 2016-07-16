(ns users.kafka
  (:require [clj-kafka.producer :as p]
            [clj-kafka.consumer.zk :as c]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [users.session :as session]
            [clojure.edn :as edn]
            [clojure.core.async :as async :refer [>! <! <!! >!!]]))


(def producer (future (p/producer {"metadata.broker.list" "localhost:9091"
                                   "serializer.class" "kafka.serializer.DefaultEncoder"
                                   "partitioner.class" "kafka.producer.DefaultPartitioner"})))

(def consumer (future (c/consumer {"zookeeper.connect" "localhost:2181"
                                   "group.id" "common"
                                   "auto.offset.reset" "largest"
                                   "auto.commit.enable" "false"})))



(def consumer-chan (async/chan))

(defonce sid-chans (atom {}))

(def from-kafka (comp edn/read-string #(String. %) #(.value %)))

(def to-kafka (comp #(.getBytes %) pr-str))

(def session-key (comp keyword #(String. %) #(.key %)))

(defn send-msg! [session topic msg]
   (p/send-message @producer (p/message topic (.getBytes session) (to-kafka msg))))

(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))


(defmulti process-request (comp :operation :message))

(defmethod process-request :token [{:keys [message sid]}]
  (send-msg! (name sid) "users" {:type :response
                                 :data (session/unsign-token (-> message :params :token))}))

(defn process-message [{:keys [message sid] :as msg}]
  (prn "from common: " msg)
    (case (:type message)
    :request (process-request msg)
    :response (>!! (sid @sid-chans) (:data message))
    nil)
  )

(async/go
  (with-resource [cons @consumer]
    c/shutdown
    (doseq [msg (c/messages cons "common")]
      (prn msg)
      (>! consumer-chan {:message (from-kafka msg)
                         :sid (session-key msg)}))))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))
