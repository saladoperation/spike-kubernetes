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

(def id
  (first *command-line-args*))

(def compiler
  ({"web"   {:output-to  "dev-resources/public/js/main.js"
             :output-dir "dev-resources/public/js/out"
             :main       "spike_kubernetes.web"
             :asset-path "/js/out"}
    "serve" {:output-to "target/main.js"
             :main      "spike_kubernetes.serve"
             :target    :nodejs}} id))

(def build
  (helpers/deep-merge build-template {:id       id
                                      :compiler compiler}))

(repl-api/start-figwheel! {:build-ids        [id]
                           :all-builds       [build]
                           :figwheel-options {:server-port ((case id
                                                              "serve" inc
                                                              identity)
                                                             3449)}})

(repl-api/cljs-repl id)
