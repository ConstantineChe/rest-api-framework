(ns utils.test
  (:require  [clojure.java.jdbc :as sql]
             [ragtime.repl :as repl]
             [io.pedestal.test :refer :all]
             [cheshire.core :as json]))

(defn exec-raw [connection]
  (fn [query]
    (sql/with-db-connection [conn connection]
      (with-open [s (.createStatement (:connection conn))]
        (.executeUpdate s query)))))

(defn init-db! [config]
  (fn [tests]
   (repl/migrate config)
   (try (tests)
        (finally (repl/rollback config (-> config :migrations count))))))

(defn clear-tables! [connection tables]
  (fn [test]
    (try (test)
         (finally (do (println "clear tables")
                      (dorun (map #((exec-raw connection) (str "TRUNCATE TABLE " %)) tables)))))))
