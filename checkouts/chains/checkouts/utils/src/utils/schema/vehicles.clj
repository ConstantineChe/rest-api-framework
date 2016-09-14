(ns utils.schema.vehicles
  (:require [schema.core :as s]))



(s/defschema Vehicle
  {(s/required-key :id) s/Int
   (s/optional-key :user_id) s/Int
   (s/required-key :make_id) s/Int
   (s/required-key :model_id) s/Int
   (s/required-key :year) s/Int
   (s/required-key :modification_id) s/Int
   (s/required-key :registration_number) s/Str
   (s/required-key :vin_code) s/Str
   (s/required-key :enabled) s/Bool})

(s/defschema InputVehicle
  {(s/required-key :user_id) s/Int
   (s/required-key :make_id) s/Int
   (s/required-key :model_id) s/Int
   (s/required-key :year) s/Int
   (s/required-key :modification_id) s/Int
   (s/required-key :registration_number) s/Str
   (s/required-key :vin_code) s/Str})

(s/defschema InputVehicleMake
  {(s/required-key :name) s/Str})

(s/defschema VehicleMake
  {(s/required-key :id) s/Int
   (s/required-key :name) s/Str
   (s/required-key :enabled) s/Bool})

(s/defschema InputVehicleModel
  {(s/required-key :name) s/Str
   (s/required-key :make_id) s/Int})

(s/defschema VehicleModel
  {(s/required-key :id) s/Int
   (s/required-key :name) s/Str
   (s/required-key :make_id) s/Int
   (s/required-key :enabled) s/Bool})

(s/defschema InputVehicleModification
  {(s/required-key :name) s/Str
   (s/required-key :made_from) s/Str
   (s/required-key :made_until) s/Str
   (s/required-key :model_id) s/Int})

(s/defschema VehicleModification
  {(s/required-key :id) s/Int
   (s/required-key :name) s/Str
   (s/required-key :made_from) s/Str
   (s/required-key :made_until) s/Str
   (s/required-key :model_id) s/Int
   (s/required-key :enabled) s/Bool})

(s/defschema VehicleOutput
  {(s/required-key :id) s/Int
   (s/required-key :attrs)
   {(s/optional-key :user_id) s/Int
    (s/optional-key :make_id) s/Int
    (s/optional-key :model_id) s/Int
    (s/optional-key :year) s/Int
    (s/optional-key :modification_id) s/Int
    (s/optional-key :registration_number) s/Str
    (s/optional-key :vin_code) s/Str
    (s/optional-key :enabled) s/Bool}})

(s/defschema VehicleMakeOutput
  {(s/required-key :id) s/Int
   (s/required-key :attrs)
   {(s/optional-key :name) s/Str
    (s/optional-key :enabled) s/Bool}})

(s/defschema VehicleModelOutput
  {(s/required-key :id) s/Int
   (s/required-key :attrs)
   {(s/optional-key :name) s/Str
    (s/optional-key :make_id) s/Int
    (s/optional-key :enabled) s/Bool}})

(s/defschema VehicleModificationOutput
  {(s/required-key :id) s/Int
   (s/required-key :attrs)
   {(s/optional-key :name) s/Str
    (s/optional-key :made_from) java.util.Date
    (s/optional-key :made_until) java.util.Date
    (s/optional-key :model_id) s/Int}})
