(ns gallery.service-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [utils.test :refer [init-db! clear-tables!]]
            [ragtime.repl :as repl]
            [gallery.db :as db]
            [utils.db :as dbu]
            [cheshire.core :as json]
            [gallery.service :as service]
            [environ.core :refer [env]]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(def config (dbu/load-config db/db-connection))

(def tables ["gallery_tbl"])

(defn api-request
  ([method route]
   (api-request method route nil {}))
  ([method route body]
   (api-request method route body {}))
  ([method route body headers]
   (case body
     nil (response-for service method route
                       :headers (merge headers {"Content-Type" "application/json"}))
     (response-for service method route
                   :body (json/generate-string body)
                   :headers (merge headers {"Content-Type" "application/json"}))
     )))

(use-fixtures :once (init-db! config))
(use-fixtures :each (clear-tables! db/db tables))



(with-state-changes [(before :facts (repl/migrate config))
                     (after :facts (repl/rollback config (-> config :migrations count)))]
  (facts "about gallery service"

         (fact "TODO"
               nil => nil)))
