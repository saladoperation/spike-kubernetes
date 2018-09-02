(ns clojure
  (:require [cats.core :as m]
            [spike-kubernetes.command :as command]))

(def tagged-name
  "relationship/spike-kubernetes")

(->> (concat (map (partial apply command/lein)
                  [["test"]
                   ["cljsbuild" "once" "prod"]
                   ["uberjar"]])
             (map (partial apply command/docker)
                  [["build"
                    "-f"
                    "docker/clojure/Dockerfile"
                    "-t"
                    tagged-name
                    "."]
                   ["run"
                    "-d"
                    tagged-name]]))
     (apply m/>>)
     println)

(shutdown-agents)
