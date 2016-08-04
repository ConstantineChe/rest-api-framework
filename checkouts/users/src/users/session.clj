(ns users.session
  (:require [buddy.sign.jwt :as jwt]
            ))

(defn current-time []
  (quot (System/currentTimeMillis) 1000))

(def secret "secret")

(defn create-auth-token [client user]
  (jwt/sign {:iss "default"  ; [RFC7523]: The JWT MUST contain an "iss" (issuer) claim that contains a
                              ;  unique identifier for the entity that issued the JWT.issuer of this token
             :sub (:id user) ; [RFC7523]: For client authentication, the subject MUST be the "client_id" of the OAuth client.
             :aud "default"  ; [RFC7523]: The JWT MUST contain an "aud" (audience) claim containing a
                             ;   value that identifies the authorization server as an intended audience.
             :exp (+ (current-time) (* 60 60)) ; [RFC7523]: The JWT MUST contain an "exp" (expiration time) claim that
                                               ;   limits the time window during which the JWT can be used.
             }
            (str client secret)))
(defn create-refresh-token [client user]
  (jwt/sign {:iss "default"
             :sub (:id user)
             :aud "default"} (str client secret)))

(defn unsign-token
  "doc-string"
  [client token]
  (let [claim (try (jwt/unsign token (str client secret))
                   (catch java.lang.Exception e nil))
        expired? (if claim (> (current-time) (:exp claim)))]
    (merge {:status (if claim (if-not expired? "expired" "success") "fail")}
           (if (and claim (not expired?)) (:sub claim))))
)

(defn refresh-token [client refresh-token auth-token]
  (let [auth-claim (try (jwt/unsign auth-token (str client secret))
                        (catch java.lang.Exception e nil))
        refresh-claim (try (jwt/unsign refresh-token (str client secret))
                           (catch java.lang.Exception e nil))]
    (if (every? true? (map (fn [[ka va] [kr vr]] (= va vr)) (select-keys auth-claim [:iss :sub :aud]) refresh-claim))
      (jwt/sign (assoc auth-claim :exp (+ (current-time) (* 60 60))) (str client secret)))))
