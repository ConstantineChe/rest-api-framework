(ns users.kafka
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
            [users.session :as session]
            [clojure.edn :as edn]
            [clojure.core.async :as async :refer [>! <! <!! >!!]])
  (:import [franzy.serialization.serializers KeywordSerializer EdnSerializer]
           [franzy.serialization.deserializers KeywordDeserializer EdnDeserializer]))


(defn producer []
  (let [pc {:bootstrap.servers ["192.168.99.100:9092"]
            :client.id "users"}
        key-serializer (serializers/string-serializer)
        value-serializer (nippy-serializer)]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn common-consumer []
  (let [cc {:bootstrap.servers ["192.168.99.100:9092"]
            :group.id          "users"
            :auto.offset.reset :latest}
        key-deserializer (deserializers/string-deserializer)
        value-deserializer (nippy-deserializer)
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

(defmethod process-request :token [{:keys [message sid]}]
  (send-msg! sid "users" {:type :response
                          :data (session/unsign-token (-> message :params :token))}))

(defn process-message [{:keys [message sid] :as msg}]
  (prn "from common: " msg)
    (case (:type message)
    :request (process-request msg)
    :response (>!! (sid @sid-chans) (:data message))
    nil)
    )

(async/go
  (with-open [p (producer)]
    (prn (send-sync! p (<! producer-chan)))))

(async/go
  (with-open [c (common-consumer)]
    (prn "connect common consumer")
    (assign-partitions! cons {:topic "common" :partitions 0})
    (doseq [msg (poll! c)]
      (prn msg)
      (>! consumer-chan {:message (:value msg)
                         :sid (:key msg)}))
    (prn "closed common consumer")))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))
