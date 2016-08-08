(ns vehicles.service
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
            [vehicles.db :as db]
            [vehicles.kafka :as k]
            [utils.kafka-service :as service]
            [clojure.java.io :as io]
            [utils.interceptors :refer [request-session restrict-unauthorized token-auth]]
            [utils.schema
             [users :as users-schema]
             [common :as common-schema]
             [vehicles :as vehicles-schema]]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [pedestal-api
             [core :as api]
             [helpers :refer [before defbefore defhandler handler]]]
            [schema.core :as s]))


(def create-vehicle
  (handler
   ::create-vehicle
   {:summary "Create new vehicle"
    :responses {201 {:body {:message s/Str
                            :data vehicles-schema/Vehicle}}}
    :parameters {:body-params {:vehicle vehicles-schema/InputVehicle
                               :user s/Int}}
    :operationId :create-vehicle}
   (fn [{params :body-params}]
     (let [{:keys [vehicle user]} params]
       (-> (response {:message "Vehicle created"
                      :data (db/create-vehicle vehicle user)})
           (status 201))))))

(def create-vehicle-make
  (handler
   ::create-vehicle-make
   {:summary "Create new vehicle make"
    :responses {201 {:body {:message s/Str
                            :data vehicles-schema/VehicleMake}}}
    :parameters {:body-params vehicles-schema/InputVehicleMake}
    :operationId :create-vehicle-make}
   (fn [{vehicle-make :body-params}]
     (-> (response {:message "Vehicle make created"
                    :data (db/create-vehicle-make vehicle-make)})
         (status 201)))))

(def create-vehicle-model
  (handler
   ::create-vehicle-model
   {:summary "Create new vehicle model"
    :responses {201 {:body {:message s/Str
                            :data vehicles-schema/VehicleModel}}}
    :parameters {:body-params vehicles-schema/InputVehicleModel}
    :operationId :create-vehicle-model}
   (fn [{vehicle-model-input :body-params}]
     (let [vehicle-model (db/create-vehicle-model vehicle-model-input)]
       (-> (response {:message "Vehicle model created"
                      :data (db/create-vehicle-model vehicle-model)})
           (status 201))))))

(def create-vehicle-modification
  (handler
   ::create-vehicle-modification
   {:summary "Create new vehicle modification"
    :responses {201 {:body {:message s/Str
                            :data vehicles-schema/VehicleModification}}}
    :parameters {:body-params vehicles-schema/InputVehicleModification}
    :operationId :create-vehicle-modification}
   (fn [{vehicle-modification :body-params}]
     (-> (response {:message "Vehicle modification created"
                    :data (db/create-vehicle-modification vehicle-modification)})
         (status 201)))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))

(s/with-fn-validation
  (api/defroutes routes
    {:info {:title       "Vehicles"
            :description "Vehicles api"
            :version     "2.0"}
     :tags [{:name         "vehicles"
             :description  "api for vehicles"
             :externalDocs {:description "Find out more"
                            :url         "http://swagger.io"}}]}
    [[["/" ^:interceptors [session
                           (token-auth "vehicles" k/producer-chan)
                           ;restrict-unauthorized
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["vehicles"]})]
       ["/vehicles"  {:post create-vehicle}
        ["/models" {:post create-vehicle-model}]
        ["/makes" {:post create-vehicle-make}]
        ["/modifications" {:post create-vehicle-modification}]]
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
              ::http/port 8082
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
