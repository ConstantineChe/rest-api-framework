(ns utils.schema.users
  (:require [schema.core :as s]))

(defn opt [key]
  (s/optional-key key))

(defn req [key]
  (s/required-key key))

(s/defschema InputUser
  {(req :name) s/Str
   (req :email) s/Str
   (opt :surname) s/Str
   (opt :middlename) s/Str
   (req :password) s/Str
   (req :password_conf) s/Str
   (opt :gender) (s/enum "male" "female")
   (opt :phones) [s/Str]
   (opt :dob) s/Str}
  )

(s/defschema User
  {(req :id) s/Int
   (req :name) s/Str
   (opt :surname) s/Str
   (opt :middlename) s/Str
   (req :email) s/Str
   (req :password) s/Str
   (req :registration_date) s/Str
   (opt :gender) (s/enum "male" "female")
   (opt :phones) [s/Str]
   (req :status) (s/enum "basic" "vip")
   (opt :dob) s/Str
   (req :enabled) s/Bool})

(s/defschema Vehicle
  {(req :id) s/Int
   (req :make_id) s/Int
   (req :model_id) s/Int
   (req :year) s/Int
   (req :modification_id) s/Int
   (req :registration_number) s/Str
   (req :vin_code) s/Str
   (req :enabled) s/Bool})

(s/defschema InputVehicle
  {(req :make_id) s/Int
   (req :model_id) s/Int
   (req :year) s/Int
   (req :modification_id) s/Int
   (req :registration_number) s/Str
   (req :vin_code) s/Str
   (req :enabled) s/Bool})

(s/defschema InputVehicleMake
  {(req :name) s/Str})

(s/defschema VehicleMake
  {(req :id) s/Int
   (req :name) s/Str
   (req :enabled) s/Bool})

(s/defschema InputVehicleModel
  {(req :name) s/Str
   (req :make_id) s/Int})

(s/defschema VehicleModel
  {(req :id) s/Int
   (req :name) s/Str
   (req :make_id) s/Int
   (req :enabled) s/Bool})

(s/defschema InputVehicleModification
  {(req :name) s/Str
   (req :made_from) s/Str
   (req :made_until) s/Str
   (req :midel_id) s/Int})

(s/defschema VehicleModification
  {(req :id) s/Int
   (req :name) s/Str
   (req :made_from) s/Str
   (req :made_until) s/Str
   (req :midel_id) s/Int
   (req :enabled) s/Str})
