(ns utils.schema.common
  (:require [schema.core :as s]))

(s/defschema Settings
  {:hello s/Str
   (s/optional-key :name) s/Str})
