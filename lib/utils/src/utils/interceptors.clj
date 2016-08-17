(ns utils.interceptors
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :as interceptor :refer
             [definterceptor defon-request]]
            [ring.util.response :refer [response status]]
            [io.pedestal.http.ring-middlewares :as middleware]
            [clj-redis-session.core :refer [redis-store]]
            [utils.kafka-service :as service]
            [clojure.string :as str]
            [clojure.core.async :refer [<!!]]))

(defn token-auth [service kafka-component]
  (interceptor/before
   ::token-auth
   (fn [{:keys [request] :as context}]
     (let [[type token] (try (str/split (get-in request [:headers "authorization"]) #" ")
                             (catch java.lang.Exception e [nil nil]))
           client (get-in request [:headers "user-agent"])
           msg-key (keyword (str "auth-" token))
           chan (if token (service/get-chan! msg-key))
           _ (if (and (= "Bearer" type) token)
               (service/send-msg! kafka-component msg-key "users"
                                  {:type :request
                                   :from service
                                   :operation :token
                                   :params {:token token
                                            :client client}}))
           user (if token (<!! chan))]
       (assoc-in context [:request :user]
                 (if (= "success" (:status user))
                   (:user user)
                   nil))))))

(def array-params
  (interceptor/before
   ::array-params
   (fn [{:keys [request] :as context}]
     (let [params (:query-params request)]
       (assoc-in context [:request :query-params]
                 (reduce-kv (fn [new-params key val]
                              (if (re-find #"\[*\]" (name key))
                                (let [[outer inner] (clojure.string/split (name key) #"\[")
                                      outer (keyword outer)
                                      inner (keyword (.substring inner 0 (dec (count inner))))]
                                  (assoc-in new-params [outer inner] val))
                                (merge new-params {key val})))
                            {} params))))))


(def restrict-unauthorized
  (interceptor/after
   ::restrict
   (fn [{:keys [request response] :as context}]
     (if (:user request)
       context
       (assoc context :response
              {:status 401
               :body {:message "Not authorized"}})))))

(def request-session
  (interceptor/before
   ::request-session
   (fn [{:keys [request] :as context}]
    (let [cookies (get-in request [:headers "cookie"])
          [_ session] (try (str/split
                            (first (filter #(.startsWith % "JSESSIONID")
                                           (str/split cookies #"; ")))
                            #"=")
                           (catch java.lang.Exception e
                             [nil "nil"]))]
      (assoc context :request (assoc request :session-id session))))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))
