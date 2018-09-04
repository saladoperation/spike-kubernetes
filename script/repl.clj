(ns repl
  (:require [figwheel-sidecar.repl-api :as repl-api]
            [spike-kubernetes.helpers :as helpers]))

(def build-template
  {:source-paths ["src"]
   :compiler     {:source-map-timestamp true
                  :preloads             ['devtools.preload]
                  :external-config      {:devtools/config
                                         {:features-to-install :all}}}
   :figwheel     true})

(def builds
  (map (partial helpers/deep-merge build-template)
       [{:id       "web"
         :compiler {:output-to  "dev-resources/public/js/main.js"
                    :output-dir "dev-resources/public/js/out"
                    :main       "spike_kubernetes.web"
                    :asset-path "/js/out"}}]))

(repl-api/start-figwheel!
  {:build-ids  (map :id builds)
   :all-builds builds})

(-> *command-line-args*
    second
    repl-api/cljs-repl)
