(ns utils.schema.users
  (:require [schema.core :as s]))

(s/defschema User
  {:name s/Str
   :email s/Str
   :token s/Str})
