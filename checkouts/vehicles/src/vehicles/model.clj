(ns vehicles.model
  (:require [vehicles.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [cheshire.core :as json]))



(def vehicle-makes
  {:entity `db/vehicle-makes
   :default-order [:name :ASC]
   :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
   :fields {:own #{:name}
            :joins {}
            :language-fields #{}
            :external {}}})

(def vehicle-models
  {:entity `db/vehicle-models
   :fks {:make_id :makes}
   :default-order [:name :ASC]
   :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
   :fields {:own #{:name :make_id}
            :joins {:makes #(util/select-fks vehicle-makes :make_id %)}
            :language-fields #{}
            :external {}}})

(def vehicle-modifications
  {:entity `db/vehicle-modifications
   :fks {:model_id :model}
   :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
   :default-order [:name :ASC]
   :fields {:own #{:name :made_from :made_until :model_id}
            :joins {:models #(util/select-fks vehicle-models :model_id %)}
            :language-fields #{}
            :external {}}})

(def vehicles
  {:entity `db/vehicles
   :fks {:make_id :makes
         :model_id :models
         :modification_id :modifications}
   :default-order [:id :ASC]
   :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
   :fields {:own #{:year :registration_number :make_id :model_id :modification_id}
            :joins {:makes #(util/select-fks vehicle-makes :make_id %)
                    :models #(util/select-fks vehicle-models :model_id %)}
            :language-fields #{}
            :externals {:modifications
                        {:topic "vehicles"
                         :from "vehicles"
                         :operation :include-modifications
                         :params {:ids #(set (map (fn [item]
                                                 (:modification_id item)) %))}}}}})
