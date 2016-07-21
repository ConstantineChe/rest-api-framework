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
            [environ.core :refer [env]]
            [clojure.core.async :as async :refer [>! <! <!! >!!]])
  (:import [franzy.serialization.serializers KeywordSerializer EdnSerializer]
           [franzy.serialization.deserializers KeywordDeserializer EdnDeserializer]))

(def bootstrap-servers (if-let [serv (:kafka-server env)] serv "localhost:9091"))

(defn producer []
  (let [pc {:bootstrap.servers [bootstrap-servers]
            :client.id "common"}
        key-serializer (serializers/keyword-serializer)
        value-serializer (nippy-serializer)
        ]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn users-consumer []
  (let [cc {:bootstrap.servers [bootstrap-servers]
            :group.id "common"
            :auto.offset.reset :earliest
            :enable.auto.commit true}
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (nippy-deserializer)
        defaults (defaults/make-default-consumer-options)]
    (consumer/make-consumer cc
                            key-deserializer
                            value-deserializer
                            defaults
                            )))

(def producer-chan (async/chan))

(def consumer-chan (async/chan))

(defonce sid-chans (atom {}))


(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))

(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic "common" :partition 0 :key session :value msg}))


(defmulti process-request (comp :operation :message))

(defmethod process-request :settings [{:keys [message sid]}]
  (send-msg! sid "common" {:type :response
                           :data (db/get-settings)}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(defn process-message [{:keys [message sid] :as msg}]
  (prn "from users: " msg)
  (case (:type message)
    :request (process-request msg)
    :response (if-let [ch (sid @sid-chans)] (>!! ch (:data message)) (println "No chan to response"))
    (println "Invalid msg format: " message)))


(defn start-consumer! []
  (async/thread
    (let [users-partition {:topic :users :partition 0}]
      (with-open [c (users-consumer)]
        (assign-partitions! c [users-partition])
        (seek-to-end-offset! c [users-partition])
        (while true
          (let [cr (poll! c)]
            (doseq [msg (into [] cr)]
              (prn msg)
              (process-message {:message (:value msg)
                                :sid (:key msg)})))
          )))
    ))

(defn start-producer! []
  (async/thread
    (with-open [p (producer)]
      (while true
        (prn (send-sync! p (<!! producer-chan))))
      )))

(start-consumer!)
(start-producer!)
