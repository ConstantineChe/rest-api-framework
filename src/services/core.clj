(ns services.core
  (:require [users.server :as users]
            [common.server :as commons]
            [vehicles.server :as vehicles]
            [io.pedestal.http :as http])
  (:gen-class))

(defn -main
  "run services"
  [& [arg :as args]]
  (let [dev? (if (= "dev" arg) true)]
    (when dev?
      (users/run-dev {::http/port 8080})
      (commons/run-dev {::http/port 8081})
      (vehicles/run-dev {::http/port 8082}))
    (when-not dev?
      (users/-main)
      (commons/-main)
      (vehicles/-main))))
