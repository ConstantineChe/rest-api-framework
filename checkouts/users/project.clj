(defproject users "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [io.pedestal/pedestal.service "0.5.0"]
                 [io.pedestal/pedestal.immutant "0.5.0"]
                 [pedestal-api "0.2.0" :exclusions [prismatic/schema]]
                 [com.taoensso/carmine "2.13.1"]
                 [clj-redis-session "2.1.0"]
                 [prismatic/schema "1.1.2"]
                 [environ "1.0.3"]
                 [midje "1.8.3"]
                 [buddy/buddy-auth "1.1.0"]
                 [buddy/buddy-sign "1.1.0"]
                 [clj-kafka "0.3.4" :exclusions [org.slf4j/slf4j-log4j12]]
                 [clj-http "2.2.0"]
                 [ch.qos.logback/logback-classic "1.1.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]]
  :target-path "target/%s/"
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :plugins [[info.sunng/lein-bootclasspath-deps "0.2.0"]
            [lein-midje "3.0.0"]
            [lein-environ "1.0.3"]]

  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "users.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.0"]]
                   :env {}}
             :uberjar {:aot [users.server]}}
  :main users.server)
