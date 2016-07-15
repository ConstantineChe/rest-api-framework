(ns common.db)

(defn get-settings
  ([] {:hello "Hello2"})
  ([name] {:hello "Hello2"
           :name name}))
