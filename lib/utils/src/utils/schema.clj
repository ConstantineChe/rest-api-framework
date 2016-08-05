(ns utils.schema
  (:require [schema.core :as s]))

(defn opt [key]
  (s/optional-key key))

(defn req [key]
  (s/required-key key))

(defmacro KafkaRequest [name params]
  `(s/defschema ~name
     {:type :request
      :operation s/Keyword
      :params params}))

(defmacro KafkaResponse [name data]
  `(s/defschema ~name
     {:type :response
      :data params}))
