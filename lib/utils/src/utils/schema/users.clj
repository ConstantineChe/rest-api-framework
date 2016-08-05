(ns utils.schema.users
  (:require [schema.core :as s]
            [utils.schema :refer [opt req]]))

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
   (opt :surname) (s/maybe s/Str)
   (opt :middlename) (s/maybe s/Str)
   (req :email) s/Str
   (req :password) s/Str
   (req :registration_date) s/Str
   (opt :gender) (s/maybe (s/enum "male" "female"))
   (opt :phones) (s/maybe [s/Str])
   (req :status) (s/enum "basic" "vip")
   (opt :dob) (s/maybe s/Str)
   (req :enabled) s/Bool})

(s/defschema UserWithoutPassword
  (dissoc User :password))
