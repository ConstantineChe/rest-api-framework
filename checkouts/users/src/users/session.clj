(ns users.session
  (:require [buddy.sign.jwt :as jwt]
            ))

(def secret "secret")

(defn create-token [client data]
  (jwt/sign data (str client secret)))

(defn unsign-token
  "doc-string"
  [client token]
  (let [user (try (jwt/unsign token (str client secret))
                  (catch java.lang.Exception e nil))]
    (merge {:status (if user "success" "fail")}
     user))
  )
