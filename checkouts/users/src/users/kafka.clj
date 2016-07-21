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
            [franzy.common.metadata.protocols :refer :all]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [taoensso.nippy :as nippy]
            [users.session :as session]
            [clojure.edn :as edn]
            [clojure.core.async :as async :refer [>! <! <!! >!!]]
            [environ.core :refer [env]])
  (:import  [org.apache.kafka.common.serialization Serializer Deserializer]
            [franzy.serialization.deserializers KeywordDeserializer]
            [franzy.serialization.serializers KeywordSerializer]
            [franzy.serialization.nippy.deserializers NippyDeserializer]
            [franzy.serialization.nippy.serializers NippySerializer]))


(def bootstrap-servers (if-let [serv (:kafka-server env)] serv "localhost:9091"))

(defn producer []
  (let [pc {:bootstrap.servers [bootstrap-servers]
            :client.id "users"}
        key-serializer (serializers/keyword-serializer)
        value-serializer (nippy-serializer)
        ]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn consumer []
  (let [cc {:bootstrap.servers       [bootstrap-servers]
            :group.id                "users"
            :auto.offset.reset       :earliest
            :enable.auto.commit      true}
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

(defn send-msg! [session topic msg]
  (>!! producer-chan {:topic "users" :partition 0 :key session :value msg}))

(defn get-chan! [sid]
  (if-not (sid @sid-chans)
    (swap! sid-chans assoc sid (async/chan)))
  (sid @sid-chans))


(defmulti process-request (comp :operation :message))

(defmethod process-request :token [{:keys [message sid]}]
  (send-msg! sid "users" {:type :response
                          :data (session/unsign-token (-> message :params :token))}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))



(defn process-message [{:keys [message sid] :as msg}]
  (prn "from common: " msg)
    (case (:type message)
    :request (process-request msg)
    :response (if-let [ch (sid @sid-chans)] (>!! ch (:data message)) (println "No chan to response"))
    (println "Invalid msg format:" message))
    )


(defn start-consumer! []
  (async/thread
    (let [common-partition {:topic :common :partition 0}]
      (with-open [c (consumer)]
        (assign-partitions! c [common-partition])
        (seek-to-end-offset! c [common-partition])
        (while true
          (let [cr (poll! c)]
            (doseq [msg (into [] cr)]
              (prn msg)
              (process-message {:message (:value msg)
                                :sid (:key msg)}))))
        ))))

(defn start-producer! []
  (async/thread
    (with-open [p (producer)]
      (while true
        (let [msg (<!! producer-chan)]
          (flush! p)
          (prn msg)
          (prn (send-sync! p msg))))
      )))

(start-consumer!)
(start-producer!)


(comment (with-open [c (consumer)]
    (assign-partitions! c [{:topics :users :partition 0}])
    (clojure.pprint/pprint (into [] (poll! c))))

 (send-msg! "user" "users" "{:type :request :operation :token}"))
