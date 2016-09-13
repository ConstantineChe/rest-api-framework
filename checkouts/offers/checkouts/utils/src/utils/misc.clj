(ns utils.misc
  (:require [clj-time.format :as f]
            [clj-time.core :as t] ))

(defn date-time
  ([]
   (t/to-time-zone (t/now) (t/time-zone-for-id "Europe/Kiev")))
  ([time]
   (t/to-time-zone time (t/time-zone-for-id "Europe/Kiev")))
  ([time zone]
   (t/to-time-zone time (t/time-zone-for-id zone))))

(defn to-iso-string [time]
  (f/unparse (.withZone (f/formatters :date-hour-minute-second-ms) (.getZone time)) time))

(def iso-now (comp to-iso-string date-time))
