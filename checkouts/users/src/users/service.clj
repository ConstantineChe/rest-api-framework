(ns users.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :as interceptor
             :refer [definterceptor defon-request]]
            [io.pedestal.http.ring-middlewares :as middleware]
            [clj-redis-session.core :refer [redis-store]]
            [ring.util.response :refer [response status]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [buddy.hashers :refer [check]]
            [users.session :as session]
            [users.db :as db]
            [users.kafka :as k]
            [utils.kafka-service :as service]
            [utils.interceptors :refer [request-session restrict-unauthorized]]
            [utils.schema
             [users :as us]
             [common :as cs]]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [pedestal-api
             [core :as api]
             [helpers :refer [before defbefore defhandler handler]]]
            [schema.core :as s]))



(def users
  (handler
   ::users
   {:summary "api users"
    :responses {200 {:body {:data [us/UserWithId]}}}
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :operationId :users}
   (fn [request]
     (-> (response {:data (db/get-users)})
         (status 200)
         (assoc-in [:session :name] (-> request :query-params :name))))))


(def create-user
  (handler
   ::create-user
   {:summary "create new user"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params us/User}
    :operationId :create-user}
   (fn [request]
     (db/create-user (:body-params request))
     {:status 201 :body {:message "user created"}})))

(def login
  (handler
   ::token
   {:summary "login"
    :responses {200 {:body {:message s/Str (s/optional-key :data) {:token s/Str :user s/Str}}}}
    :parameters {:body-params {:email s/Str :password s/Str}}
    :operationId :get-token}
   (fn [request]
     (let [{:keys [email password]} (:body-params request)
           client (get-in request [:headers "user-agent"])
           user (db/get-user-by-email email)]
       (if (check password (:password user))
         (-> (response {:message "success" :data {:token (session/create-token client {:email email})
                               :user email}})
            (status 200)
            (assoc-in [:session :email] email))
           (response {:message "invalid username or password"}))))))

(def users-with-commons
  (handler
   ::users-with-commons
   {:summary "ms req"
    :responses {200 {:body {:data {:users [us/UserWithId]
                                   :commons cs/Settings}}}}
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :operationId :users-with-commons}
   (fn [request]
     (let [sid (-> request :session-id keyword)
           sid (if sid sid :nil)
           chan (service/get-chan! sid)]
       (service/send-msg! sid "users" {:type :request
                                 :operation :settings})
       (-> (response {:data {:users (db/get-users)
                             :commons (<!! chan)}})
           (status 200))))))


(def token-auth
   (interceptor/before
    ::token-auth
    (fn [{:keys [request] :as context}]
      (let [token (get-in request [:headers "auth-token"])
            client (get-in request [:headers "user-agent"])
            user (if token (session/unsign-token client token))]
        (assoc-in context [:request :user]
                  (if (= "success" (:status user))
                    (:user user)
                    nil))))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))

(s/with-fn-validation
  (api/defroutes routes
    {:info {:title       "Users"
            :description "Users api"
            :version     "2.0"}
     :tags [{:name         "users"
             :description  "api users"
             :externalDocs {:description "Find out more"
                            :url         "http://swagger.io"}}]}
    [[["/" ^:interceptors [session
                           token-auth
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["users"]})]
       ["/users" ;^:interceptors [restrict-unauthorized]
        {:get users
         :post create-user}
        ["/commons" {:get users-with-commons}]]
       ["/login" {:post login}]
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
