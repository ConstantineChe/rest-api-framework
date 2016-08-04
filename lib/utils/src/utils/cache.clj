(ns utils.cache
  (:require [taoensso.carmine :as car :refer [wcar]]
            [environ.core :refer [env]]
            [clojure.set :as set]))


(def redis-connection {:pool {} :spec (merge {:host (:redis-host env "127.0.0.1")
                                              :port (:redis-port env "6379")}
                                             (if-let [pass (:redis-password env)]
                                               {:password pass})
                                             (if-let [db (:redis-db env)]
                                               {:db db}))})

(defn reset-tags [& tags]
  (let [keys (for [tag tags]
               (into #{} (wcar redis-connection (car/keys (str "*:" tag ":*")))))]
    (doseq [key (apply set/intersection keys)]
      (wcar redis-connection (car/del key)))))
