(ns utils.model
  (:require [korma.core :as kc]
            [utils.kafka-service :as k]))

(defn sort-query [field]
  (if (.startsWith field "-")
    [(.substring field 1) :DESC]
    [field :ASC])
  )

(comment
  (reduce (fn [[key req] included]
                       (let [sid (keyword (str (:topic req) (name (:operation req))))
                             chan (k/get-chan! sid)]
                         (k/send-msg! (:kafka service) (:topic req)
                                      {:type :request
                                       :from (:name service)
                                       :operation (:operation req)})
                         (merge included {key chan})))
                     (filter (:includes model) {:fields query})))

(defn parse-query [model query sid]
  (let [fields (:fields query)
        model-fields (:fields model)]
    {:select {:fields (if fields (filter (:own model-fields) fields) (:own model-fields))
              :order (if (:sort query) (sort-query (:sort query)) [:id :ASC])
              :where (if (:filter query) (:filter query))
              :limit (if (:limit query) (Integer. (:limit query)) 0)
              :offset (if (:offset query) (Integer. (:offset query)) 0)}
     :joins (if fields (filter (:joins model-fields) fields) (:joins model-fields))
     :external (reduce-kv (fn [included key req]
                         (let [sid (keyword (str (:topic req) (name (:operation req))))
                               chan (k/get-chan! sid)]
                           (merge included {key {:chan chan
                                                 :sid sid}})))
                       {}
                       (if fields
                         (filter (:external model-fields) fields)
                         (:external model-fields)))}))

(defn execute-query* [service model query]
  (let [data ((:query model) (:select query))]))

(defn transform-entity [entity]
  {:id (:id entity)
   :attrs (dissoc entity :id)})

(defn transform-response [mdoel response])
