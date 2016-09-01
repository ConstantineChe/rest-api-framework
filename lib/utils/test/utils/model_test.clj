(ns utils.model-test
  (:require [clojure.test :refer :all]
            [utils.test :refer :all]
            [midje.sweet :refer :all]
            [utils.model :refer :all :as m]
            [utils.db :as db]
            [korma.db :as kdb]
            [utils.db :as dbu]
            [ragtime.repl :as repl]
            [cheshire.core :as json]
            [korma.core :as kc :refer [select insert defentity]]
            [clojure.core.async :as async]
            [utils.kafka-service :as service]
            [schema.test]
            [environ.core :refer [env]]))

(def connection (db/db-connection {:db (str (:db env  "carbook") "_test")
                                   :username (:db-user env)
                                   :password (:db-password env)}))

(kdb/defdb db connection)

(def config (dbu/load-config connection))

(defentity test-entity
  (kc/table "test_tbl"))

(defentity test-include
  (kc/table "test_include_tbl"))

(defentity test-external-include
  (kc/table "test_external_include_tbl"))

(def test-model (create-model {:entity `test-entity
                               :fields {:own #{:name :data_1}
                                        :language-fields #{}}}))

(defmulti process-request (comp :operation :message))

(def kafka (service/kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "model_test"
                                             :auto.offset.reset       :earliest
                                             :enable.auto.commit      true}
                           :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                            :client.id "model_test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :model_test :partition 0}]
                           :handler process-request}))

(def kafka-external (service/kafka {:consumer-config {:bootstrap.servers       [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :group.id                "model_external_test"
                                             :auto.offset.reset       :earliest
                                             :enable.auto.commit      true}
                           :producer-config {:bootstrap.servers [(:kafka-bootstrap-servers env "localhost:9091")]
                                             :client.id "model_external_test"}
                           :producer-chan (async/chan)
                           :subscriptions [{:topic :model_external_test :partition 0}]
                           :handler process-request}))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(use-fixtures :once schema.test/validate-schemas)
(use-fixtures :once (fn [_] (.start kafka)))

(facts "About data models"
       (against-background [(before :facts (do (.start kafka)
                                               (repl/migrate config)))
                            (after :facts (do (.stop kafka)
                                              (repl/rollback config (-> config :migrations count))))]
         (fact "it is possible to request single entity fields from model"
               (.fetch-data test-model kafka {:query-params {:filter {:ids (json/generate-string [1 2 3])}}} true)
               => {:data [{:id 1
                           :attrs
                           {:data_1 "test_data_1" :name "test_n_1"}}
                          {:id 2
                           :attrs
                           {:data_1 "test_data_2" :name "test_n_2"}}
                          {:id 3
                           :attrs
                           {:data_1 "test_data_3" :name "test_n_3"}}]}

               (.fetch-data test-model kafka {:query-params {:filter {:ids (json/generate-string [1 3])}
                                                             :fields "name"}} true)
               => {:data [{:id 1
                           :attrs
                           {:name "test_n_1"}}
                          {:id 3
                           :attrs
                           {:name "test_n_3"}}]}


               (.fetch-data test-model kafka {:query-params {:filter {:ids (json/generate-string [1 3])}
                                                             :fields "name"
                                                             :sort "-id"}} true)
               => {:data [{:id 3
                           :attrs
                           {:name "test_n_3"}}
                          {:id 1
                           :attrs
                           {:name "test_n_1"}}]}

               (.fetch-data test-model kafka {:query-params {:fields "name"
                                                             :limit 2
                                                             :sort "-id"}} true)
               => {:data [{:id 4
                           :attrs
                           {:name "test_n_4"}}
                          {:id 3
                           :attrs
                           {:name "test_n_3"}}]})))
