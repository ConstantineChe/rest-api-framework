(ns utils.schema
  (:require [schema.core :as s]))

(s/defschema KafkaMessage
  {:type s/Keyword})
