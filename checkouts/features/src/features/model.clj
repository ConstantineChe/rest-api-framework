(ns features.model
  (:require [features.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]))



(def features
  (util/create-model
   {:entity `db/features
    :fields {:own #{:icon :for_garages :for_tirestations :for_carwashes :afst :ord}
             :language-fields #{}}}))
