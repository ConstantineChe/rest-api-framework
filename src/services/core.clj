(ns services.core
  (:require [users.server :as users]
            [common.server :as commons]
            [vehicles.server :as vehicles]
            [features.server :as features]
            [gallery.server :as gallery]
            [scheduler.core :as scheduler]
            [io.pedestal.http :as http])
  (:gen-class))

(defn -main
  "run services"
  [& [arg :as args]]
  (let [dev? (if (= "dev" arg) true)]
    (when dev?
      (users/run-dev {::http/port 8080})
      (commons/run-dev {::http/port 8081})
      (vehicles/run-dev {::http/port 8082})
      (features/run-dev {::http/port 8083})
      (gallery/run-dev {::http/port 8084})
      (scheduler/-main))
    (when-not dev?
      (users/-main)
      (commons/-main)
      (vehicles/-main)
      (scheduler/-main)
      (features/-main)
      (gallery/-main))))
