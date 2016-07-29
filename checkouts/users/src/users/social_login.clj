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
                  (.apiKey "267097843478756")
                  (.apiSecret "90c9539c9831521a7c78bfb137f80b93")
                  (.scope "friends")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (VkontakteApi/instance))))

(def google-service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "AIzaSyAGUF08yBlVgypDHqDtTUmp7Y-a4mMkYnA")
                  (.apiSecret "anonymous")
                  (.scope "https://docs.google.com/feeds/")
                  (.callback "http:/localhost:8080/test")
                  )]
    (.build builder (GoogleApi20/instance))))


(def google-access-token (.getAccessTokenPasswordGrant google-service "che.constantine@gmail.com" "test"))

(def vk-url (.getAuthorizationUrl vk-service nil))

(def fb-url (.getAuthorizationUrl fb-service))



(prn fb-url vk-url)
