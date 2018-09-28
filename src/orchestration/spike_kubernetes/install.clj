(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [spike-kubernetes.command :as command]))

(def apt-get
  (partial command/sudo "apt-get"))

(def apt-update-install
  #(m/>> (apt-get "update")
         (command/curl "-sL"
                       "https://deb.nodesource.com/setup_8.x"
                       "|"
                       "sudo"
                       "-E"
                       "bash"
                       "-")
         (apt-get "install"
                  "-y"
                  "inotify-tools"
                  "nodejs"
                  "silversearcher-ag"
                  "tmux"
                  "vim")))
