(ns utils.logger
  (:require [io.pedestal.log :as log :refer [LoggerSource]]
            [utils.cache :refer [wcar*]]
            [taoensso.carmine :as car])
  (:import [org.slf4j Logger LoggerFactory]))

(extend-protocol LoggerSource
  Logger
  (-level-enabled? [t level-key]
    (case level-key
      :trace (.isTraceEnabled t)
      :debug (.isDebugEnabled t)
      :info (.isInfoEnabled t)
      :warn (.isWarnEnabled t)
      :error (.isErrorEnabled t)))
  (-trace
    ([t body]
     (.trace t ^String (if (string? body) body (pr-str (str body)))))
    ([t body throwable]
     (.trace t (if (string? body) ^String body ^String (pr-str body)) ^Throwable throwable)))
  (-debug
    ([t body]
     (.debug t ^String (if (string? body) body (pr-str body))))
    ([t body throwable]
     (.debug t (if (string? body) ^String body ^String (pr-str body)) ^Throwable throwable)))
  (-info
    ([t body]
     (wcar* (car/set (str ":logs:info:" (new java.util.Date) ":") body))
     (.info t ^String (if (string? body) (str body) (pr-str (str ">>= " body)))))
    ([t body throwable]
     (.info t (if (string? body) ^String (str ">>= " body) ^String (pr-str (str ">>= " body))) ^Throwable throwable)))
  (-warn
    ([t body]
     (wcar* (car/set (str ":logs:warn:" (new java.util.Date) ":") body))
     (.warn t ^String (if (string? body) body (pr-str body))))
    ([t body throwable]
     (.warn t (if (string? body) ^String body ^String (pr-str body)) ^Throwable throwable)))
  (-error
    ([t body]
     (wcar* (car/set (str ":logs:error:" (new java.util.Date) ":") body))
     (.error t ^String (if (string? body) body (pr-str body))))
    ([t body throwable]
     (wcar* (car/set (str ":logs:error:" (new java.util.Date) ":") body))
     (.error t (if (string? body) ^String body ^String (pr-str body)) ^Throwable throwable)))
  nil
  (-level-enabled? [t level-key] false)
  (-trace
    ([t body] nil)
    ([t body throwable] nil))
  (-debug
    ([t body] nil)
    ([t body throwable] nil))
  (-info
    ([t body] nil)
    ([t body throwable] nil))
  (-warn
    ([t body] nil)
    ([t body throwable] nil))
  (-error
    ([t body] nil)
    ([t body throwable] nil)))

(defmacro with-time-log [keyvals & body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# (time (do ~@body))]
         (log/info ~@(reduce-kv conj [] keyvals) :time (str s#))
         r#))))
