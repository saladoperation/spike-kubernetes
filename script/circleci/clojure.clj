(ns clojure
  (:require [aid.core :as aid]
            [cats.core :as m]
            [environ.core :refer [env]]
            [spike-kubernetes.command :as command]))

(def image
  (str (:docker-username env)
       "/"
       (ns-name *ns*)
       (aid/casep env
                  :circle-tag (->> env
                                   :circle-tag
                                   (str ":"))
                  "")))

(->> (concat (map (partial apply command/lein) [["test"]
                                                ["cljsbuild" "once" "prod"]
                                                ["uberjar"]])
             (map (partial apply command/docker)
                  (concat [["build"
                            "-f"
                            "docker/clojure/Dockerfile"
                            "-t"
                            image
                            "."]
                           ["run"
                            "-d"
                            image]]
                          (aid/casep env
                                     :circle-tag [["login"
                                                   "-u"
                                                   "-p"
                                                   (:docker-password env)]
                                                  ["push"
                                                   image]]
                                     []))))
     (apply m/>>)
     println)

(shutdown-agents)
