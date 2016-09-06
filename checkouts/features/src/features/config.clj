(ns features.config
  (:require [environ.core :refer [env]]))

(def db-connection {:db (or (:db env) (:vehicles-db env) "carbook")
                     :username (:db-user env)
                     :password (:db-password env)})

(def redis-connection {:pool {} :spec (merge {:host (:redis-host env "127.0.0.1")
                                              :port (Integer. (:redis-port env "6379"))}
                                             (if-let [pass (:redis-password env)]
                                               {:password pass})
                                             (if-let [db (:redis-db env)]
                                               {:db db}))})

(def kafka {:producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                              :client.id "features"}
            :consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                              :group.id                "features"
                              :auto.offset.reset       :latest
                              :enable.auto.commit      true}
            :subscriptions [{:topic :features :partition 0}]})

(def http-port (:http-port env 8083))
