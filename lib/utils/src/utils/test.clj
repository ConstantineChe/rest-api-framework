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

(defn api-request
  ([service method route]
   (api-request method route nil {}))
  ([service method route body]
   (api-request method route body {}))
  ([service method route body headers]
   (case body
     nil (response-for service method route
                       :headers (merge headers {"Content-Type" "application/json"}))
     (response-for service method route
                   :body (json/generate-string body)
                   :headers (merge headers {"Content-Type" "application/json"}))
     )))
