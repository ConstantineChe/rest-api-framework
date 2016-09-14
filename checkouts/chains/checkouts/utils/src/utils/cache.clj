(ns utils.cache
  (:require [taoensso.carmine :as car :refer [wcar]]
            [utils.redis :refer [wcar*]]
            [clojure.set :as set]
            [io.pedestal.log :as log]))




(defn reset-tags [& tags]
  (let [keys (for [tag tags]
               (into #{} (wcar* (car/keys (str "*:" tag ":*")))))]
    (doseq [key (apply set/intersection keys)]
      (wcar* (car/del key)))))


(defmacro with-cache [key exp & forms]
  (let [value (gensym "value")
        cache (gensym "cache")]
      (list `if-let [cache (list `wcar* (list `car/get key))]
         cache
         (list `let [value (apply list `do forms)]
               (list `wcar* (list `car/set key value)
                     (list `car/expire key exp))
               value))))

(defn get-cache [key]
  (wcar* (car/get key)))
