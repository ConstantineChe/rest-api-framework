(ns common.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :as interceptor :refer [definterceptor defon-request]]
            [ring.util.response :refer [response status]]
            [io.pedestal.http.ring-middlewares :as middleware]
            [clj-redis-session.core :refer [redis-store]]
            [clojure.string :as str]
            [cheshire.core :as ch]
            [common.kafka :as k]
            [common.db :as db]
            [clojure.core.async :refer [<!!]]
            [pedestal-api
             [core :as api]
             [helpers :refer [before defbefore defhandler handler]]]
            [schema.core :as s]))

(s/defschema Settings
  {:hello s/Str
   (s/optional-key :name) s/Str})



(def settings
  (handler
   ::common
   {:summary "website settings"
    :parameters {:query-params {(s/optional-key :name) s/Str}}
    :responses {200 {:body {:data Settings
                            (s/optional-key :user) s/Str}}}
    :operationId :settings}
   (fn [request]
     (let [name (-> request :query-params :name)
           data (db/get-settings name)
           sid (-> request :session-id keyword)
           sid (if sid sid :nil)
           chan (k/get-chan! sid)]
       (k/send-msg! sid "common" {:type :request
                                  :operation :token
                                  :params {:token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoidGVzdCJ9.wk1swko8GbuwRMRuTR6q_x7AZQGbbwm8sZLyg90afbs"}})
      {:status 200
       :body {:data data
              :user (:user (<!! chan))}}))))

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

(s/with-fn-validation
  (api/defroutes routes
    {:info {:title       "Commons"
            :description "Commons api"
            :version     "2.0"}
     :tags [{:name         "common"
             :description  "website settings"
             :externalDocs {:description "Find out more"
                            :url         "http://swagger.io"}}]}
    [[["/" ^:interceptors [session
                           request-session
                           api/error-responses
                           (api/negotiate-response)
                           (api/body-params)
                           api/common-body
                           (api/coerce-request)
                           (api/validate-response)
                           (api/doc {:tags ["common"]})]
       ["/commons"
        {:get settings}
        ]

       ["/swagger.json" {:get api/swagger-json}]
       ["/*resource" {:get api/swagger-ui}]]]]))




;; Consumed by common.server/create-server
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
              ::http/port 8081
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
