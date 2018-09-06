(ns repl
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [figwheel-sidecar.repl-api :as repl-api]
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

(def js-directory
  "js")

(def output-directory
  "out")

(def get-public-js
  (comp str
        (partial helpers/join-paths "dev-resources/public" js-directory)))

(def entry-point
  "main.js")

(def compiler*
  ({helpers/clojure-name       {:output-to  (get-public-js entry-point)
                                :output-dir (get-public-js output-directory)
                                :asset-path (helpers/join-paths
                                              js-directory
                                              output-directory)}
    helpers/clojurescript-name {:output-to (helpers/join-paths "target/dev"
                                                               entry-point)
                                :target    :nodejs
                                :npm-deps  {:en-inflectors "1.0.12"}}}
    ;Not using lein npm install gives the following error.
    ;Error: Cannot find module 'cookies'
    id))

(def build
  (helpers/deep-merge build-template
                      {:id       id
                       :compiler (s/setval :main
                                           (str "spike_kubernetes." id)
                                           compiler*)}))

(repl-api/start-figwheel!
  {:build-ids        [id]
   :all-builds       [build]
   :figwheel-options {:server-port
                      ((aid/case-eval id
                                      helpers/clojurescript-name inc
                                      identity)
                        3449)}})

(repl-api/cljs-repl id)
