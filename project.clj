(defproject spike-kubernetes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/test.check "0.9.0"]
                 [aid "0.1.1"]
                 [aysylu/loom "1.0.2"]
                 [binaryage/oops "0.6.2"]
                 [buddy "2.0.0"]
                 [cheshire "5.8.0"]
                 [clj-http "3.9.1"]
                 [cljs-ajax "0.7.4"]
                 [com.rpl/specter "1.1.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [compliment "0.3.6"]
                 [environ "1.1.0"]
                 [funcool/cats "2.2.0"]
                 [hiccup "1.0.5"]
                 [incanter "1.9.3"]
                 [macchiato/core "0.2.14"]
                 [me.raynes/fs "1.4.6"]
                 [mount "0.1.13"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.immutant/immutant "2.1.10"]
                 [reagent "0.8.1"]
                 [reagent-forms "0.5.42"]
                 [ring "1.6.3"]]
  :plugins [[com.jakemccrary/lein-test-refresh "0.23.0"]
            [lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.5"]
            [lein-doo "0.1.10"]
            [lein-exec "0.3.7"]
            [lein-npm "0.6.2"]]
  :main spike-kubernetes.core
  :source-paths ["src/orchestration"]
  :test-paths ["test/orchestration"]
  :target-path "target/%s"
  :uberjar-name "main.jar"
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.16"]]}}
  :cljsbuild
  {:builds
   {:orchestration {:source-paths ["src/orchestration"]
                    :compiler     {:output-to     "resources/public/js/main.js"
                                   :optimizations :advanced
                                   :main          spike_kubernetes.core}}
    :alteration    {:source-paths ["src/alteration"]
                    :compiler     {:output-to  "target/prod/main.js"
                                   ;deleting :output-dir and setting :optimizations :advanced gives the following error.
                                   ;Error: Cannot find module 'react'
                                   :output-dir "target/prod/out"
                                   :main       spike_kubernetes.core
                                   :target     :nodejs
                                   ;Not using lein npm install gives the following error.
                                   ;Error: Cannot find module 'cookies'
                                   :npm-deps   {:en-inflectors "1.0.12"}}}
    :test          {:source-paths ["src/alteration" "test"]
                    :compiler     {:output-to  "target/test/main.js"
                                   :output-dir "target/test/out"
                                   :main       spike_kubernetes.test.core
                                   :target     :nodejs
                                   :npm-deps   {:en-inflectors "1.0.12"}}}}}
  :npm {:dependencies [[en-inflectors "1.0.12"]
                       [nodemon "1.18.4"]
                       [ws "6.0.0"]]
        :package      {:scripts {:nodemon "nodemon"}}})
