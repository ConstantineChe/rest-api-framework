(ns utils.schema
  (:require [schema.core :as s]))

(defmacro KafkaRequest [name params]
  `(s/defschema ~name
     {:type :request
      :operation s/Keyword
      :params params}))

(defmacro KafkaResponse [name data]
  `(s/defschema ~name
     {:type :response
      :data params}))
