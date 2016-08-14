(ns utils.model
  (:require [korma.core :as kc]
            [utils.kafka-service :as k]
            [schema.core :as s]
            [clojure.set :as set]))

(defn sort-query [field]
  (if (.startsWith field "-")
    [(.substring field 1) :DESC]
    [field :ASC])
  )

(defn build-select [entity query]
  `(kc/select ~entity
              ~@(filter identity
                        [(if (eval `(:fields ~query)) `(kc/fields ~@(eval `(:fields ~query))))
                         (if (eval `(:where ~query)) `(kc/where ~(eval `(:where ~query))))
                         (if (eval `(:offset ~query)) `(kc/offset ~(eval `(:offset ~query))))
                         (if (eval `(:limit ~query)) `(kc/limit ~(eval `(:limit ~query))))
                         (if (eval `(:order ~query)) `(kc/order ~@(eval `(:order ~query))))])))

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


(defn parse-query* [model query sid]
  (let [fields (:fields query)
        model-fields (:fields model)]
    {:select {:fields (vec (reduce (fn [fields field]
                                  (if ((:language-fields model-fields) field)
                                    (conj fields (kc/raw (str (name field) "->>'EN' AS "
                                                              (name field))))
                                    (conj fields field)))
                                []
                                (if fields
                                  (vec
                                   (filter
                                    (fn [fld]
                                      (if (vector? fld) ((set fields) (second fld))
                                          ((set fields) fld)))
                                    (vec (set/union (:language-fields model-fields)
                                                    (:own model-fields)))))
                                  (vec (set/union (:language-fields model-fields)
                                                  (:own model-fields))))))

              :order (if (:sort query) (sort-query (:sort query)) [:id :ASC])
              :where (if (:filter query)
                       (reduce-kv (fn [where k v]
                                    (merge where
                                           {k (if (sequential? v)
                                                [''in v]
                                                v)}))
                                  {}
                                  (:filter query)))
              :limit (if (:limit query) (:limit query))
              :offset (if (:offset query) (:offset query))}
     :joins (if fields (filter fields (:joins model-fields)) (:joins model-fields))
     :external (reduce-kv (fn [included key req]
                         (let [sid (keyword (str (:topic req) (name (:operation req))))
                               chan (k/get-chan! sid)]
                           (merge included {key {:chan chan
                                                 :sid sid}})))
                       {}
                       (if fields
                         (vec (filter (:external model-fields) fields))
                         (:external model-fields)))}))

(defn transform-entity [entity]
  {:id (:id entity)
   :attrs (dissoc entity :id)})


(defn execute-query* [service model query]
  (let [data (gensym "data")
        joins (gensym "joins")]
    `(let [~data ~(build-select (:entity model) (:select query))]
      (array-map :data (vec (map transform-entity ~data))
                 :included (reduce-kv (fn [joins# k# v#]
                                        (merge joins#
                                               {k# (v# ~data)}))
                                      {} ~(eval `(:joins (:fields ~model))))))))

(defmacro execute-query [service model query]
  (eval `(execute-query* ~service ~model (parse-query* ~model ~query "sid"))))
