(ns users.model
  (:require [users.db :as db]
            [utils.schema.users :as us]
            [utils.model :as util]
            [schema.core :as s]
            [cheshire.core :as json]))

(def users
  (util/create-model
   {:entity `db/users
    :default-order [:id :ASC]
    :map-params {[:filter :ids] [[:filter :id] (fn [ids] (vec (json/parse-string ids)))]
                 [:fields] [[:fields] (fn [fields] (util/string->array fields keyword))]}
    :fields {:own #{:name :surname :middlename :email
                    :registration_date :gender :phones :status :dob}
             :joins {}
             :language-fields #{}
             :external {}}}))

(def my-vehicles
  (util/create-model
   {:data (fn [request] [{:id -1 :vehicles [2 3 4 5]}])
    :fields {:externals {:vehicles
                         {:topic "vehicles"
                          :from "users"
                          :cache {:tag "vehicles" :exp (* 5 60)}
                          :with-includes? true
                          :operation :include-vehicles
                          :params {:filter (fn [data] {:id (-> data first :vehicles)})
                                   :sort "id"}}
                         }}}))
