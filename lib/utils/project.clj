(defproject utils "0.3.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ "1.0.3"]
                 [io.pedestal/pedestal.service "0.5.0"]
                 [io.pedestal/pedestal.immutant "0.5.0"]
                 [prismatic/schema "1.1.3"]
                 [com.taoensso/carmine "2.13.1"]
                 [clj-redis-session "2.1.0"]
                 [korma "0.4.2"]
                 [ragtime "0.5.2"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [com.taoensso/encore "2.65.0"]
                 [ymilky/franzy "0.0.2-SNAPSHOT"]
                 [ymilky/franzy-nippy "0.0.1"]
                 [ymilky/franzy-common "0.0.1"]
                 [org.clojure/core.async "0.2.385"]]
  :plugins [[lein-localrepo "0.5.3"]]
  :aot :all)
