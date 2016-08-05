(defproject common "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/encore "2.65.0"]
                 [io.pedestal/pedestal.service "0.5.0"]
                 [io.pedestal/pedestal.immutant "0.5.0"]
                 [pedestal-api "0.2.0" :exclusions [prismatic/schema]]
                 [prismatic/schema "1.1.2"]
                 [com.taoensso/carmine "2.13.1"]
                 [clj-redis-session "2.1.0"]
                 [environ "1.0.3"]
                 [midje "1.8.3"]
                 [utils "0.3.1-SNAPSHOT"]
                 [ch.qos.logback/logback-classic "1.1.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]]
  :min-lein-version "2.0.0"

  :resource-paths ["config", "resources"]
  :target-path "target/%s/"
  :plugins [[info.sunng/lein-bootclasspath-deps "0.2.0"]
            [lein-midje "3.0.0"]
            [lein-environ "1.0.3"]
            [lein-localrepo "0.5.3"]]
  :aliases {"migrate"  ["run" "-m" "common.db/migrate"]
            "rollback" ["run" "-m" "common.db/rollback"]}
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "common.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.0"]]}
             :env {:db-user "constantine"
                   :db-password "123123q"
                   :db "services"
                   :kafka-server "localhost:9091"}
             :uberjar {:aot [common.server]}}
  :main ^{:skip-aot true} common.server)
