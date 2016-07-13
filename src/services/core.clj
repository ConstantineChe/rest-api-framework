(ns services.core
  (:require [users.server :as users]
            [common.server :as commons]
            [io.pedestal.http :as http]))

(defn -main
  "run services"
  [& [arg :as args]]
  (when (= "dev" arg)
    (users/run-dev {::http/port 8080})
    (commons/run-dev {::http/port 8081})))
