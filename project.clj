(defproject spike-kubernetes "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [fogus/ring-edn "0.3.0"]
                 [hiccup "1.0.5"]
                 [mount "0.1.13"]
                 [org.immutant/immutant "2.1.10"]
                 [reagent "0.8.1"]
                 [ring "1.6.3"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.5"]]
  :main ^:skip-aot spike-kubernetes.core
  :target-path "target/%s"
  :cljsbuild {:builds
              {:prod
               {:source-paths ["src"]
                :compiler     {:output-to     "resources/public/js/main.js"
                               :main          spike_kubernetes.web
                               :optimizations :advanced}}}}
  :profiles {:dev     {:dependencies [[binaryage/devtools "0.9.10"]
                                      [figwheel-sidecar "0.5.14"]]}
             :uberjar {:uberjar-name "main.jar"}})
