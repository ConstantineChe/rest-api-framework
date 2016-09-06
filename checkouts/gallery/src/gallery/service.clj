(ns gallery.service
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
            [gallery.db :as db]
            [gallery.kafka :as k]
            [gallery.model :as model]
            [utils.model :as um]
            [utils.kafka-service :as service]
            [clojure.java.io :as io]
            [utils.interceptors :refer [request-session restrict-unauthorized
                                        token-auth array-params]]
            [utils.schema :as us]
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


(def get-gallery
    (handler
   ::get-gallery
   {:summary "Get gallery"
    :responses {201 {:body s/Any}}
    :parameters {:body-params s/Any}
    :operationId :create-vehicle-make}
   (fn [request]
     (response {:data :gallery}))))

(def redis-connection {})

(def session (middleware/session  {:store (redis-store redis-connection)}))

(s/with-fn-validation
  (api/defroutes routes
    {:info {:title       "Gallery"
            :description "Gallery api"
            :version     "2.0"}
     :tags [{:name         "vehicles"
             :description  "api for gallery"
             :externalDocs {:description "Find out more"
                            :url         "http://swagger.io"}}]}
    [[["/" ^:interceptors [session
                           (token-auth "vehicles" k/kafka-component)
                           ;restrict-unauthorized
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           array-params
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["vehicles"]})]
       {:get get-gallery}
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
              ::http/port 8084
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
