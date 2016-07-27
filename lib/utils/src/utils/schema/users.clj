(ns utils.schema.users
  (:require [schema.core :as s]))

(s/defschema User
  {:name s/Str
   :surname s/Str
   :middlename s/Str
   :email s/Str
   :password s/Str
   :registration_date s/Str
   :gender (s/enum "male" "female")
   :phones [s/Str]
   :status (s/enum "basic" "vip")
   :dob s/Str
   :enabled (s/enum s/Bool "true" "false")})

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
