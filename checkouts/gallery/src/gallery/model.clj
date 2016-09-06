(ns gallery.model
  (:require [gallery.db :as db]
            [utils.schema.vehicles :as vs]
            [utils.model :as util]
            [korma.core :as kc]
            [cheshire.core :as json]
            [io.pedestal.log :as log]
            [clojure.core.async :as async]))



(def gallery
  (util/create-model
   {:entity `db/gallery
    :fields {:own #{:business_id :caption :src :thumbnails :ord}
             :language-fields #{}}}))
