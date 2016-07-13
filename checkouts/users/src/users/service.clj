(ns users.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :refer [definterceptor]]
            [io.pedestal.http.ring-middlewares :as middleware]
            [clj-redis-session.core :refer [redis-store]]
            [ring.util.response :refer [response status]]
            [clj-http.client :as client]
            [buddy.sign.jws :as jws]
            [cheshire.core :as ch]
            [environ.core :refer [env]]
             [pedestal-api
             [core :as api]
             [helpers :refer [before defbefore defhandler handler]]]
             [schema.core :as s]))

(s/defschema User
  {:name s/Str
   :email s/Str
   :token s/Str})

(s/defschema Commons
  {:hello s/Str
   (s/optional-key :name) s/Str})

(def secret "secret")

(def users
  (handler
   ::users
   {:summary "api users"
    :responses {200 {:body {:data [User]}}}
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :operationId :users}
   (fn [request]
     (-> (response {:data [{:name "user1"
                            :email "user1@mail.de"
                            :token "token1"}
                           {:name "user2"
                            :email "user2@mail.te"
                            :token "token2"}]})
         (status 200)
         (assoc-in [:session :name] (-> request :query-params :name))))))

(def token
  (handler
   ::token
   {:summary "api token"
    :responses {200 {:body {:data {:token s/Str :user s/Str}}}}
    :parameters {:query-params {:user s/Str}}
    :operationId :get-token}
   (fn [request]
     (let [user (-> request :query-params :user)]
       (-> (response {:data (merge (ch/encode {:token (jws/sign {:user user})})
                                   {:user user})})
           (status 200))))))

(def unsign-token
  (handler
   ::unsign-token
   {:summary "unsign api token"
    :responses {200 {:body {:data {(s/optional-key :user) s/Str
                                   :status s/Str}}}}
    :parameters {:query-params {:token s/Str}}
    :operationId :unsign-token}
   (fn [request]
     (let [user (-> request :query-params token jws/unsign)]
       (-> (response {:data (merge
                             {:status (if user "success" "fail")}
                             user)})
           (status 200))))))

(def users-with-commons
  (handler
   ::users-with-commons
   {:summary "ms req"
    :responses {200 {:body {:data {:users [User]
                                   :commons Commons}}}}
    :parameters {}
    :operationId :users-with-commons}
   (fn [request]
     (let [commons (-> (client/get "http://localhost:8081/commons/")
                       :body (ch/parse-string true) :data)]
       (-> (response {:data {:users [{:name "user1"
                                      :email "user1@mail.de"
                                      :token "token1"}
                                     {:name "user2"
                                      :email "user2@mail.te"
                                      :token "token2"}]
                             :commons commons}})
           (status 200))))))


(def redis-connection {})

(definterceptor session-interceptor
  (middleware/session  {:store (redis-store redis-connection)}))

(s/with-fn-validation
  (api/defroutes routes
    {:info {:title       "Users"
            :description "Users api"
            :version     "2.0"}
     :tags [{:name         "users"
             :description  "api users"
             :externalDocs {:description "Find out more"
                            :url         "http://swagger.io"}}]}
    [[["/" ^:interceptors [session-interceptor
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["users"]})]
       ["/users"
        {:get users}
        ["/commons" {:get users-with-commons}]
        ["/token" {:get token}
         ["/unsign" {:get unsign-token}]]]

       ["/swagger.json" {:get api/swagger-json}]
       ["/*resource" {:get api/swagger-ui}]]]]))

;; Consumed by users.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes
              ::http/router :linear-search

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::http/type :immutant
              ;;::http/host "localhost"
              ::http/port 8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
