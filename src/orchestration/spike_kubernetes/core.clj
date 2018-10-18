(ns spike-kubernetes.core
  (:gen-class)
  (:require [aid.core :as aid]
            [cats.monad.either :as either]
            [clojure.string :as str]
            [mount.core :as mount]
            [spike-kubernetes.circleci :as circleci]
            [spike-kubernetes.document.preparation :as document-preparation]
            [spike-kubernetes.document.tuning :as tuning]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.install :as install]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.preparation :as preparation]))

(def get-namespace
  (comp symbol
        first
        (partial (aid/flip str/split) #"/")
        str))

(defmacro require-call
  [qualified & more]
  `(do (-> ~qualified
           get-namespace
           require)
       ((resolve ~qualified) ~@more)))

(defn -main
  [& more]
  (aid/case-eval
    (first more)
    "install" (install/install)
    helpers/preparation-name
    ((aid/case-eval
       (second more)
       helpers/document-name document-preparation/prepare
       preparation/prepare))
    "tuning" (tuning/tune)
    "learning" (require-call 'spike-kubernetes.document.learning/learn)
    helpers/kubernetes-name ((juxt kubernetes/spit-kubernetes
                                   shutdown-agents))
    "circleci" (System/exit (aid/casep (circleci/run-circleci)
                                       either/right? 0
                                       1))
    (do (require 'spike-kubernetes.orchestration)
        (mount/start)))
  (shutdown-agents))
