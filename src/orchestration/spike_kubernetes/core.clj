(ns spike-kubernetes.core
  (:gen-class)
  (:require [aid.core :as aid]
            [cats.monad.either :as either]
            [mount.core :as mount]
            [spike-kubernetes.circleci :as circleci]
            [spike-kubernetes.document.learning :as learning]
            [spike-kubernetes.document.preparation :as document-preparation]
            [spike-kubernetes.document.tuning :as tuning]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.preparation :as preparation]))

(defn -main
  [command & more]
  (aid/case-eval
    command
    helpers/preparation-name
    ((aid/case-eval
       (first more)
       helpers/document-name document-preparation/prepare
       preparation/prepare))
    "tuning" (tuning/tune)
    "learning" (learning/learn)
    helpers/orchestration-name (do (require 'spike-kubernetes.orchestration)
                                   (mount/start))
    helpers/kubernetes-name (do (kubernetes/spit-kubernetes)
                                (shutdown-agents))
    (System/exit (aid/casep (circleci/run-circleci)
                            either/right? 0
                            1)))
  (shutdown-agents))
