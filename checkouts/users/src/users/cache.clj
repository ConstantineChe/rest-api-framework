(ns users.cache
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def redis-connection {})

(defmacro wcar* [& body] `(car/wcar redis-connection ~@body))



(defn get-user-vehicles-count [user]
  (if-let [cache (wcar* (car/get (str "vehicles-count:" user)))]
    cache
    (let [cnt {:plh :val}]
      (wcar* (car/set (str ":vehicles-count:" user ":") cnt))
      cnt)))

(get-user-vehicles-count "ttt")
