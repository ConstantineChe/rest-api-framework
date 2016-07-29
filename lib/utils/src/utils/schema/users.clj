(ns utils.schema.users
  (:require [schema.core :as s]))

(defn opt [key]
  (s/optional-key key))

(s/defschema User
  {(opt :name) s/Str
   (opt :surname) s/Str
   (opt :middlename) s/Str
   :email s/Str
   :password s/Str
   :registration_date s/Str
   (opt :gender) (s/enum "male" "female")
   (opt :phones) [s/Str]
   :status (s/enum "basic" "vip")
   (opt :dob) s/Str
   :enabled s/Bool})

(s/defschema UserWithId
  (merge User {:id s/Int}))

(s/defschema Vehicle
  {:make_id s/Int
   :model_id s/Int
   :year s/Int
   :modification_id s/Int
   :registration_number s/Str
   :vin_code s/Str
   :enabled s/Bool})

(s/defschema VehicleWithId
  (merge Vehicle {:id s/Int}))
