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

(def token-auth
  (interceptor/before
   ::token-auth
   (fn [{:keys [request] :as context}]
     (let [token (get-in request [:headers "auth-token"])
           msg-key (keyword (str "auth-" token))
           chan (if token (service/get-chan! msg-key))
           _ (service/send-msg! msg-key "common"
                                {:type :request
                                 :operation :token
                                 :params {:token token}})
           user (if token (<!! chan))]
       (assoc-in context [:request :user]
                 (if (= "success" (:status user))
                   (:user user)
                   nil))))))

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
