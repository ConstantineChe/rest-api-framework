(ns vehicles.model
  (:require [vehicles.db :as db]
            [vehicles.kafka :as k]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [schema.core :as s]))

(defn select-ids [entity ids]
  (kc/select entity (kc/where {:id [in ids]})))

(defmacro build-select [entity query]
  `(kc/select ~entity
              ~@(filter identity
                        [(if (eval `(:fields ~query)) `(kc/fields ~@(eval `(:fields ~query))))
                         (if (eval `(:where ~query)) `(kc/where ~(eval `(:where ~query))))
                         (if (eval `(:offset ~query)) `(kc/offset ~(eval `(:offset ~query))))
                         (if (eval `(:limit ~query)) `(kc/limit ~(eval `(:limit ~query))))
                         (if (eval `(:order ~query)) `(kc/order ~@(eval `(:order ~query))))])))

(def vehicles
  {:entity 'db/vehicles
   :fields {:own [:year :registration_number :make_id :model_id]
            :joins {:makes #(select-ids db/vehicle-makes
                                                  (reduce (fn [item ids]
                                                            (conj ids (:make_id item)))
                                                          [] %))
                    :models #(select-ids db/vehicle-models
                                                   (reduce (fn [item ids]
                                                             (conj ids (:model_id item)))
                                                           [] %))}
            :language-fields #{}
            :external {:modifications
                       {:topic "vehicles"
                        :operation :modifications
                        :params {:ids #(reduce (fn [item ids]
                                                 (conj ids (:modification_id item)))
                                               [] %)}}}}})

;(build-select db/vehicles (:select (utils.model/parse-query vehicle-model {} "test")))

(def sel (:select (utils.model/parse-query vehicle-model
                                           {:filter {:id  [11 12 22 33]
                                                     :year 1999}
                                            :limit 5  :sort "-year"} "test")))

(clojure.pprint/pprint
 (utils.model/parse-query vehicles
                          {:filter {:id [1 2 3]} :limit 5 :sort "-year"} "test"))

(utils.model/execute-query "w"
                             vehicles
                             {:filter {:id [1 2 3]} :limit 5 :sort "-year"})

(macroexpand-1 '(utils.model/execute-query "w"
                             vehicles
                             {:filter {:id [1 2 3]} :limit 5 :sort "-year"}))

(comment (kc/select db/vehicles
             (kc/offset (:offset sel))
             (kc/limit (:limit sel)))


 (macroexpand-1 '(build-select (:entity vehicle-model) sel))

 (-> (kc/select* db/vehicles) (kc/where {:id [in [11 13]]}) (kc/select))

 (build-select db/vehicles sel))
