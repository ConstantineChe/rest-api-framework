(ns utils.model
  (:require [korma.core :as kc]
            [utils.kafka-service :as k]
            [utils.cache :as cache :refer [with-cache]]
            [schema.core :as s]
            [clojure.set :as set]
            [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [<!! >!! <! >!]]))

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
  {:id (:id entity)
   :attrs (dissoc entity :id)})

(defn normalize-query [query query-map]
  (reduce-kv (fn [query key [new-key f]]
               (let [val (get-in query key)]
                 (if val
                   (-> (update-in query (butlast key) dissoc (last key))
                       (assoc-in new-key (f val)))
                   query)))
             query query-map))

(defn params->key [tag params]
  (if (set? params) (apply str ":" tag ":" (interpose "," params))
      (apply str ":" tag ":" (:limit params) (:offset params)
             (map (fn [[k v]] (apply str (if (sequential? v)
                                          (interpose "," v) v)))
                  (:filter params)))))

(defn query-select [query model]
  (let [fields (:fields query)
        model-fields (:fields model)
        own-fields (set/union (:language-fields model-fields)
                              (:own model-fields))]
    {:fields
     (vec (into #{:id}
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
     :offset (:offset query)}))

(defn query-joins [query model]
  (let [fields (:fields query)
        model-joins (-> model :fields :joins)]
    (reduce-kv (fn [joins k v]
                 (merge joins {k {:params v
                                  :chan (async/chan)}}))
               {}
               (if fields (select-keys model-joins (filter identity (map #(% (:fks model)) fields)))
                   model-joins))))

(defn query-externals [query model sid]
  (let [fields (:fields query)
        external-fields (-> model :fields :externals)]
    (reduce-kv (fn [included key req]
                 (let [sid (keyword (str sid "-" (:topic req) "-" (name (:operation req))))
                       chan (k/get-chan! sid)]
                   (merge included {key (merge {:sid sid
                                                :chan chan}
                                               (if (:cache req)
                                                 {:cache (params->key (:cache req) query)}))})))
               {}
               (if fields
                 (select-keys external-fields (filter identity (map #(% (:fks model)) fields)))
                 external-fields))))

(defn parse-query [model query sid]
  (let [query (normalize-query query (:map-params model))
        fields (:fields query)
        external-fields (-> model :fields :externals)]
    (merge {}
           (if (:entity model)
             {:select (query-select query model)
              :joins (query-joins query model)})
           (if-let [data-fn (:data model)]
             {:data (data-fn query)})
           (if external-fields
             {:externals (query-externals query model sid)}))))

(defn select-ids [{:keys [model query sid]}]
  (let [query (parse-query model query sid)]
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
    (if-not (cache/get-cache (:cache v))
      (k/send-msg! service (:sid v) (-> model :fields :externals k :topic)
                   {:type :request
                    :from (-> model :fields :externals k :from)
                    :operation (-> model :fields :externals k :operation)
                    :params (reduce-kv (fn [params key val]
                                         (merge params {key (if (fn? val) (val data) val)}))
                                       {} (:params (k (:externals (:fields model)))))}))))

(defn join-fields [joins data]
  (doseq [[k {:keys [chan params]}] joins]
    (async/go (let [ids (set (map #((:fk params) %) data))
                    params (assoc-in params [:query :filter :ids] ids)]
                (>! chan (with-cache (params->key (:cache params) ids)
                           (select-ids params)))))))


(defn execute-select [service model req include?]
  (let [query (parse-query model (:query-params req) (:session-id req))
        data (if (:select query)
               (build-select (:entity model) (:select query))
               (:data query))]
    (if include? (async/go (join-fields (:joins query) data)
                           (kafka-request service (:externals query) model data)))
    (merge {:data (vec (map transform-entity data))}
           (if (and include? (some (complement empty?) [(:joins query) (:externals query)]))
             {:included (merge (if (:joins query)
                                 (reduce-kv (fn [joins k v]
                                              (merge joins
                                                     {k (<!! (:chan v))}))
                                            {} (:joins query)))
                               (if (:externals query)
                                 (reduce-kv (fn [externals k v]
                                              (merge externals
                                                     {k (with-cache (:cache v) (<!! (:chan v)))}))
                                            {} (:externals query))))}))))
