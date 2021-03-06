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

(defonce uid-chans (atom {}))

(defn get-chan! [uid]
  (if-not (uid @uid-chans)
    (swap! uid-chans assoc uid (async/chan 1)))
  (uid @uid-chans))

(defn create-chan! []
  (let [chan (async/chan)
        uid (-> (java.util.UUID/randomUUID) str keyword)]
    (swap! uid-chans assoc uid chan)
    [uid chan]))

(defn get-response! [uid]
  (let [response (try (<!! (uid @uid-chans))
                      (catch Exception e :nothing)
                      (finally (swap! uid-chans dissoc uid)))]
    response))

(defn process-message [handler {:keys [message uid] :as msg}]
  (log/debug :desc "got message from Kafka" :topic (:from message) :message msg)
    (case (:type message)
    :request (handler msg)
    :response (if-let [ch (uid @uid-chans)] (>!! ch (:data message)) (println "No chan to response"))
    (log/error :error "Invalid msg format:" :message message))
    )


(defn send-message! [producer-chan uid topic msg]
  (log/debug :desc "sending message to producer chan" :uid uid
             :topic topic :message  msg :chan producer-chan)
  (async/go (>! producer-chan {:topic topic :partition 0 :key uid :value msg})))


(defn start-consumer! [consumer partitions handler terminate-ch]
  (async/thread
    (with-open [c consumer]
      (let [topics (map (comp name :topic) partitions)]
        (subscribe-to-partitions! c topics)
        (log/info :msg "Partitions subscribed" :partitions (partition-subscriptions c))
        (loop []
          (let [cr (poll! c)]
            (doseq [msg (into [] cr)]
              (log/debug :desc "Kafka message consumed" :message msg)
              (async/go (process-message handler {:message (:value msg)
                                                  :uid (:key msg)})))
            (let [[val chan] (async/alts!! [terminate-ch (async/timeout 1)])]
              (if-not (= chan terminate-ch)
                (recur))))))))
  terminate-ch)

(defn start-producer! [producer producer-chan terminate-ch]
  (async/thread
    (with-open [p producer]
      (loop []
        (let [msg (<!! producer-chan)
              [val chan] (async/alts!! [terminate-ch (async/timeout 1)])]
          (log/debug :desc "Async send message to Kafka" :message msg)
          (send-async! p msg)
          (if-not (= chan terminate-ch)
            (recur))
          ))))
  terminate-ch)

(defprotocol PKafka
  (send-msg! [component ^String uid ^String topic message])

  (request! [component topic operation params])

  (response! [component request data]))

(defrecord Kafka [consumer-config producer-config consumer-thread producer-thread producer-chan subscriptions handler]
  component/Lifecycle

  (start [component]
    (println "Starting Kafka...")
    (let [consumer-thread (start-consumer! (consumer consumer-config subscriptions) subscriptions handler consumer-thread)
          producer-thread (start-producer! (producer producer-config) producer-chan producer-thread)]
      (println "Kafka started.")
      (assoc component :consumer-thread consumer-thread :producer-thread producer-thread)))

  (stop [component]
    (println "Stopping Kafka Prozeß...")
    (async/close! producer-thread)
    (async/close! consumer-thread)
    (println "Kafka stopped")))


(extend-protocol PKafka
  Kafka
  (send-msg! [component uid topic message]
    (send-message! (:producer-chan component) uid topic message)
    component)

  (request! [component topic operation params]
    (let [[uid] (create-chan!)]
      (send-message! (:producer-chan component) uid topic
                     {:type :request
                      :from (-> component :subscriptions first :topic name)
                      :operation operation
                      :params params})
      uid))

  (response! [component request data]
    (send-message! (:producer-chan component) (:uid request) (-> request :message :from)
               {:type :response :data data})))

(defn kafka [config]
  (map->Kafka (assoc config :consumer-thread (async/chan) :producer-thread (async/chan))))
