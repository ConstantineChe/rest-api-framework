(defproject scheduler "0.1.0-SNAPSHOT"
  :description "Scheduler microservice"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [utils "0.3.1-SNAPSHOT"]
                 [prismatic/schema "1.1.2"]
                 [environ "1.0.3"]
                 [org.clojure/core.async "0.2.385"]
                 [clojurewerkz/quartzite "2.0.0"]
                 [com.stuartsierra/component "0.3.1"]]
  :aliases {"migrate"  ["run" "-m" "scheduler.db/migrate"]
            "rollback" ["run" "-m" "scheduler.db/rollback"]
            "autotest" ["with-profile" "test" "midje" ":autotest"]}
  :profiles {:dev {:kafka-server "localhost:9091"
                   :db "carbook_scheduler"}
             :uberjar {:aot :all}}
  :main scheduler.core)
