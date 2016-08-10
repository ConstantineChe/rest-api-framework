(ns utils.kafka-service
  (:require [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as consumer]
            [franzy.clients.consumer.defaults :as defaults]
            [franzy.common.models.types :as types]
            [franzy.clients.consumer.callbacks :as callbacks]
            [franzy.serialization.deserializers :as deserializers]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.nippy.deserializers :refer [nippy-deserializer]]
            [franzy.serialization.nippy.serializers :refer [nippy-serializer]]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.clients.producer.protocols :refer :all]
            [io.pedestal.log :as log]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [clojure.core.async :as async :refer [>! <! <!! >!!]]))


(defn producer [config]
  (let [pc config
        key-serializer (serializers/keyword-serializer)
        value-serializer (nippy-serializer)]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn consumer [config subscriptions]
  (let [cc config
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (nippy-deserializer)
        partitions (map types/map->TopicPartition subscriptions)
        rebalance-listener (callbacks/consumer-rebalance-listener
                            (fn [topic-partitions]
                              (log/info :msg "topic partitions assigned:" :partitions topic-partitions))
                            (fn [topic-partitions]
                              (log/info :msg "topic partitions revoked:" :partitions topic-partitions)))
        defaults (defaults/make-default-consumer-options {:rebalance-listener-callback rebalance-listener})]
    (consumer/make-consumer cc
                            key-deserializer
                            value-deserializer
                            defaults
                            )))

(defonce sid-chans (atom {}))


(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))

(defn process-message [handler {:keys [message sid] :as msg}]
  (println "<<<<<<<<<<<<<" (:from message) "<KAFKA: " msg)
    (case (:type message)
    :request (handler msg)
    :response (if-let [ch (sid @sid-chans)] (>!! ch (:data message)) (println "No chan to response"))
    (println "Invalid msg format:" message))
    )


(defn send-message! [producer-chan session topic msg]
  (println ">>>>>>>>>>>>CHAN>SEND: " session topic msg)
  (>!! producer-chan {:topic topic :partition 0 :key session :value msg}))

(defn start-consumer! [consumer partitions handler]
  (async/thread
    (with-open [c consumer]
      (let [topics (map (comp name :topic) partitions)]
        (subscribe-to-partitions! c topics)
        (println "Partitions subscribed to:" (partition-subscriptions c))
        (while true
          (let [cr (poll! c)]
            (doseq [msg (into [] cr)]
              (println "<<<<<<<<<<<<<<<KAFKA<GET: " msg)
              (process-message handler {:message (:value msg)
                                        :sid (:key msg)})))
          )))
    ))

(defn start-producer! [producer producer-chan]
  (async/thread
    (with-open [p producer]
      (while true
        (println ">>>>>>>>>>>KAFKA>SEND: " (send-sync! p (<!! producer-chan))))
      )))


(defrecord Kafka [consumer-config producer-config consumer-thread producer-thread producer-chan subscriptions handler]
  component/Lifecycle

  (start [component]
    (println "Starting Kafka...")
    (let [consumer-thread (start-consumer! (consumer consumer-config subscriptions) subscriptions handler)
          producer-thread (start-producer! (producer producer-config) producer-chan)]
      (println "Kafka started.")
      (assoc component :consumer-thread consumer-thread :produer-thread producer-thread)))

  (stop [component]
    (async/close! producer-thread)
    (async/close! consumer-thread)
    (println "Cannot stop Kafka Prozeß (yet)...")))

(defprotocol KafkaProtocol
  (send-msg! [component ^String sid ^String topic message]))

(extend-protocol KafkaProtocol
  Kafka
  (send-msg! [component sid topic message]
    (send-message! (:producer-chan component) sid topic message)
    component))
