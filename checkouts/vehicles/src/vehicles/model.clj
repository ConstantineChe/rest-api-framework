(ns vehicles.model
  (:require [vehicles.db :as db]
            [vehicles.kafka :as k]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]))

(defn select-ids [entity ids]
  (kc/select entity (kc/where {:id [in ids]})))

(defmacro build-select [entity query]
  `(kc/select ~entity
              ~@(filter identity
                        [(if (eval `(:fields ~query)) `(kc/fields ~@(eval `(:fields ~query))))
                         (if (eval `(:where ~query)) `(kc/where ~(eval `(:where ~query))))
                         (if (eval `(:fields ~query)) `(kc/offset ~(eval `(:offset ~query))))
                         (if (eval `(:fields ~query)) `(kc/limit ~(eval `(:limit ~query))))
                         (if (eval `(:fields ~query)) `(kc/order ~@(eval `(:order ~query))))])))

(def vehicle-model
  {:query (fn [select] (build-select db/vehicles select))
   :fields {:own [:year :registration_number]
            :joins {:make {:key :make_id
                           :select #(select-ids db/vehicle-makes
                                         (reduce (fn [item ids]
                                                   (conj ids (:make_id item)))
                                                 [] %))}
                    :model {:key :model_id
                            :select #(select-ids db/vehicle-models
                                          (reduce (fn [item ids]
                                                    (conj ids (:model_id item)))
                                                  [] %))}}
            :language-fields #{}
            :external {:example-include {:topic "vehicles"
                                         :operation :modifications
                                         :params {:ids #(reduce (fn [item ids]
                                                                  (conj ids (:modification_id item)))
                                                                [] %)}}}}})

(build-select db/vehicles (:select (utils.model/parse-query vehicle-model {} "test")))

(def sel (:select (utils.model/parse-query vehicle-model {:limit "5" :offset "10" :sort "-year"} "test")))

(:select (utils.model/parse-query vehicle-model {:limit "5" :sort "-year"} "test"))

(kc/select db/vehicles
           (kc/offset (:offset sel))
           (kc/limit (:limit sel)))

(apply str (interpose ", " (:fields sel)))
(macroexpand-1 '(build-select db/vehicles sel))
