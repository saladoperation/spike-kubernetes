(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def apt-get
  (partial command/sudo "apt-get"))

(def apt-packages
  #{"inotify-tools"
    "nodejs"
    "silversearcher-ag"
    "tmux"
    "vim"})

(def install-apt
  #(m/>> (apt-get "update")
         (command/curl "-sL"
                       "https://deb.nodesource.com/setup_8.x"
                       "|"
                       "sudo"
                       "-E"
                       "bash"
                       "-")
         (apply apt-get "install" "-y" apt-packages)))

(def bash-c
  (comp (partial command/bash "-c")
        #(str "\"" % "\"")))

(def install-python
  #(-> helpers/python-commands
       helpers/get-shell-script
       bash-c))
