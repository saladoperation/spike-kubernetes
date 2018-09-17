(ns spike-kubernetes.core
  (:gen-class)
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [mount.core :as mount]
            [cats.monad.either :as either]
            [spike-kubernetes.circleci :as circleci]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]))

(defn -main
  [command & more]
  (aid/case-eval
    command
    helpers/orchestrate-name (do (require 'spike-kubernetes.orchestration)
                                 (mount/start))
    helpers/kubernetes-name (do (kubernetes/spit-kubernetes)
                                (shutdown-agents))
    (System/exit (aid/casep (circleci/run-circleci)
                            either/right? 0
                            1))))
