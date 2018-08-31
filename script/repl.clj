(ns repl
  (:require [figwheel-sidecar.repl-api :as repl-api]))

(def web-id
  "web")

(def builds
  [{:id           web-id
    :source-paths ["src"]
    :compiler     {:output-to            "dev-resources/public/js/main.js"
                   :output-dir           "dev-resources/public/js/out"
                   :main                 "spike_kubernetes.web"
                   :asset-path           "/js/out"
                   :source-map-timestamp true
                   :preloads             ['devtools.preload]
                   :external-config      {:devtools/config {:features-to-install :all}}}
    :figwheel     true}])

(repl-api/start-figwheel!
  {:build-ids  (map :id builds)
   :all-builds builds})

(repl-api/cljs-repl web-id)
