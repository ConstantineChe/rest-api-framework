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
            [users.model :as model]
            [utils.model :as um]
            [utils.kafka-service :as service]
            [clojure.java.io :as io]
            [users.social-login :as social]
            [utils.interceptors :refer [request-session restrict-unauthorized]]
            [utils.schema :as us]
            [utils.schema
             [users :as user-schema]
             [common :as common-schema]
             [vehicles :as vehicles-schema]]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [pedestal-api
             [core :as api]
             [helpers :refer [before defbefore defhandler handler]]]
            [schema.core :as s]))


;;========================== API route handlers ================================

(def users
  (handler
   ::users
   {:summary "api users"
    :responses {200 {:body {:data [(dissoc user-schema/User (s/required-key :password))]}}}
    :parameters {:query-params {(s/optional-key :limit) s/Int}}
    :operationId :users}
   (fn [request]
     (prn (:query-params request))
     (-> (response {:data (db/get-users)})
         (status 200)))))


(def create-user
  (handler
   ::create-user
   {:summary "create new user"
    :responses {201 {:body {:message s/Str}}}
    :parameters {:body-params user-schema/InputUser}
    :operationId :create-user}
   (fn [{user :body-params}]
     (db/create-user (dissoc user :password_conf))
     {:status 201 :body {:message "user created"}})))

(def get-token
  (handler
   ::get-token
   {:summary "get authorization token"
    :responses {200 {:body {:message s/Str
                            (s/optional-key :data) {:auth-token s/Str
                                                    :refresh-token s/Str
                                                    :user s/Str}}}}
    :parameters {:body-params {:email s/Str :password s/Str}}
    :operationId :get-token}
   (fn [request]
     (let [{:keys [email password]} (:body-params request)
           client (get-in request [:headers "user-agent"])
           user (db/get-user-by-email email)]
       (if (check password (:password user))
         (-> (response {:message "success"
                        :data {:auth-token (session/create-auth-token client user)
                               :refresh-token (session/create-refresh-token client user)
                               :user email}})
            (status 200)
            (assoc-in [:session :email] email))
         (response {:message "invalid username or password"}))))))

(def users-with-commons
  (handler
   ::users-with-commons
   {:summary "ms req"
    :responses {200 {:body {:data {:users [(dissoc user-schema/User (s/required-key :password))]
                                   :commons common-schema/Settings}}}}
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :operationId :users-with-commons}
   (fn [request]

     (let [sid (-> request :session-id keyword)
           sid (if sid sid :nil)
           chan (service/get-chan! sid)]
       (k/produce! sid "common" {:type :request
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
                            (s/optional-key :data) {:token s/Str}}}}
    :parameters {:body-params {:refresh-token s/Str}}
    :operationId :refresh-token}
   (fn [request]
     (let [[type auth-token] (try (str/split (get-in request [:headers "authorization"]) #" ")
                                  (catch java.lang.Exception e [nil nil]))
           refresh-token (-> request :body-params :refresh-token)
           client (get-in request [:headers "user-agent"])]
       (if-let [new-auth-token (session/refresh-token client refresh-token auth-token)]
         (-> (response {:message "success" :data {:token new-auth-token}})
             (status 200))
         (response {:message "invalid refresh or auth token"}))))))

(def create-vehicle
  (handler
   ::create-vehicle
   {:summary "Create user vihicle"
    :responses {201 {:body {:message s/Str
                            (s/optional-key :data) vehicles-schema/Vehicle}}}
    :parameters {:body-params (dissoc vehicles-schema/InputVehicle (s/required-key :user_id))}
    :operationId :create-vehicle}
   (fn [{vehicle :body-params user :user :as request}]
     (let [sid (request :session-id)
           sid (if sid (keyword (str "create-vehicle" sid)) :create-vehicle-nil)
           chan (service/get-chan! sid)
           vehicles-response (future (<!! chan))]
       (k/produce! sid "vehicles" {:type :request
                                          :from "users"
                                          :operation :create-vehicle
                                          :params {:vehicle vehicle
                                                   :user-id user}})
       (-> (response {:message "Created" :data @vehicles-response})
           (status 201))))))

(def delete-vehicle
  (handler
   ::delete-vehicle
   {:summary "Delete user vehicle"
    :responses {200 {:body {:message s/Str}}}
    :parameters {:body-params {:vehicle-id s/Int}}
    :operationId :delete-vehicle}
   (fn [{params :body-params user :user :as request}]
     (let [sid (request :session-id)
           sid (if sid (keyword (str "delete-vehicle" sid)) :delete-vehicle-nil)
           chan (service/get-chan! sid)
           vehicles-response (future (<!! chan))]
       (k/produce! sid "vehicles" {:type :request
                                   :from "users"
                                   :operation :delete-vehicle
                                   :params {:vehicle-id (:vehicle-id params)
                                                   :user-id user}})
       (if (= "success" (:status @vehicles-response))
         (-> (response {:message (:message @vehicles-response)})
             (status 200))
         (-> (response {:message (:message @vehicles-response)})
             (status 400)))))))

(def my-vehicles
  (handler
   ::my-vehicles
   {:summary "Get user's vehicles"
    :responses {200 {:body (us/api-response {:id s/Int
                                             :attrs
                                             {:vehicles [s/Int]}}
                                            {:vehicles vehicles-schema/VehicleOutput
                                             :makes vehicles-schema/VehicleMakeOutput
                                             :models vehicles-schema/VehicleModelOutput
                                             :modifications vehicles-schema/VehicleModificationOutput})}}
    :parameters {}
    :operationId :my-vehicles}
   (fn [{params :body-params user :user :as request}]
     (response (.fetch-data model/my-vehicles k/kafka-component  request true)))))

(def change-current-vehicle
  (handler
   ::change-current-vehicle
   {:summary "Change user's current vehicle"
    :responses {200 {:body {(s/optional-key :data)  vehicles-schema/Vehicle (s/optional-key :message) s/Str}}}
    :parameters {:body-params {:vehicle-id s/Int}}
    :operationId :change-current-vehicle}
   (fn [{params :body-params user :user :as request}]
     (let [sid (request :session-id)
           sid (if sid (keyword (str "change-current-vehicle" sid)) :delete-vehicle-nil)
           chan (service/get-chan! sid)
           vehicles-response (future (<!! chan))]
       (k/produce! sid "vehicles" {:type :request
                                   :from "users"
                                   :operation :get-users-vehicle
                                   :params {:vehicle-id (:vehicle-id params)
                                            :user-id user}})
       (if (= "success" (:status @vehicles-response))
         (-> (response {:data (:vehicle @vehicles-response)})
             (assoc-in [:session :current-vehicle] (:vehicle @vehicles-response)))
         (-> (response {:message (str "User has no car with id " (:vehicle-id params))})))))))

;;============================ Interceptors ====================================

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

;;========================= Site route handlers ================================

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
      (response (json/generate-string
                 {:message "success"
                  :data {:auth-token (session/create-auth-token client user)
                         :refresh-token (session/create-refresh-token client user)
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
      (response (json/generate-string
                 {:message "success"
                  :data {:auth-token (session/create-auth-token client user)
                         :refresh-token (session/create-refresh-token client user)
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
      (response (json/generate-string
                 {:message "success"
                  :data {:auth-token (session/create-auth-token client user)
                         :refresh-token (session/create-refresh-token client user)
                         :user email}})))))

(defn google-login [request]
  (redirect (social/google-url social/google-service)))

(defn fb-login [request]
  (redirect (social/fb-url social/fb-service)))

(defn vk-login [request]
  (redirect (social/vk-url social/vk-service)))

(defn login-page
  [request]
  (response (slurp (io/resource "login.html"))))

(defn register-page
  [request]
  (response (slurp (io/resource "register.html"))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))


;;============================== Routes ==================================


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
                           request-session
                           token-auth
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["users"]})]
       ["/my" ;^:interceptors [restrict-unauthorized]
        ["/vehicles"
         {:get my-vehicles
          :post create-vehicle
          :delete delete-vehicle}]
        ["/change-current-vehicle" {:put change-current-vehicle}]]
       ["/users" ;^:interceptors [restrict-unauthorized]
        {:get users
         :post create-user}
        ["/token" {:post get-token}
         ["/refresh" {:post refresh-token}]]
        ["/commons" {:get users-with-commons}]]
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
