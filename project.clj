(defproject spike-kubernetes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [aid "0.1.1"]
                 [binaryage/oops "0.6.2"]
                 [cheshire "5.8.0"]
                 [com.rpl/specter "1.1.1"]
                 [environ "1.1.0"]
                 [fogus/ring-edn "0.3.0"]
                 [funcool/cats "2.2.0"]
                 [hiccup "1.0.5"]
                 [macchiato/core "0.2.14"]
                 [me.raynes/fs "1.4.6"]
                 [mount "0.1.13"]
                 [org.immutant/immutant "2.1.10"]
                 [reagent "0.8.1"]
                 [ring "1.6.3"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.5"]
            [lein-exec "0.3.7"]
            [lein-npm "0.6.2"]]
  :main spike-kubernetes.core
  :target-path "target/%s"
  :uberjar-name "main.jar"
  :profiles {:dev {:dependencies [[environ "1.1.0"]
                                  [binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.14"]]}}
  :cljsbuild
  {:builds
   {:clojure       {:source-paths ["src"]
                    :compiler     {:output-to     "resources/public/js/main.js"
                                   :optimizations :advanced
                                   :main          spike_kubernetes.clojure}}
    :clojurescript {:source-paths ["src"]
                    :compiler     {:output-to  "target/prod/main.js"
                                   ;deleting :output-dir and setting :optimizations :advanced gives the following error.
                                   ;Error: Cannot find module 'react'
                                   :output-dir "target/prod/out"
                                   :main       spike_kubernetes.clojurescript
                                   :target     :nodejs
                                   ;Not using lein npm install gives the following error.
                                   ;Error: Cannot find module 'cookies'
                                   :npm-deps   {:en-inflectors "1.0.12"}}}}}
  :npm {:dependencies [[en-inflectors "1.0.12"]
                       [ws "6.0.0"]]})
