(ns utils.schema.vehicles
  (:require [schema.core :as s]))

(defn req [key]
  (s/required-key key))

(defn opt [key]
  (s/required-key key))

(s/defschema Vehicle
  {(req :id) s/Int
   (opt :user_id) s/Int
   (req :make_id) s/Int
   (req :model_id) s/Int
   (req :year) s/Int
   (req :modification_id) s/Int
   (req :registration_number) s/Str
   (req :vin_code) s/Str
   (req :enabled) s/Bool})

(s/defschema InputVehicle
  {(req :user_id) s/Int
   (req :make_id) s/Int
   (req :model_id) s/Int
   (req :year) s/Int
   (req :modification_id) s/Int
   (req :registration_number) s/Str
   (req :vin_code) s/Str})

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
   (req :model_id) s/Int})

(s/defschema VehicleModification
  {(req :id) s/Int
   (req :name) s/Str
   (req :made_from) s/Str
   (req :made_until) s/Str
   (req :model_id) s/Int
   (req :enabled) s/Bool})
