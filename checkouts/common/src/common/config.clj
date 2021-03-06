(ns common.config
  (:require [environ.core :refer [env]]))

(def db-connection {:db (or (:db env) (:users-db env) "carbook_users")
                     :username (:db-user env)
                     :password (:db-password env)})

(def redis-connection {:pool {} :spec (merge {:host (:redis-host env "127.0.0.1")
                                              :port (:redis-port env "6379")}
                                             (if-let [pass (:redis-password env)]
                                               {:password pass})
                                             (if-let [db (:redis-db env)]
                                               {:db db}))})

(def kafka {:producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                              :client.id "common"}
            :consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                              :group.id                "common"
                              :auto.offset.reset       :earliest
                              :enable.auto.commit      true}
            :subscriptions [{:topic :common :partition 0}]})

(def http-port (Integer. (:http-port env "8081")))
