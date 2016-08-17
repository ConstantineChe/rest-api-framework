(ns utils.model
  (:require [korma.core :as kc]
            [utils.kafka-service :as k]
            [schema.core :as s]
            [clojure.set :as set]
            [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [<!! >! >!!]]))

(defn sort-query [field]
  (if (.startsWith field "-")
    [(keyword (.substring field 1)) :DESC]
    [(keyword field) :ASC])
  )

(defn build-select [entity query]
  (eval `(kc/select ~entity
                ~@(filter identity
                          [(if (eval `(:fields ~query)) `(kc/fields ~@(eval `(:fields ~query))))
                           (if (eval `(:where ~query)) `(kc/where ~(eval `(:where ~query))))
                           (if (eval `(:offset ~query)) `(kc/offset ~(eval `(:offset ~query))))
                           (if (eval `(:limit ~query)) `(kc/limit ~(eval `(:limit ~query))))
                           (if (eval `(:order ~query)) `(kc/order ~@(eval `(:order ~query))))]))))


(defn transform-entity [entity]
  (if (map? entity)
    {:id (:id entity)
     :attrs (dissoc entity :id)}
    (into {} entity)))

(defn normalize-query [query query-map]
  (reduce-kv (fn [query key [new-key f]]
               (let [val (get-in query key)]
                 (if val
                   (-> (update-in query (butlast key) dissoc (last key))
                       (assoc-in new-key (f val)))
                   query)))
             query query-map))

(defn parse-query [model query sid]
  (let [query (normalize-query query (:map-params model))
        fields (:fields query)
        model-fields (:fields model)
        external-fields (:externals model-fields)
        own-fields (set/union (:language-fields model-fields)
                              (:own model-fields))]
    (merge {}
           (if (:entity model)
             {:select {:fields (vec (into #{:id}
                                          (map (fn [field]
                                                 (if ((:language-fields model-fields) field)
                                                        (kc/raw (str (name field) "->>'EN' AS "
                                                                     (name field)))
                                                        field))
                                               (if fields
                                                 (filter
                                                  (fn [fld]
                                                    (if (vector? fld) ((set fields) (second fld))
                                                        ((set fields) fld)))
                                                  own-fields)
                                                 own-fields))))
                       :order (if (:sort query) (sort-query (:sort query)) (:default-order model))
                       :where (if (:filter query)
                                (reduce-kv (fn [where k v]
                                             (merge where
                                                    {k (if (sequential? v)
                                                         `['(symbol "in") ~v]
                                                         v)}))
                                           {}
                                           (:filter query)))
                       :limit (:limit query)
                       :offset (:offset query)}
              :joins (reduce-kv (fn [joins k v]
                                  (merge joins {k {:fn v
                                                   :chan (async/chan)}}))
                                {}
                                (if fields (select-keys (:joins model-fields) (filter identity (map #(% (:fks model)) fields)))
                                    (:joins model-fields)))})
           (if-let [data-fn (:data model)]
             {:data (data-fn query)})
           (if external-fields
             {:externals (reduce-kv (fn [included key req]
                                      (let [sid (keyword (str sid "-" (:topic req) "-" (name (:operation req))))
                                            chan (k/get-chan! sid)]
                                        (merge included {key {:sid sid
                                                              :chan chan}})))
                                    {}
                                    (if fields
                                      (select-keys external-fields (filter identity (map #(% (:fks model)) fields)))
                                      external-fields))}))))

(defn select-fks [model key data]
  (let [ids (set (map (fn [item] (key item)) data))
        query (parse-query model {:filter {:id ids}} "")]
    (vec (map transform-entity (build-select (:entity model) (:select query))))))

(defn select-ids [model ids]
  (let [query (parse-query model {:filter {:id ids}} "")]
    (vec (map transform-entity
              (build-select (:entity model) (:select query))))))

(defn string->array [value transform]
  (cond (vector? value)
        value
        (set? value)
        (vec value)
        (string? value)
        (let [array (str/split value #",")]
             (vec (set (map transform array))))))

(defn kafka-request [service query-externals model data]
  (doseq [[k v] query-externals]
    (k/send-msg! service (:sid v) (-> model :fields :externals k :topic)
                 {:type :request
                  :from (-> model :fields :externals k :from)
                  :operation (-> model :fields :externals k :operation)
                  :params (reduce-kv (fn [params key val]
                                       (merge params {key (val data)}))
                                     {} (:params (k (:externals (:fields model)))))})))

(defn join-fields [joins data]
  (doseq [[k v] joins]
    (async/go (>! (:chan v) ((:fn v) data)))))


(defn execute-select [service model req]
  (let [query (parse-query model (:query-params req) (:session-id req))
        data (if (:select query)
               (build-select (:entity model) (:select query))
               (:data query))]
    (prn query)
    (async/go (join-fields (:joins query) data)
              (kafka-request service (:externals query) model data))
    (merge {:data (if (sequential? data) (vec (map transform-entity data))
                      [(transform-entity data)])}
                  (if (some (complement empty?) [(:joins query) (:externals query)])
                    {:included (merge (if (:joins query)
                                        (reduce-kv (fn [joins k v]
                                                     (merge joins
                                                            {k (<!! (:chan v))}))
                                                   {} (:joins query)))
                                      (if (:externals query)
                                        (reduce-kv (fn [externals k v]
                                                     (merge externals
                                                            {k (<!! (:chan v))}))
                                                   {} (:externals query))))}))))
