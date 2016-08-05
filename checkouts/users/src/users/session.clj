(ns users.session
  (:require [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            ))

(defn current-time []
  (quot (System/currentTimeMillis) 1000))

(defn secret [client] (hash/sha256 (str client "secret")))

(def enc {:alg :dir :enc :a128cbc-hs256})

(defn create-auth-token [client user]
  (jwt/encrypt {:iss "default"  ; [RFC7523]: The JWT MUST contain an "iss" (issuer) claim that contains a
                                ;  unique identifier for the entity that issued the JWT.issuer of this token
                :sub (:id user) ; [RFC7523]: For client authentication, the subject MUST be the "client_id" of the OAuth client.
                :aud "default"  ; [RFC7523]: The JWT MUST contain an "aud" (audience) claim containing a
                                ;   value that identifies the authorization server as an intended audience.
                :exp (+ (current-time) (* 60 60)) ; [RFC7523]: The JWT MUST contain an "exp" (expiration time) claim that
                                                  ;  limits the time window during which the JWT can be used.
                }
               (secret client) enc))

(defn create-refresh-token [client user]
  (jwt/encrypt {:iss "default"
                :sub (:id user)
                :aud "default"} (secret client) enc))

(defn unsign-token
  "doc-string"
  [client token]
  (let [claim (try (jwt/decrypt token (secret client) enc)
                   (catch java.lang.Exception e nil))
        expired? (and claim (> (current-time) (:exp claim)))]
    (merge {:status (if claim (if expired? "expired" "success") "fail")}
           (if (and claim (not expired?))
             {:user (:sub claim)})))
)

(defn refresh-token [client refresh-token auth-token]
  (let [auth-claim (try (jwt/decrypt auth-token (secret client) enc)
                        (catch java.lang.Exception e nil))
        refresh-claim (try (jwt/decrypt refresh-token (secret client) enc)
                           (catch java.lang.Exception e nil))]
    (if (and auth-claim refresh-claim
             (every? true?
                     (map (fn [[ka va] [kr vr]]
                            (= va vr))
                          (select-keys auth-claim [:iss :sub :aud]) refresh-claim)))
      (jwt/encrypt (assoc auth-claim :exp (+ (current-time) (* 60 60))) (secret client) enc))))
