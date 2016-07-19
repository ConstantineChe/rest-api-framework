(ns common.kafka
  (:require [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as consumer]
            [franzy.clients.consumer.defaults :as defaults]
            [franzy.serialization.deserializers :as deserializers]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.nippy.deserializers :refer [nippy-deserializer]]
            [franzy.serialization.nippy.serializers :refer [nippy-serializer]]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.clients.producer.protocols :refer :all]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [common.db :as db]
            [clojure.edn :as edn]
            [clojure.core.async :as async :refer [>! <! <!! >!!]])
  (:import [franzy.serialization.serializers KeywordSerializer EdnSerializer]
           [franzy.serialization.deserializers KeywordDeserializer EdnDeserializer]))


(def producer
  (let [pc {:bootstrap.servers ["192.168.99.100:9092"]
            :client.id "common"}
        key-serializer (KeywordSerializer.)
        value-serializer (EdnSerializer.)]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(def common-consumer
  (let [cc {:bootstrap.servers ["192.168.99.100:9092"]
            :group.id          "common"
            :auto.offset.reset :latest}
        key-deserializer (KeywordDeserializer.)
        value-deserializer (EdnDeserializer.)
        defaults (defaults/make-default-consumer-options)]
    (consumer/make-consumer cc
                            key-deserializer
                            value-deserializer
                            defaults
                            )))


(def consumer-chan (async/chan))

(def producer-chan (async/chan))

(defonce sid-chans (atom {}))

(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic topic :partition 0 :key session :value msg}))

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

(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic topic :partition 0 :key session :value msg}))

(async/go
  (with-open [p producer]
    (prn (send-sync! p (<! producer-chan)))))

(async/go
  (with-open [cons users-consumer]
    (prn "connect users consumer")
    (assign-partitions! cons {:topic "users" :partitions 0})
    (doseq [msg (poll! cons)]
      (prn msg)
      (>! consumer-chan {:message (:value msg)
                         :sid (:key msg)}))
    (prn "users consumer closed")))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))
