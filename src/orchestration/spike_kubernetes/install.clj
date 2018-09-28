(ns spike-kubernetes.install
  (:require [clojure.java.shell :as sh]
            [aid.core :as aid]
            [cats.core :as m]
            [cats.monad.either :as either]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

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

(def python-path
  "python")

(defn python
  [& more]
  (sh/with-sh-dir python-path
                  (apply command/export
                         "PYTHONPATH=$(pwd)"
                         "&&"
                         "source"
                         "activate"
                         "spike-kubernetes"
                         "&&"
                         "python"
                         more)))

(def install-python
  #(m/>> (->> "pu.yml"
              (str (aid/casep (command/nvidia-smi)
                              either/right "g"
                              "c"))
              (helpers/get-path python-path "environments")
              (command/conda "env" "create" "--force" "-f"))
         (python "-m" "spacy" "download" "en")))
