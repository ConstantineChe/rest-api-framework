(defproject scheduler "0.1.0-SNAPSHOT"
  :description "Scheduler microservice"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [utils "0.3.1-SNAPSHOT"]
                 [prismatic/schema "1.1.2"]
                 [environ "1.0.3"]
                 [clojurewerkz/quartzite "2.0.0"]])
