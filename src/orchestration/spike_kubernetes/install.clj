(ns spike-kubernetes.install
  (:require [clojure.java.shell :as sh]
            [cats.core :as m]
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

(defn python
  [& more]
  (sh/with-sh-dir helpers/python-name
                  (-> [["PYTHONPATH=$(pwd)"]
                       helpers/source-command
                       (cons helpers/python-name more)]
                      helpers/get-shell-script
                      command/export)))

(def bash-c
  (comp (partial command/bash "-c")
        #(str "\"" % "\"")))

(def install-python
  #(m/>> (-> helpers/conda-command
             command/join-whitespace
             bash-c)
         (python "-m" "spacy" "download" "en")))
