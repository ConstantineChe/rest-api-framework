(ns users.service-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [clojure.java.jdbc :as sql]
            [users.db :as db]
            [utils.db :as dbu]
            [users.service :as service]
            [environ.core :as env]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(def test-db-connection (dbu/db-connection {:db (str (or (:db env) (:users-db env)) "_test")
                                            :user (:db-user env)
                                            :password (:db-password env)}))

(def config (db/load-config test-db-connection))

(def tables ["users_tbl" "vehicles_tbl" "vehicle_makes_tbl" "vehicle_models_tbl" "vehicle_modifications_tbl"])

(defn exec-raw [query]
  (sql/with-db-connection [conn db/db-connection]
         (with-open [s (.createStatement (:connection conn))]
           (.executeUpdate s query))))

(defn init-db! [tests]
  (try (exec-raw (str "CREATE DATABASE " (:db env) "_test"))
       (catch PSQLException e
         nil))
  (kdb/defdb test-db test-db-connection)
  (repl/migrate config)
  (try (tests)
       (finally (repl/rollback config (-> config :migrations count)))))

(defn clear-tables! [test]
  (try (test)
       (finally (dorun (map #(exec-raw (str "TRUNCATE TABLE " %) tables))))))

(use-fixtures :once init-db!)
(use-fixtures :each clear-tables!)

(deftest home-page-test
  (is (=
       (:body (response-for service :get "/"))
       "Hello World!"))
  (is (=
       (:headers (response-for service :get "/"))
       {"Content-Type" "text/html;charset=UTF-8"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"})))


(deftest about-page-test
  (is (.contains
       (:body (response-for service :get "/about"))
       "Clojure 1.8"))
  (is (=
       (:headers (response-for service :get "/about"))
       {"Content-Type" "text/html;charset=UTF-8"
        "Strict-Transport-Security" "max-age=31536000; includeSubdomains"
        "X-Frame-Options" "DENY"
        "X-Content-Type-Options" "nosniff"
        "X-XSS-Protection" "1; mode=block"})))
