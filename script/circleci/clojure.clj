(ns clojure
  (:require [aid.core :as aid]
            [cats.core :as m]
            [environ.core :refer [env]]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]
            [cats.monad.either :as either]))

(aid/defcurried effect
                [f x]
                (f x)
                x)

(->> (concat (map (partial apply command/lein) [["test"]
                                                ["cljsbuild" "once" "prod"]
                                                ["uberjar"]])
             (map (partial apply command/docker)
                  (concat [["build"
                            "-f"
                            "docker/clojure/Dockerfile"
                            "-t"
                            helpers/clojure-image
                            "."]
                           ["run"
                            "-d"
                            helpers/clojure-image]]
                          (aid/casep env
                                     :circle-tag [["login"
                                                   "-u"
                                                   helpers/username
                                                   "-p"
                                                   (:docker-password env)]
                                                  ["push"
                                                   helpers/clojure-image]]
                                     []))))
     (apply m/>>)
     (effect println)
     (#(aid/casep %
                  either/right? 0
                  -1))
     System/exit)
