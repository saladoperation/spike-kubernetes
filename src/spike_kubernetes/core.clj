(ns spike-kubernetes.core
  (:gen-class)
  (:require [mount.core :as mount]))

(defn -main
  [command]
  (require 'spike-kubernetes.serve)
  (mount/start))
