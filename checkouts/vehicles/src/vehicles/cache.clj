(ns vehicles.cache
  (:require [taoensso.carmine :as car :refer [wcar]]
            [utils.cache :as cache :refer [wcar*]]))




(defn get-user-vehicles-count [user]
  (if-let [cache (wcar* (car/get (str "vehicles-count:" user)))]
    cache
    (let [cnt {:plh :val}
          key (str ":vehicles-count:" user ":")]
      (wcar* (car/set key cnt)
             (car/expire key (* 60 60)))
      cnt)))
