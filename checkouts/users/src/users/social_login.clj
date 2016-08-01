(ns users.social-login
  (:import [com.github.scribejava.core.builder ServiceBuilder]
           [com.github.scribejava.apis FacebookApi VkontakteApi GoogleApi20 TwitterApi]
           [com.github.scribejava.core.model OAuthRequest Verb]))

(def fb-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "267097843478756")
                  (.apiSecret "90c9539c9831521a7c78bfb137f80b93")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (FacebookApi/instance))))

(def vk-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "5570398")
                  (.apiSecret "NnYsMJ6FosA6YAb3yXwQ")
                  (.scope "notify")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (VkontakteApi/instance))))

(def google-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "332296593369-rkjeql76as0omvr02qqvod86slp0m78o.apps.googleusercontent.com")
                  (.apiSecret "EPVJDDiLqBMQMlZXiNYClqlf")
                  (.scope "https://www.googleapis.com/auth/plus.login")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (GoogleApi20/instance))))


(def google-access-token (.getAccessTokenPasswordGrant google-service "ttqqqww@gmail.com" "123123"))

(def vk-url (.getAuthorizationUrl vk-service nil))

(def fb-url (.getAuthorizationUrl fb-service))



(prn fb-url vk-url google-access-token)
