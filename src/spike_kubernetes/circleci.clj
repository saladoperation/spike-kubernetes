(ns spike-kubernetes.circleci
  (:require [aid.core :as aid]
            [cats.core :as m]
            [environ.core :refer [env]]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.command :as command]))

(aid/defcurried effect
                [f x]
                (f x)
                x)

(defn run-circleci
  []
  (kubernetes/spit-kubernetes)
  (m/>>= (->> (concat (map (partial apply command/lein)
                           [["test"]
                            ["cljsbuild" "once" "prod"]
                            ["uberjar"]])
                      (map (partial apply command/docker)
                           [["build"
                             "-f"
                             "docker/clojure/Dockerfile"
                             "-t"
                             helpers/clojure-image
                             "."]
                            ["run" "-d" helpers/clojure-image]
                            ["login"
                             "-u"
                             helpers/username
                             "-p"
                             (:docker-password env)]]))
              (effect println)
              (apply m/>>))
         #(aid/casep env
                     :circle-tag (command/docker "push" helpers/clojure-image)
                     (m/return %))))
