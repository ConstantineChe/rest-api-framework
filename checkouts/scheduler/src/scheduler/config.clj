(ns scheduler.config
  (:require [environ.core :refer [env]]))

(def db-connection {:db (or (:db env) (:scheduler-db env) "carbook_scheduler")
                     :username (:db-user env)
                     :password (:db-password env)})

(def kafka {:producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                              :client.id "scheduler"}
            :consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                              :group.id                "scheduler"
                              :auto.offset.reset       :earliest
                              :enable.auto.commit      true}
            :consumer-subscriptions [{:topic :scheduler :partition 0}]})
