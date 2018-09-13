(ns spike-kubernetes.core
  (:gen-class)
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [mount.core :as mount]
            [cats.monad.either :as either]))

(def get-namespace
  (comp symbol
        first
        (partial (aid/flip str/split) #"/")
        str))

(defmacro require-call
  [qualified]
  `(do (-> ~qualified
           get-namespace
           require)
       ((resolve ~qualified))))

(defn -main
  [command & more]
  (case command
    "serve" (do (require 'spike-kubernetes.serve)
                (mount/start))
    "kubernetes"
    (do (require-call 'spike-kubernetes.kubernetes/spit-kubernetes)
        (shutdown-agents))
    (System/exit
      (aid/casep (require-call 'spike-kubernetes.circleci/run-circleci)
                 either/right? 0
                 1))))
