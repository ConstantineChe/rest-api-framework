(ns utils.kafka-service
  (:require [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as consumer]
            [franzy.clients.consumer.defaults :as defaults]
            [franzy.serialization.deserializers :as deserializers]
            [franzy.serialization.serializers :as serializers]
            [franzy.serialization.nippy.deserializers :refer [nippy-deserializer]]
            [franzy.serialization.nippy.serializers :refer [nippy-serializer]]
            [franzy.clients.consumer.protocols :refer :all]
            [franzy.clients.producer.protocols :refer :all]
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

(defn consumer [config]
  (let [cc config
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (nippy-deserializer)
        defaults (defaults/make-default-consumer-options)]
    (consumer/make-consumer cc
                            key-deserializer
                            value-deserializer
                            defaults
                            )))

(def producer-chan (async/chan))

(defonce sid-chans (atom {}))


(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))

(defn process-message [handler {:keys [message sid] :as msg}]
  (prn "from common: " msg)
    (case (:type message)
    :request (handler msg)
    :response (if-let [ch (sid @sid-chans)] (>!! ch (:data message)) (println "No chan to response"))
    (println "Invalid msg format:" message))
    )


(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic topic :partition 0 :key session :value msg}))

(defn start-consumer! [consumer partitions handler]
  (async/thread
    (with-open [c consumer]
      (assign-partitions! c partitions)
      (seek-to-end-offset! c partitions)
      (while true
        (let [cr (poll! c)]
          (doseq [msg (into [] cr)]
            (prn msg)
            (process-message handler {:message (:value msg)
                                      :sid (:key msg)})))
        ))
    ))

(defn start-producer! [producer]
  (async/thread
    (with-open [p producer]
      (while true
        (prn (send-sync! p (<!! producer-chan))))
      )))
