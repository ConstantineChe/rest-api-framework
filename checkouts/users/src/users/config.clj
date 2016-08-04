(ns users.config
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
                              :client.id "users"}
            :consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                              :group.id                "users"
                              :auto.offset.reset       :earliest
                              :enable.auto.commit      true}
            :consumer-subscriptions [{:topic :users :partition 0}]})

(def http-port (:http-port env 8080))

(def vk-config {:api-key (:vk-api-key env "5573637")
                :api-secret (:vk-api-secret env "unksmyfNASsW8G7PtB0D")
                :scope "notify,offline,email"
                :callback "http://localhost/login/vk/auth"})

(def fb-config {:api-key (:fb-api-key env "274772472896284")
                :api-secret (:fb-api-secret env "3e5214972b192ceaea008e6fad221a19")
                :callback "http://localhost/login/fb/auth"})

(def google-config {:api-key (:google-api-key env "332296593369-rkjeql76as0omvr02qqvod86slp0m78o.apps.googleusercontent.com")
                    :api-secret (:google-api-secret env "EPVJDDiLqBMQMlZXiNYClqlf")
                    :scope "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/plus.login"
                    :callback "http://localhost/login/google/auth"})
