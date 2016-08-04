(ns users.social-login
  (:import [com.github.scribejava.core.builder ServiceBuilder]
           [com.github.scribejava.apis FacebookApi VkontakteApi GoogleApi20 TwitterApi]
           [com.github.scribejava.core.model OAuthRequest Verb]))


(def fb-resource-url "https://graph.facebook.com/me?fields=id,first_name,last_name,middle_name,gender,birthday,email,picture" )

(def vk-resource-url "https://api.vk.com/method/users.get?fields=uid,first_name,last_name,sex,bdate,photo_50,email")

(def google-resource-url "https://www.googleapis.com/plus/v1/people/me")

(def fb-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "274772472896284")
                  (.apiSecret "3e5214972b192ceaea008e6fad221a19")
                  (.callback "http://localhost/login/fb/auth")
                  )]
    (.build builder (FacebookApi/instance))))

(def vk-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "5573637")
                  (.apiSecret "unksmyfNASsW8G7PtB0D")
                  (.scope "notify,offline,email")
                  (.callback "http://localhost/login/vk/auth")
                  )]
    (.build builder (VkontakteApi/instance))))

(def google-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "332296593369-rkjeql76as0omvr02qqvod86slp0m78o.apps.googleusercontent.com")
                  (.apiSecret "EPVJDDiLqBMQMlZXiNYClqlf")
                  (.scope "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/plus.login")
                  (.callback "http://localhost/login/google/auth")
                  )]
    (.build builder (GoogleApi20/instance))))



(defn vk-url [service] (.getAuthorizationUrl service nil))

(defn fb-url [service] (.getAuthorizationUrl service))

(defn google-url [service] (.getAuthorizationUrl service {"access_type" "offline"
                                                          "prompt" "consent"}))

(google-url google-service)

(defn auth
  "doc-string"
  [code auth-service resource-url]
  (let [service auth-service
        at (.getAccessToken service code)
        req (new OAuthRequest Verb/GET resource-url service)
        _ (.signRequest service at req)
        res (.send req)
        ]
    (.getBody res)
    )
  )
