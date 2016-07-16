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
  (let [user (jwt/unsign token secret)]
    (merge {:status (if user "success" "fail")}
     user))
  )
