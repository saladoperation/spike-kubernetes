(ns spike-kubernetes.install
  (:require [clojure.java.shell :as sh]
            [aid.core :as aid]
            [cats.core :as m]
            [cats.monad.either :as either]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def apt-get
  (partial command/sudo "apt-get"))

(def install-apt
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

(defn python
  [& more]
  (sh/with-sh-dir helpers/python-name
                  (apply command/export
                         "PYTHONPATH=$(pwd)"
                         "&&"
                         "source"
                         "activate"
                         "spike-kubernetes"
                         "&&"
                         helpers/python-name
                         more)))

(def install-python
  #(m/>> (apply command/conda helpers/conda-arguments)
         (python "-m" "spacy" "download" "en")))
