(defproject services "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [common "0.0.1-SNAPSHOT"]
                 [users "0.0.1-SNAPSHOT"]
                 [io.aviso/config "0.1.13"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-modules "0.3.11"]]
  :target-path "target/%s/"
  :resource-paths ["config", "resources" "checkouts/users/src" "checkouts/common/src"]
  :modules {:dirs ["checkouts/common" "checkouts/users"]}
  :profiles {:uberjar {:aot :all}}
  :aot :all
  :main services.core)
