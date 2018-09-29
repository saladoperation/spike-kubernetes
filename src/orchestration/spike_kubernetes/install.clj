(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def run-commands
  (comp (partial command/bash "-c")
        #(str "\"" % "\"")
        helpers/get-shell-command))

(def sudo-name
  "sudo")

(def install-apt
  #(m/>> (command/curl "-sL"
                       "https://deb.nodesource.com/setup_8.x"
                       "|"
                       sudo-name
                       "-E"
                       "bash"
                       "-")
         (->> helpers/apt-commands
              (map (partial cons sudo-name))
              run-commands)))

(def install-conda
  #(-> [helpers/conda-command helpers/source-command helpers/spacy-command]
       run-commands))

(def container-name
  "rabbitmq")

(def port-option-name
  "-p")

(def install-docker
  #(->> [["kill" container-name]
         ["rm" container-name]
         ["run"
          "-d"
          "--name"
          container-name
          port-option-name
          (helpers/get-forwarding 5672)
          port-option-name
          (helpers/get-forwarding 15672)
          "rabbitmq:3-management"]]
        (map (partial concat [sudo-name "docker"]))
        run-commands))

(def install
  #(m/>> (install-apt)
         (install-conda)
         (helpers/install-npm)
         (install-docker)))
