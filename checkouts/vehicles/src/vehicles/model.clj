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
   :fields {:own #{:id :year :registration_number [:make_id :make] [:model_id :model]}
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
                           {:fields [:id :model :make :registration_number
                                      :vin_code :year]
                             :filter {:id (vec (range 10))}
                             :limit 5 :sort "-year"}
                           "test"))
;clojure.pprint/pprint
;macroexpand-1

(clojure.pprint/pprint
 (utils.model/execute-query "w"
                              vehicles
                              {:fields [:id :model :make :registration_number
                                        :vin_code :year]
                               :filter {:id (vec (range 10))}
                               :limit 5 :sort "-year"}))
