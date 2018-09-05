(ns spike-kubernetes.circleci
  (:require [aid.core :as aid]
            [cats.core :as m]
            [environ.core :refer [env]]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.command :as command]))

(defn get-build-run-command
  [image]
  [["build" "-f" "docker/clojure/Dockerfile" "-t" image "."]
   ["run" "-d" image]])

(def build-run-docker
  (comp (partial apply m/>>)
        (partial map (partial apply command/docker))
        get-build-run-command
        helpers/get-image))

(def build-clojurescript*
  (partial command/lein "cljsbuild" "once"))

(def build-clojure
  #(m/>> (build-clojurescript* helpers/clojure-name)
         (command/lein "uberjar")
         (build-run-docker helpers/clojure-name)))

(def build-clojurescript
  #(m/>> (build-clojurescript* helpers/clojurescript-name)
         (build-run-docker helpers/clojurescript-name)))

(def push
  (comp (partial command/docker "push")
        helpers/get-image))

(def all!
  (comp doall
        map))

(aid/defcurried effect
                [f x]
                (f x)
                x)

(defn run-circleci
  []
  (kubernetes/spit-kubernetes)
  (effect println
          (m/>>= (m/>> (command/docker "login"
                                       "-u"
                                       helpers/username
                                       "-p"
                                       (:docker-password env))
                       (command/lein "test")
                       (build-clojure)
                       (build-clojurescript))
                 #(aid/casep env
                             :circle-tag (->> [helpers/clojure-name
                                               helpers/clojurescript-name]
                                              (all! push)
                                              (apply m/>>))
                             (m/pure %)))))
