(ns scheduler.core
  (:require [clojurewerkz.quartzite.scheduler :as qs]
             [clojurewerkz.quartzite.triggers :as t]
             [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
             [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
             [clj-time.local :as l]
             [clj-time.core :as time]
             [io.pedestal.log :as log]))


(declare do-job done)

(defjob Jobs
  [ctx]
  (let [current-time (l/local-now)
        schedules get-schedules]
    (dorun (for [job schedules]
             (try (do (do-job job)
                      (done job))
                  (catch Exception e (log/error "Error doing job" "\n" (.getMessage e))))))))

(defn init-scheduler!
  "Initialize scheduler job"
  []
  (let [s   (-> (qs/initialize) qs/start)
        job (j/build
             (j/of-type Jobs)
             (j/with-identity (j/key "jobs.common.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (cron-schedule "0 */5 * ? * *"))))]
    (qs/schedule s job trigger))
  )
