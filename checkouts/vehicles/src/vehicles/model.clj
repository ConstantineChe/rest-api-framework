(ns vehicles.model
  (:require [vehicles.db :as db]
            [vehicles.kafka :as k]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [schema.core :as s]))

(defn select-ids [entity ids]
  (vec (map utils.model/transform-entity
         (kc/select entity (kc/where {:id [in ids]})))))

(def vehicles
  {:entity 'db/vehicles
   :fields {:own #{:year :registration_number [:make_id :make] [:model_id :model]}
            :joins {:makes #(select-ids db/vehicle-makes
                                                  (reduce (fn [ids item]
                                                            (conj ids (:make item)))
                                                          [] %))
                    :models #(select-ids db/vehicle-models
                                                   (reduce (fn [ids item]
                                                             (conj ids (:model item)))
                                                           [] %))}
            :language-fields #{}
            :external {:modifications
                       {:topic "vehicles"
                        :operation :modifications
                        :params {:ids #(reduce (fn [item ids]
                                                 (conj ids (:modification_id item)))
                                               [] %)}}}}})

(clojure.pprint/pprint
 (utils.model/parse-query* vehicles
                          {:filter {:id [1 2 3]} :limit 5 :sort "-year"} "test"))
;clojure.pprint/pprint
(macroexpand-1
 '(utils.model/execute-query "w"
                            vehicles
                            {:filter {:id [1 2 3]} :fields [:year :enabled] :limit 5 :sort "-year"}))
