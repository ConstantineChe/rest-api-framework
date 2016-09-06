(defproject services "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [common "0.0.1-SNAPSHOT"]
                 [users "0.0.1-SNAPSHOT"]
                 [vehicles "0.0.1-SNAPSHOT"]
                 [scheduler "0.1.0-SNAPSHOT"]
                 [features "0.0.1-SNAPSHOT"]
                 [gallery "0.0.1-SNAPSHOT"]
                 [io.aviso/config "0.1.13"]
                 [environ "1.0.3"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-modules "0.3.11"]
            [lein-environ "1.0.3"]]
  :target-path "target/%s/"
  :resource-paths ["config" "resources" "checkouts/users/src" "checkouts/common/src" "checkouts/vehicles/src"]
  :modules {:dirs ["checkouts/common"
                   "checkouts/users"
                   "checkouts/vehicles"
                   "checkouts/scheduler"
                   "checkouts/features"
                   "checkouts/gallery"]}
  :env {:vehicles-db "carbook_vehicles"
        :users-db "carbook_users"
        :scheduler-db "carbook_scheduler"
        :cron-expr "0 * * ? * *"
        :db-user "constantine"
        :db-password "123123q"}
  :profiles {:uberjar {:aot :all}}
  :aot :all
  :main services.core)
