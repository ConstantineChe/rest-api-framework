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
        key-serializer (serializers/string-serializer)
        value-serializer (serializers/string-serializer)]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn users-consumer []
  (let [cc {:bootstrap.servers [bootstrap-servers]
            :group.id "common"
            :auto.offset.reset :latest
            :enable.auto.commit true}
        key-deserializer (deserializers/string-deserializer)
        value-deserializer (deserializers/string-deserializer)
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
  (send-msg! sid "common" {:type :response
                                  :data (db/get-settings)}))

(defn process-message [{:keys [message sid] :as msg}]
  (prn "from users: " msg)
  (case (:type message)
    :request (process-request msg)
    :response (>!! (sid @sid-chans) (:data message))
    (println "Invalid msg format: " message)))

(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic topic :partition 0 :key session :value msg}))

(defn start-producer! []
  (async/go-loop []
    (let [msg (<! producer-chan)]
      (with-open [p (producer)]
        (send-sync! p msg))
      (recur))))

(defn start-consumer! []
  (async/go-loop []
    (with-open [c (users-consumer)]
      (assign-partitions! c [{:topic :users :partition 0}])
      (let [cr (poll! c)]
        (doseq [msg (into [] cr)]
          (>! consumer-chan {:message (:value msg)
                             :sid (:key msg)}))))
    (recur)
    ))

(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))

(start-producer!)

(start-consumer!)
