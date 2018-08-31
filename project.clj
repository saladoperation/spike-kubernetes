(defproject spike-kubernetes "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [fogus/ring-edn "0.3.0"]
                 [mount "0.1.12"]
                 [org.immutant/immutant "2.1.10"]
                 [ring "1.6.3"]]
  :plugins [[lein-ancient "0.6.15"]]
  :main ^:skip-aot spike-kubernetes.core
  :target-path "target/%s"
  :profiles {:uberjar {:uberjar-name "main.jar"}})
