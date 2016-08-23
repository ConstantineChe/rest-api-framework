(ns utils.redis
  (:require [taoensso.carmine :as car :refer [wcar]]
            [environ.core :refer [env]]))

(def redis-connection {:pool {} :spec (merge {:host (:redis-host env "127.0.0.1")
                                              :port (Integer. (:redis-port env "6379"))}
                                             (if-let [pass (:redis-password env)]
                                               {:password pass})
                                             (if-let [db (:redis-db env)]
                                               {:db db}))})

(defmacro wcar* [& body] `(car/wcar redis-connection ~@body))
