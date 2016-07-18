(ns users.kafka
  (:require [franzy.clients.producer.client :as producer]
            [franzy.clients.consumer.client :as cconsumer]
            [franzy.clients.consumer.defaults :as defaults]
            [franzy.serialization.deserializers :as deserializers]
            [franzy.serialization.serializers :as serializers]
            [franzy.clients.consumer.protocols :refer :all]
            [clj-kafka.core :refer [with-resource]]
            [clj-kafka.zk :as zk]
            [clj-kafka.offset :as offset]
            [common.db :as db]
            [clojure.edn :as edn]
            [clojure.core.async :as async :refer [>! <! <!! >!!]]))


(def producer (future (producer/make-producer {:bootstrap.servers ["192.168.99.100:9092"]
                                        :retries           1
                                        :batch.size        16384
                                        :linger.ms         1
                                        :buffer.memory     33554432}
                                       (serializers/keyword-serializer)
                                       (serializers/edn-serializer))))

(def common-consumer (future (consumer/consumer {:bootstrap.servers ["192.168.99.100:9092"]
                                                 :group.id          "common"
                                                 :auto.offset.reset :latest}
                                                (deserializers/keyword-deserializer)
                                                (deserializers/edn-deserializer)
                                                (defaults/make-default-consumer-options))))


(def consumer-chan (async/chan))

(defonce sid-chans (atom {}))

(def from-kafka (comp edn/read-string #(String. %) #(.value %)))

(def to-kafka (comp #(.getBytes %) pr-str))

(def session-key (comp keyword #(String. %) #(.key %)))

(defn send-msg! [session topic msg]
  (prn "send " session topic msg)
  (p/send-message @producer (p/message topic (.getBytes session) (to-kafka msg))))

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

(async/go
  (with-open [cons @users-consumer]
    (prn "connect users consumer")
    (assign-partitions! cons {:topic "users" :partitions 0})
    (doseq [msg (poll! cons)]
      (prn msg)
      (>! consumer-chan {:message (from-kafka msg)
                         :sid (session-key msg)}))
    (prn "users consumer closed")))


(async/go-loop []
  (process-message (<! consumer-chan))
  (recur))
