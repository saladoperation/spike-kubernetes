(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [clojure.set :as set]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def sudo-name
  "sudo")

(def required-packages
  #{"nodejs"})

(def optional-packages
  #{"glances"
    "inotify-tools"
    "silversearcher-ag"
    "tmux"
    "vim"})

(def apt-packages
  (set/union required-packages optional-packages))

(def apt-commands
  (map (partial concat [sudo-name "apt-get"])
       [["update"]
        (concat ["install" "-y"] apt-packages)]))

(def install-apt
  #(helpers/>> (command/curl "-sL"
                             "https://deb.nodesource.com/setup_8.x"
                             "|"
                             sudo-name
                             "-E"
                             "bash"
                             "-")
               (helpers/run-commands apt-commands)))

(def dev-version
  8.0)

(def install-venv
  #(-> dev-version
       helpers/get-venv-commands
       helpers/run-commands))

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
        helpers/run-commands))

(def juxt->>
  (comp (partial comp (partial apply m/>>))
        juxt))

(def install
  (juxt->> install-apt
           helpers/install-word2vecf
           install-venv
           helpers/install-npm
           install-docker))
