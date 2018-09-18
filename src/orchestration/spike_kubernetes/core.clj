(ns spike-kubernetes.core
  (:gen-class)
  (:require [aid.core :as aid]
            [mount.core :as mount]
            [cats.monad.either :as either]
            [spike-kubernetes.circleci :as circleci]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.preparation :as prepare]))

(defn -main
  [command & more]
  (aid/case-eval
    command
    helpers/preparation-name (prepare/prepare)
    helpers/orchestration-name (do (require 'spike-kubernetes.orchestration)
                                   (mount/start))
    helpers/kubernetes-name (do (kubernetes/spit-kubernetes)
                                (shutdown-agents))
    (System/exit (aid/casep (circleci/run-circleci)
                            either/right? 0
                            1))))
