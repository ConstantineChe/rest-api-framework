(ns businesses.model
  (:require [businesses.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [schema.core :as s]))



(def businesses
  (util/create-model
   {:entity `db/businesses
    :map-params {[:filter :id] [[:filter :glr_id_pk] identity]
                 [:filter :business_id] [[:filter :glr_bsn_id_fk] identity]
                 [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
    :fields {:own #{[:glr_bsn_id_fk :business_id] ["glr_image->>'src'" :src]
                    ["glr_image->>'thumbnails'" :thumbnails]
                    [:glr_order :ord]}
             :language-fields #{[:glr_name :caption]}}
    :entity-schema {:id s/Int
                    :attrs
                    {:business_id s/Int
                     (s/optional-key :caption) s/Str
                     (s/optional-key :src) s/Str
                     (s/optional-key :thumbnails) s/Any
                     (s/optional-key :ord) s/Int}}
    :get-params {(s/optional-key :fields) s/Str
                 (s/optional-key (keyword "filter[id]")) s/Int
                 (keyword "filter[business_id]") s/Int
                 (s/optional-key :sort) s/Str
                 (s/optional-key :offset) s/Int
                 (s/optional-key :limit) s/Int}}))

(def total
  (util/create-model
   {:entity `db/businesses
    :fields {:aggregate #{["count(bsn_id_pk)" :coutBusinesses]
                          ["count(case bsn_type when 'garages' 1 end" :countGarages]
                          ["count(case bsn_type when 'carwashes' 1 end" :countCarwashes]
                          ["count(case bsn_type when 'tirestations' 1 end" :countTirestations]}}
    :entity-schema {:id s/Int
                    :attrs
                    {:business_id s/Int
                     (s/optional-key :countBusinesses) s/Int
                     (s/optional-key :countGarages) s/Int
                     (s/optional-key :countCarwashes) s/Int
                     (s/optional-key :countTirestations) s/Int}}
    :get-params {(s/optional-key :fields) s/Str}}))
