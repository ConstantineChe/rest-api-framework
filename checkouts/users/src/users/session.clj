(ns users.session
  (:require [buddy.sign.jwt :as jwt]
            ))

(def secret "secret")

(defn create-token [user]
  (merge {:token (jwt/sign {:user user} secret)}
         {:user user}))

(defn unsign-token
  "doc-string"
  [token]
  (let [user (try (jwt/unsign token secret)
                  (catch java.lang.Exception e nil))]
    (merge {:status (if user "success" "fail")}
     user))
  )
