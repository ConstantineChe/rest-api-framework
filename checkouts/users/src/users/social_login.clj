(ns users.social-login
  (:import [com.github.scribejava.core.builder ServiceBuilder]
           [com.github.scribejava.apis FacebookApi VkontakteApi GoogleApi20 TwitterApi]
           [com.github.scribejava.core.model OAuthRequest Verb]))

(def service
  (let [builder (doto (ServiceBuilder.)
                  (.apiKey "267097843478756")
                  (.apiSecret "90c9539c9831521a7c78bfb137f80b93")
                  )]
    (.build builder (FacebookApi/instance))))



(def url (.getAuthorizationUrl service))

(prn url)
