(ns scheduler.core
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [scheduler.db :as db]
            [clj-time.local :as l]
            [clj-time.core :as time]
            [io.pedestal.log :as log]))


(defn do-job [job]
  (prn (:data job)))

(defjob Jobs
  [ctx]
  (doseq [job (db/get-jobs)]
    (do (do-job job)
        (prn "kron triggered")
        (db/mark-as-executed (:id job)))))

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

(init-scheduler!)
