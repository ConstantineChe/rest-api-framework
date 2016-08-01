(ns users.social-login
  (:import [com.github.scribejava.core.builder ServiceBuilder]
           [com.github.scribejava.apis FacebookApi VkontakteApi GoogleApi20 TwitterApi]
           [com.github.scribejava.core.model OAuthRequest Verb]))


(def fb-resource-url "https://graph.facebook.com/v2.6/me")

(def fb-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "274772472896284")
                  (.apiSecret "3e5214972b192ceaea008e6fad221a19")
                  (.callback "http://localhost/login/fb")
                  )]
    (.build builder (FacebookApi/instance))))

(def vk-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "5570398")
                  (.apiSecret "NnYsMJ6FosA6YAb3yXwQ")
                  (.scope "notify")
                  (.callback "http:/localhost/test")
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



(def vk-url (.getAuthorizationUrl vk-service nil))

(def fb-url (.getAuthorizationUrl fb-service))

(defn fb-access-token
  "doc-string"
  [code]
  (.getAccessToken fb-service code)
  )
