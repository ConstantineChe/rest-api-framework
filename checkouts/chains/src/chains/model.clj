(ns chains.model
  (:require [chains.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]
            [schema.core :as s]))



(def chains
  (util/create-model
   {:entity `db/chains
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
