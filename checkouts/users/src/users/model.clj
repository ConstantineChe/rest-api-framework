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
   :fields {:own #{:name :surname :middlename :email
                   :registration_date :gender :phones :status :dob}
            :joins {}
            :language-fields #{}
            :external {}}})

(def my-vehicles
  {:data (fn [request] [{:id -1 :vehicles [1 2 3 4 5]}])
   :fields {:externals {:vehicles
                        {:topic "vehicles"
                         :from "users"
                         :cache "vehicles"
                         :operation :include-vehicles
                         :params {:filter (fn [data] {:id (-> data first :vehicles)})
                                  :sort "id"}}
                        :modifications
                        {:topic "vehicles"
                         :from "users"
                         :cache "modifications"
                         :operation :include-modifications
                         :params {:filter (fn [data] {:id (-> data first :vehicles)})}}
                       :models
                       {:topic "vehicles"
                        :from "users"
                        :cache "models"
                        :operation :include-models
                        :params {:filter (fn [data] {:id (-> data first :vehicles)})}}
                       :makes
                       {:topic "vehicles"
                        :from "users"
                        :cache "makes"
                        :operation :include-makes
                        :params {:filter (fn [data] {:id (-> data first :vehicles)})}}}}})
