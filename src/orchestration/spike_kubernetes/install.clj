(ns spike-kubernetes.install
  (:require [cats.core :as m]
            [com.rpl.specter :as s]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def bash-c
  (comp (partial command/bash "-c")
        #(str "\"" % "\"")))

(def install-apt
  #(m/>> (command/curl "-sL"
                       "https://deb.nodesource.com/setup_8.x"
                       "|"
                       "sudo"
                       "-E"
                       "bash"
                       "-")
         (->> helpers/apt-commands
              (s/setval [s/ALL s/BEFORE-ELEM] "sudo")
              helpers/get-shell-script
              bash-c)))

(def install-python
  #(-> [helpers/conda-command helpers/source-command helpers/spacy-command]
       helpers/get-shell-script
       bash-c))
