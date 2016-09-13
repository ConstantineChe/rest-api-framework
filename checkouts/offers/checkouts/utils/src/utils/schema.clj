(ns utils.schema
  (:require [schema.core :as s]))

(defmacro opt [key]
  `(s/optional-key ~key))

(defmacro req [key]
  `(s/required-key ~key))

(def lang-field
  {s/Keyword s/Str})

(defn api-response [entity includes]
  {:data [entity]
   (s/optional-key :included)
   (reduce-kv (fn [included k v]
                (merge included
                       {(s/optional-key k) [v]}))
              {} includes)})

(defmacro KafkaRequest [name params]
  `(s/defschema ~name
     {:type :request
      :operation s/Keyword
      :params params}))

(defmacro KafkaResponse [name data]
  `(s/defschema ~name
     {:type :response
      :data params}))
