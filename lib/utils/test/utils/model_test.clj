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

(def test-include-model (create-model {:entity `test-include
                                       :fields {:own #{:name :data_include}
                                                :language-fields #{}}}))

(def test-external-include-model (create-model {:entity `test-external-include
                                                :fields {:own #{:name :data_external}
                                                         :language-fields #{}}}))


(def test-model (create-model {:entity `test-entity
                               :fields {:own #{:name :data_1}
                                        :language-fields #{:lang}}}))

(def test-model-with-includes (create-model {:entity `test-entity
                                             :fks {:include_id :include}
                                             :fields {:own #{:name :data_1 :include_id}
                                                      :joins {:include {:fk :include_id
                                                                             :model test-include-model}}
                                                      :language-fields #{}}}))
(def test-model-all-includes (create-model {:entity `test-entity
                                            :fks {:include_id :include :external_include_id :external_include}
                                            :fields {:own #{:name :data_1 :include_id :external_include_id}
                                                     :joins {:include {:fk :include_id
                                                                            :model test-include-model}}
                                                     :language-fields #{}
                                                     :externals {:external_include
                                                                {:topic "model_external_test"
                                                                 :from "model_test"
                                                                 :operation :include-externals
                                                                 :params {:filter
                                                                          (fn [data]
                                                                            {:id (set (map (fn [i]
                                                                                             (:external_include_id i)) data))
                                                                             })}}}}}))

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

(defmethod process-request :include-externals [{:keys [message uid] :as msg}]
  (service/response! kafka-external msg (.GET test-external-include-model kafka-external {:query-params (:params message)} false)))

(defmethod process-request :default [msg]
  (println "Invalid request operation: " (-> msg :message :operation)))


(use-fixtures :once schema.test/validate-schemas)

(facts "About data models"
       (against-background [(before :facts (do (.start kafka)
                                               (.start kafka-external)
                                               (repl/migrate config)))
                            (after :facts (do (.stop kafka)
                                              (.stop kafka-external)
                                              (repl/rollback config (-> config :migrations count))))]
         (fact "Model can request includes from another services."
               (.GET test-model-all-includes kafka {:query-params {}} true)
               => {:data [{:id 1
                           :attrs {:data_1 "test_data_1" :include_id 1 :external_include_id 1 :name "test_n_1"}}
                          {:id 2
                           :attrs {:data_1 "test_data_2" :include_id 2 :external_include_id 2 :name "test_n_2"}}
                          {:id 3
                           :attrs {:data_1 "test_data_3" :include_id 3 :external_include_id 3 :name "test_n_3"}}
                          {:id 4
                           :attrs {:data_1 "test_data_4" :include_id 4 :external_include_id 4 :name "test_n_4"}}]
                   :included {:include [{:id 1
                                         :attrs {:data_include "include_data_1" :name "include_1"}}
                                        {:id 2
                                         :attrs {:data_include "include_data_2" :name "include_2"}}
                                        {:id 3
                                         :attrs {:data_include "include_data_3" :name "include_3"}}
                                        {:id 4
                                         :attrs {:data_include "include_data_4" :name "include_4"}}]
                              :external_include [{:id 1
                                                  :attrs
                                                  {:data_external "external_include_data_1" :name "external_include_1"}}
                                                 {:id 2
                                                  :attrs
                                                  {:data_external "external_include_data_2" :name "external_include_2"}}
                                                 {:id 3
                                                  :attrs
                                                  {:data_external "external_include_data_3" :name "external_include_3"}}
                                                 {:id 4
                                                  :attrs
                                                  {:data_external "external_include_data_4" :name "external_include_4"}}]}})

         (fact "it is possible to request single entity fields from model"

               (.GET test-model kafka {:query-params {:filter {:ids (json/generate-string [1 2 3])}}} true)
               => {:data [{:id 1
                           :attrs
                           {:data_1 "test_data_1" :name "test_n_1" :lang "language_field_1"}}
                          {:id 2
                           :attrs
                           {:data_1 "test_data_2" :name "test_n_2" :lang "language_field_2"}}
                          {:id 3
                           :attrs
                           {:data_1 "test_data_3" :name "test_n_3" :lang "language_field_3"}}]}

               (.GET test-model kafka {:query-params {:filter {:ids (json/generate-string [1 3])}
                                                             :fields "name"}} true)
               => {:data [{:id 1
                           :attrs
                           {:name "test_n_1"}}
                          {:id 3
                           :attrs
                           {:name "test_n_3"}}]}


               (.GET test-model kafka {:query-params {:filter {:ids (json/generate-string [1 3])}
                                                             :fields "name"
                                                             :sort "-id"}} true)
               => {:data [{:id 3
                           :attrs
                           {:name "test_n_3"}}
                          {:id 1
                           :attrs
                           {:name "test_n_1"}}]}

               (.GET test-model kafka {:query-params {:fields "name,lang"
                                                             :limit 2
                                                             :sort "-id"}
                                              :lang "RU"} true)
               => {:data [{:id 4
                           :attrs
                           {:name "test_n_4" :lang "языковое_поле_4"}}
                          {:id 3
                           :attrs
                           {:name "test_n_3" :lang "языковое_поле_3"}}]})

         (fact "Model can join another models."

          (.GET test-model-with-includes kafka {:query-params {}} true)
          => {:data [{:id 1
                      :attrs {:data_1 "test_data_1" :include_id 1 :name "test_n_1"}}
                     {:id 2
                      :attrs {:data_1 "test_data_2" :include_id 2 :name "test_n_2"}}
                     {:id 3
                      :attrs {:data_1 "test_data_3" :include_id 3 :name "test_n_3"}}
                     {:id 4
                      :attrs {:data_1 "test_data_4" :include_id 4 :name "test_n_4"}}]
              :included {:include [{:id 1
                                    :attrs {:data_include "include_data_1" :name "include_1"}}
                                   {:id 2
                                    :attrs {:data_include "include_data_2" :name "include_2"}}
                                   {:id 3
                                    :attrs {:data_include "include_data_3" :name "include_3"}}
                                   {:id 4
                                    :attrs {:data_include "include_data_4" :name "include_4"}}]}})
))
