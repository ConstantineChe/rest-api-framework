(ns scheduler.core
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [scheduler.db :as db]
            [utils.kafka-service :refer [send-msg!]]
            [scheduler.kafka :as k]
            [clj-time.local :as l]
            [clj-time.core :as time]
            [io.pedestal.log :as log]))


(defn do-job [job]
  (send-msg! k/kafka-component "scheduler" "scheduler" {:type :request :params (.getValue (:data job))}))

(defjob Jobs
  [ctx]
  (log/info :msg "job triggered")
  (let [jobs (db/get-jobs)]
    (log/info :msg "jobs fetched from db: " :jobs jobs)
    (doseq [job jobs]
      (do (log/info :msg "doing job: " :job job)
          (do-job job)
          (log/info :msg "done")
          (db/mark-as-executed (:id job))))))


(defrecord Scheduler [jobs cron-expr scheduler]
  component/Lifecycle

  (start [component]
    (println "Starting scheduler...")
    (let [s (-> (qs/initialize) qs/start)
          job (j/build
               (j/of-type jobs)
               (j/with-identity (j/key "jobs.common.1")))
          trigger (t/build
                   (t/with-identity (t/key "triggers.1"))
                   (t/start-now)
                   (t/with-schedule (schedule
                                     (cron-schedule cron-expr))))]
      (qs/schedule s job trigger)
      (assoc component :scheduler s)))

  (stop [component]
    (println "Stopping scheduler...")
    (qs/shutdown scheduler)
    (assoc component :scheduler nil)))

(def scheduler-component
  (map->Scheduler {:jobs Jobs :cron-expr (:cron-expr env "*/30 * * ? * *")}))

(defn -main [& args]
  (.start k/kafka-component)
  (.start scheduler-component))
