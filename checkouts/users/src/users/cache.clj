(ns users.cache
  (:require [taoensso.carmine :as car :refer (wcar)]
            [utils.cache :as cache]))


(defmacro wcar* [& body] `(car/wcar cache/redis-connection ~@body))

(defn get-user-vehicles-count [user]
  (if-let [cache (wcar* (car/get (str "vehicles-count:" user)))]
    cache
    (let [cnt {:plh :val}]
      (wcar* (car/set (str ":vehicles-count:" user ":") cnt))
      cnt)))
