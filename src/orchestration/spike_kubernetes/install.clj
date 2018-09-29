(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [com.rpl.specter :as s]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def run-commands
  (comp (partial command/bash "-c")
        #(str "\"" % "\"")
        helpers/get-shell-command))

(def install-apt
  #(m/>> (command/curl "-sL"
                       "https://deb.nodesource.com/setup_8.x"
                       "|"
                       "sudo"
                       "-E"
                       "bash"
                       "-")
         (->> helpers/apt-commands
              (map (partial cons "sudo"))
              run-commands)))

(def install-conda
  #(-> [helpers/conda-command helpers/source-command helpers/spacy-command]
       run-commands))

(def container-name
  "rabbitmq")

(def install-docker
  #(->> [["kill" container-name]
         ["rm" container-name]
         ["run"
          "-d"
          "--name"
          container-name
          "-p"
          (helpers/get-forwarding 5672)
          "-p"
          (helpers/get-forwarding 15672)
          "rabbitmq:3-management"]]
        (map (partial concat ["sudo" "docker"]))
        run-commands))

(def install
  #(m/>> (install-apt)
         (install-conda)
         (helpers/install-npm)
         (install-docker)))
