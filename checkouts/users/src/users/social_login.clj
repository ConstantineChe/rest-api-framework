(ns users.social-login
  (:import [com.github.scribejava.core.builder ServiceBuilder]
           [com.github.scribejava.apis FacebookApi VkontakteApi GoogleApi20 TwitterApi]
           [com.github.scribejava.core.model OAuthRequest Verb]))


(def fb-resource-url "https://graph.facebook.com/me?fields=id,first_name,last_name,middle_name,birthday,email,picture" )

(def fb-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "274772472896284")
                  (.apiSecret "3e5214972b192ceaea008e6fad221a19")
                  (.callback "http://localhost/login/fb/auth")
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

(defn google-service []
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "332296593369-rkjeql76as0omvr02qqvod86slp0m78o.apps.googleusercontent.com")
                  (.apiSecret "EPVJDDiLqBMQMlZXiNYClqlf")
                  (.scope "https://www.googleapis.com/auth/plus.login")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (GoogleApi20/instance))))



(def vk-url (.getAuthorizationUrl vk-service nil))

(defn fb-url [service] (.getAuthorizationUrl service))

(defn fb-auth
  "doc-string"
  [code]
  (let [service fb-service
        at (.getAccessToken service code)
        req (new OAuthRequest Verb/GET fb-resource-url service)
        _ (.signRequest service at req)
        res (.send req)
        ]
    (.getBody res)
    )
  )
