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
        value-serializer (serializers/edn-serializer)]
    (producer/make-producer pc
                            key-serializer
                            value-serializer
                            )))

(defn common-consumer []
  (let [cc {:bootstrap.servers       [bootstrap-servers]
            :group.id                "users"
            :auto.offset.reset       :earliest
            :enable.auto.commit      true
            :auto.commit.interval.ms 1000}
        key-deserializer (deserializers/keyword-deserializer)
        value-deserializer (deserializers/edn-deserializer)
        defaults (defaults/make-default-consumer-options)]
    (consumer/make-consumer cc
                            key-deserializer
                            value-deserializer
                            defaults
                            )))


(def consumer-chan (async/chan))

(defonce sid-chans (atom {}))

(defn send-msg! [session topic msg]
  (with-open [p (producer)]
    (send-sync! p {:topic topic :partition 0 :key session :value msg})))

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
    (println "Invalid msg format: " message))
    )

(defn start-producer! []
  (async/go-loop []
    (let [msg (<! producer-chan)]

      (recur))))

(send-msg! "test" "common" {:test "tttess"})



(defn start-consumer! []
  (async/go-loop []
    (with-open [c (common-consumer)]
      (assign-partitions! c [{:topic :common :partition 0}])
      (let [cr (poll! c)]
        (doseq [msg (into [] cr)]
          (>! consumer-chan {:message (:value msg)
                             :sid (:key msg)}))))
    (recur)
    ))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))

(start-consumer!)
