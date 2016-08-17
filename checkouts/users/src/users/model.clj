(ns users.model
  (:require [users.db :as db]
            [utils.schema.users :as us]
            [utils.model :as util]
            [schema.core :as s]
            [cheshire.core :as json]))

(def users
  {:entity `db/users
   :default-order [:id :ASC]
   :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
   :fields {:own #{:name :surname :middlename :email :registration_date :gender :phones :status :dob}
            :joins {}
            :language-fields #{}
            :external {}}})
