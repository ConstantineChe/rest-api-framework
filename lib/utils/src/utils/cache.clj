(ns utils.cache
  (:require [taoensso.carmine :as car :refer [wcar]]
            [environ.core :refer [env]]
            [clojure.set :as set]
            [io.pedestal.log :as log]))


(def redis-connection {:pool {} :spec (merge {:host (:redis-host env "127.0.0.1")
                                              :port (Integer. (:redis-port env "6379"))}
                                             (if-let [pass (:redis-password env)]
                                               {:password pass})
                                             (if-let [db (:redis-db env)]
                                               {:db db}))})

(defmacro wcar* [& body] `(car/wcar redis-connection ~@body))

(defn reset-tags [& tags]
  (let [keys (for [tag tags]
               (into #{} (wcar redis-connection (car/keys (str "*:" tag ":*")))))]
    (doseq [key (apply set/intersection keys)]
      (wcar redis-connection (car/del key)))))


(defmacro with-cache [key & forms]
  (let [value (gensym "value")
        cache (gensym "cache")]
      (list `if-let [cache (list `wcar* (list `car/get key))]
         cache
         (list `let [value (apply list `do forms)]
               (list `wcar* (list `car/set key value)
                     (list `car/expire key (list `* 60 60)))
               value))))
