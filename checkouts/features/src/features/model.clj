(ns features.model
  (:require [features.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [schema.core :as s]
            [korma.core :as kc]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]))



(def features
  (util/create-model
   {:entity `db/features
    :map-params {[:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]
                 [:ids] [[:filter :id] (fn [ids]
                                         (if (string? ids)
                                           (vec (json/parse-string ids)) ids))]}
    :fields {:own #{[:ftr_for_garages :for_garages] [:ftr_for_tire_stations :for_tirestations] [:ftr_type :icon]
                    [:ftr_for_car_washes :for_carwashes] [:ftr_category :category] [:ftr_order :ord]}
             :language-fields #{[:ftr_name :caption] [:ftr_description :description]}}
    :entity-schema {:id s/Int
                    :attrs
                    {(s/optional-key :caption) s/Str
                     (s/optional-key :description) s/Str
                     (s/optional-key :category) s/Str
                     (s/optional-key :for_garages) s/Bool
                     (s/optional-key :for_tirestations) s/Bool
                     (s/optional-key :for_carwashes) s/Bool
                     (s/optional-key :ord) s/Int}}
    :get-params {(s/optional-key :fields) s/Str
                 (s/optional-key :ids) [s/Int]
                 (s/optional-key (keyword "filter[id]")) s/Int
                 (s/optional-key (keyword "filter[category]")) s/Str
                 (s/optional-key (keyword "filter[for_garages]")) s/Bool
                 (s/optional-key (keyword "filter[for_tirestations]")) s/Bool
                 (s/optional-key (keyword "filter[for_carwashes]")) s/Bool
                 (s/optional-key :sort) s/Str
                 (s/optional-key :offset) s/Int
                 (s/optional-key :limit) s/Int}}))
