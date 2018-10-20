(ns repl
  (:require [aid.core :as aid]
            [figwheel-sidecar.repl-api :as repl-api]
            [spike-kubernetes.helpers :as helpers]))

(def id
  (first *command-line-args*))

(def output-directory
  "out")

(def get-public-js
  (partial helpers/get-path "dev-resources" "public" helpers/js-directory))

(def compiler*
  ({helpers/orchestration-name {:output-to  (get-public-js helpers/main-file)
                                :output-dir (get-public-js output-directory)
                                :asset-path (helpers/get-path
                                              helpers/js-directory
                                              output-directory)}
    helpers/alteration-name    {:output-to (helpers/get-path "target"
                                                             "dev"
                                                             helpers/main-file)
                                :target    :nodejs
                                :npm-deps  {:en-inflectors "1.0.12"}}}
    ;Not using lein npm install gives the following error.
    ;Error: Cannot find module 'cookies'
    id))

(def build
  {:id           id
   ;Having the code that targets node.js in the :source-paths for the code that targets browsers gives the following error.
   ;----  Could not Analyze  dev-resources/public/js/out/macchiato/fs.cljs  ----
   ;
   ;  No such namespace: fs, could not locate fs.cljs, fs.cljc, or JavaScript source providing "fs"
   ;
   ;----  Analysis Error : Please see dev-resources/public/js/out/macchiato/fs.cljs  ----
   :source-paths [(helpers/get-path "src" id)]
   :compiler     (helpers/deep-merge
                   {:main                 "spike_kubernetes.core"
                    :source-map-timestamp true
                    :preloads             ['devtools.preload]
                    :external-config      {:devtools/config
                                           {:features-to-install :all}}}
                   compiler*)
   :figwheel     true})

(repl-api/start-figwheel!
  {:build-ids        [id]
   :all-builds       [build]
   :figwheel-options {:server-port
                      ((aid/case-eval id
                                      helpers/alteration-name inc
                                      identity)
                        3449)}})

(repl-api/cljs-repl id)
