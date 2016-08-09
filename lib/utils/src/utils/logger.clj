(ns utils.logger
  (:require [io.pedestal.log :refer [LoggerSource]])
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
     (.info t ^String (if (string? body) (str ">>= " body) (pr-str (str ">>= " body)))))
    ([t body throwable]
     (.info t (if (string? body) ^String (str ">>= " body) ^String (pr-str (str ">>= " body))) ^Throwable throwable)))
  (-warn
    ([t body]
     (.warn t ^String (if (string? body) body (pr-str body))))
    ([t body throwable]
     (.warn t (if (string? body) ^String body ^String (pr-str body)) ^Throwable throwable)))
  (-error
    ([t body]
     (.error t ^String (if (string? body) body (pr-str body))))
    ([t body throwable]
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
