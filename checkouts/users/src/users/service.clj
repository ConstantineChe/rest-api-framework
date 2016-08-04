(ns users.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :as interceptor
             :refer [definterceptor defon-request]]
            [io.pedestal.http.ring-middlewares :as middleware]
            [clj-redis-session.core :refer [redis-store]]
            [ring.util.response :refer [response status redirect]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [buddy.hashers :refer [check]]
            [users.session :as session]
            [users.db :as db]
            [users.kafka :as k]
            [utils.kafka-service :as service]
            [clojure.java.io :as io]
            [users.social-login :as social]
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
    :responses {200 {:body {:data [us/User]}}}
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
    :parameters {:body-params us/InputUser}
    :operationId :create-user}
   (fn [{user :body-params}]
     (db/create-user (dissoc user :password_conf))
     {:status 201 :body {:message "user created"}})))

(def get-token
  (handler
   ::get-token
   {:summary "login"
    :responses {200 {:body {:message s/Str
                            (s/optional-key :data) {:token s/Str :user s/Str}}}}
    :parameters {:body-params {:email s/Str :password s/Str}}
    :operationId :get-token}
   (fn [request]
     (let [{:keys [email password]} (:body-params request)
           client (get-in request [:headers "user-agent"])
           user (db/get-user-by-email email)]
       (if (check password (:password user))
         (-> (response {:message "success" :data {:token (session/create-token client {:id (:id user)})
                               :user email}})
            (status 200)
            (assoc-in [:session :email] email))
         (response {:message "invalid username or password"}))))))

(def users-with-commons
  (handler
   ::users-with-commons
   {:summary "ms req"
    :responses {200 {:body {:data {:users [us/User]
                                   :commons cs/Settings}}}}
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :operationId :users-with-commons}
   (fn [request]
     (let [sid (-> request :session-id keyword)
           sid (if sid sid :nil)
           chan (service/get-chan! sid)]
       (service/send-msg! sid "common" {:type :request
                                       :from "users"
                                       :operation :settings})
       (-> (response {:data {:users (db/get-users)
                             :commons (<!! chan)}})
           (status 200))))))

(def refresh-token
  (handler
   ::refresh-token
   {:summary "refresh authorization token"
    :responses {200 {:body {:message s/Str
                            (s/optional-key :data) {:token s/Str :user s/Str}}}}
    :parameters {:body-params {:refresh-token s/Str}}
    :operationId :refresh-token}
   (fn [request]
     (let [{:keys [email password]} (:body-params request)
           client (get-in request [:headers "user-agent"])
           user (db/get-user-by-email email)]
       (if (check password (:password user))
         (-> (response {:message "success" :data {:token (session/create-token client {:id (:id user)})
                               :user email}})
            (status 200)
            (assoc-in [:session :email] email))
         (response {:message "invalid username or password"}))))))

(def create-vehicle
  (handler
   ::create-vehicle
   {:summary "Create new vehicle"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params us/InputVehicle}
    :operationId :create-vehicle}
   (fn [{vehicle :body-params user :user}]
     (db/create-vehicle vehicle user)
     (-> (response {:message "Vehicle created"})
         (status 201)))))

(def create-vehicle-make
  (handler
   ::create-vehicle-make
   {:summary "Create new vehicle make"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params us/InputVehicleMake}
    :operationId :create-vehicle-make}
   (fn [{vehicle-make :body-params}]
     (-> (response {:message "Vehicle make created"})
         (status 201)))))

(def create-vehicle-model
  (handler
   ::create-vehicle-model
   {:summary "Create new vehicle model"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params us/InputVehicleModel}
    :operationId :create-vehicle-model}
   (fn [{vehicle-model :body-params}]
     (-> (response {:message "Vehicle model created"})
         (status 201)))))

(def create-vehicle-modification
  (handler
   ::create-vehicle-modification
   {:summary "Create new vehicle modification"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params us/InputVehicle}
    :operationId :create-vehicle-modification}
   (fn [{vehicle-modification :body-params}]
     (-> (response {:message "Vehicle modification created"})
         (status 201)))))



(def token-auth
   (interceptor/before
    ::token-auth
    (fn [{:keys [request] :as context}]
      (let [[type token] (try (str/split (get-in request [:headers "authorization"]) #" ")
                              (catch java.lang.Exception e [nil nil]))
            client (get-in request [:headers "user-agent"])
            user (if (and (= "Bearer" type) token)
                   (session/unsign-token client token))]
        (assoc-in context [:request :user]
                  (if (= "success" (:status user))
                    (:user user)
                    nil))))))
(defn fb-auth [request]
  (let [data (json/parse-string (social/auth (-> request :params :code)
                                             social/fb-service
                                             social/fb-resource-url) true)
        email (:email data (str "fb-" (:id data) "@test.cr"))
        client (get-in request [:headers "user-agent"])]
    (let [user (if-let [user (db/get-user-by-email email)] user
                       (db/create-user {:name (:first_name data)
                                        :surname (:last_name data)
                                        :middlename (:middlename data)
                                        :gender (:gender data)
                                        :email email
                                        :password "secret"}))]
      (response (json/generate-string {:message "success" :data {:token (session/create-token client {:id (:id user)})
                                             :user email}})))))

(defn vk-auth [request]
  (let [data (first (:response (json/parse-string (social/auth (-> request :params :code)
                                                               social/vk-service
                                                               social/vk-resource-url) true)))
        email (str "vk-" (:uid data) "@test.cr")
        client (get-in request [:headers "user-agent"])]
    (let [user (if-let [user (db/get-user-by-email email)] user
                       (db/create-user {:name (:first_name data)
                                        :surname (:last_name data)
                                        :gender (case (:sex data) 1 "femal" 2 "male" nil)
                                        :dob (if (= 12 (count (:bdate data)))
                                                 (apply str (interpose "-" (-> (:bdate data) (str/split #"\.") reverse ))))
                                        :email email
                                        :password "secret"}))]
      (response (json/generate-string {:message "success" :data {:token (session/create-token client {:id (:id user)})
                                                                 :user email}})))))


(defn google-auth [request]
  (let [data (json/parse-string (social/auth (-> request :params :code)
                                             social/google-service
                                             social/google-resource-url) true)
        email (:value (first (filter #(= "account" (:type %)) (:emails data))))
        client (get-in request [:headers "user-agent"])]
    (let [user (if-let [user (db/get-user-by-email email)] user
                       (db/create-user {:name (-> data :name :givenName)
                                        :surname (-> data :name :familyName)
                                        :gender (:gender data)
                                        :email email
                                        :password "secret"}))]
      (response (json/generate-string {:message "success" :data {:token (session/create-token client {:id (:id user)})
                                                                 :user email}})))))

(defn google-login [request]
  (redirect (social/google-url social/google-service)))

(defn fb-login [request]
  (redirect (social/fb-url social/fb-service)))

(defn vk-login [request]
  (redirect (social/vk-url social/vk-service)))

(defn login-page
  [request]
  (response (slurp (io/file (io/resource "login.html")))))

(defn register-page
  [request]
  (response (slurp (io/file (io/resource "register.html")))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))

(s/with-fn-validation
  (api/defroutes api-routes
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
        ["/token" {:post get-token}
         ["/refresh" {:get refresh-token}]]
        ["/commons" {:get users-with-commons}]]
       ["/vehicles" ^:interceptors [restrict-unauthorized] {:post create-vehicle}
        ["/models" {:post create-vehicle-model}]
        ["/makes" {:post create-vehicle-make}]
        ["/modifications" {:post create-vehicle-modification}]]

       ["/swagger.json" {:get api/swagger-json}]
       ["/*resource" {:get api/swagger-ui}]]]]))

(defroutes html-routes
  [[["/" ^:interceptors [http/html-body]
     ["/login" {:get login-page}
      ["/fb" {:get fb-login}
       ["/auth" {:get fb-auth}]]
      ["/vk" {:get vk-login}
       ["/auth" {:get vk-auth}]]
      ["/google" {:get google-login}
       ["/auth" {:get google-auth}]]]
     ["/register" {:get register-page}]]]])

(def routes (concat html-routes api-routes))


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
