(ns utils.model
  (:require [korma.core :as kc]
            [utils.kafka-service :as k]
            [utils.cache :as cache :refer [with-cache]]
            [schema.core :as s]
            [clojure.set :as set]
            [io.pedestal.log :as log]
            [utils.logger :refer [with-time-log]]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [<!! >!! <! >!]]))

(defn sort-query [field]
  (if (.startsWith field "-")
    [(keyword (.substring field 1)) :DESC]
    [(keyword field) :ASC])
  )

(defn build-select [entity query]
  (with-time-log {:entity entity :query query}
    (eval `(kc/select ~entity
                      ~@(filter identity
                                [(if (eval `(:fields ~query)) `(kc/fields ~@(eval `(:fields ~query))))
                                 (if (eval `(:where ~query)) `(kc/where ~(eval `(:where ~query))))
                                 (if (eval `(:offset ~query)) `(kc/offset ~(eval `(:offset ~query))))
                                 (if (eval `(:limit ~query)) `(kc/limit ~(eval `(:limit ~query))))
                                 (if (eval `(:order ~query)) `(kc/order ~@(eval `(:order ~query))))])))))

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
  (prn params)
  (if (or (vector? params) (set? params))
    (str ":" tag ":" (apply str (interpose "," params)) ":")
    (str ":" tag ":" (cond (:ids params)
                           (apply str (interpose "," (:ids params)))
                           (:id params)
                           (apply str (interpose "," (:id params)))
                             :default -1)
          ":" (:limit params) (:offset params))))

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
                                  {k (if (or (set? v) (vector? v))
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

(defn query-externals [query model sid data]
  (let [fields (:fields query)
        external-fields (-> model :fields :externals)]
    (reduce-kv (fn [included key req]
                 (let [sid (keyword (str sid "-" (:topic req) "-" (name (:operation req))))
                       chan (k/get-chan! sid)]
                   (merge included
                          {key (merge {:sid sid
                                       :chan chan
                                       :cache (:cache req)})})))
               {}
               (if fields
                 (select-keys external-fields (filter identity (map #(% (:fks model)) fields)))
                 external-fields))))

(defn parse-query [model req]
  (let [sid (:session-id req)
        query (normalize-query (:query-params req) (:map-params model))
        fields (:fields query)
        external-fields (-> model :fields :externals)
        data (if-let [data-fn (:data model)]
               (data-fn req))]
    (merge {}
           (if (:entity model)
             {:select (query-select query model)
              :joins (query-joins query model)})
           (if data
             {:data data})
           (if external-fields
             {:externals (query-externals query model sid data)}))))

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
    (if-not (cache/get-cache (params->key (:tag (:cache v))
                                          ((-> model :fields :externals k :params :filter) data)))
      (k/send-msg! service (:sid v) (-> model :fields :externals k :topic)
                   {:type :request
                    :from (-> model :fields :externals k :from)
                    :operation (-> model :fields :externals k :operation)
                    :params (reduce-kv
                             (fn [params k v]
                               (merge params {k (if (fn? v) (v data) v)}))
                             {:with-includes? (-> model :fields :externals k :with-includes?)}
                             (:params (k (:externals (:fields model)))))}))))

(declare execute-select)

(defn join-fields [joins data service]
  (doseq [[k {:keys [chan params]}] joins]
    (async/go (let [ids (vec (set (map #((:fk params) %) data)))
                    params (assoc-in params [:query :filter :ids] ids)]
                (>! chan (if (:cache params)
                           (with-cache (params->key (:tag (:cache params)) ids) (:exp (:cache params))
                                  (execute-select service (:model params)
                                                  {:session-id (:sid params)
                                                   :query-params (:query params)}
                                                  (:with-includes? params)))
                           (execute-select service (:model params)
                                           {:session-id (:sid params)
                                            :query-params (:query params)}
                                           (:with-includes? params))))))))


(defn execute-select [service model req with-includes?]
  (let [query (parse-query model req)
        data (if (:select query)
               (build-select (:entity model) (:select query))
               (:data query))
        include-chans (atom {})]
    (if with-includes? (async/go (join-fields (:joins query) data service)
                                 (kafka-request service (:externals query) model data)))
    (merge {:data (vec (map transform-entity data))}
           (if (and with-includes?
                    (some (complement empty?) [(:joins query) (:externals query)]))
             (with-time-log {:includes (keys (merge (:joins query) (:externals query)))}
               {:included (merge (if (:joins query)
                                   (reduce-kv (fn [joins k v]
                                                (let [res (<!! (:chan v))]
                                                  (when (:included res)
                                                    (>!! (k (swap! include-chans assoc k (async/chan 1)))
                                                         {:data (:included res)}))
                                                  (merge joins
                                                         {k (:data res)})))
                                              {} (:joins query)))
                                 (if (:externals query)
                                   (reduce-kv (fn [externals k v]
                                                (let [res (if (:cache v)
                                                            (with-cache (params->key (:tag (:cache v))
                                                                                     ((-> model :fields :externals
                                                                                          k :params :filter) data))
                                                              (:exp (:cache v))
                                                              (<!! (:chan v)))
                                                            (<!! (:chan v)))]
                                                  (when (:included res)
                                                    (>!! (k (swap! include-chans assoc k (async/chan 1)))
                                                         {:data (:included res)}))
                                                  (merge externals
                                                         {k (:data res)})))
                                              {} (:externals query)))
                                 (reduce merge {} (map (comp :data <!!) (vals @include-chans))))})))))
