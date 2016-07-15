(ns users.session
  (:require [buddy.sign.jws :as jws]
            [cheshire.core :as json]
            ))

(defn create-token [user]
  (merge (json/encode {:token (jws/sign {:user user})})
         {:user user}))

(defn unsign-token
  "doc-string"
  [token]
  (let [user (jws/unsign token)]
    (merge {:status (if user "success" "fail")}
     user))
  )
