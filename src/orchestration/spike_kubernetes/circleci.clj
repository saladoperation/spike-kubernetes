(ns spike-kubernetes.circleci
  (:require [clojure.java.shell :as sh]
            [aid.core :as aid]
            [cats.core :as m]
            [cats.monad.either :as either]
            [cheshire.core :refer :all]
            [com.rpl.specter :as s]
            [environ.core :refer [env]]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as timbre]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]))

(def generate-dockerfile
  (comp command/join-newline
        (partial map command/join-whitespace)
        (partial s/transform* [s/ALL s/ALL vector?] generate-string)))

(def get-code-path
  (partial helpers/get-path "code"))

(defn get-dockerfile
  [{:keys [image from-tos runs port cmd] :or {runs [":"]}}]
  (generate-dockerfile (concat [["FROM" image]]
                               (map (partial s/setval* s/BEFORE-ELEM "COPY")
                                    from-tos)
                               [["WORKDIR" (get-code-path)]]
                               (map (partial vector "RUN") runs)
                               [["EXPOSE" port]
                                ["CMD" cmd]])))
;A single RUN seems to cause the following error.
;The command '/bin/bash -c apt update && apt install -y build-essential libffi-dev && conda env create -f python/environments/cpu.yml && source activate spike-kubernetes && python -m spacy download en' returned a non-zero code: 126
;/bin/bash: /opt/conda/envs/spike-kubernetes/bin/python: Invalid argument
;(defn get-dockerfile
;  [{:keys [image from-tos run port cmd] :or {run ":"}}]
;  (generate-dockerfile (concat [["FROM" image]]
;                               (map (partial s/setval* s/BEFORE-ELEM "COPY")
;                                    from-tos)
;                               [["WORKDIR" (get-code-path)]
;                                ["RUN" ["/bin/bash" "-c" run]]
;                                ["EXPOSE" port]
;                                ["CMD" cmd]])))

(def get-target-path
  (partial helpers/get-path "target"))

(def java-name
  "java")

(def uberjar-name
  "uberjar")

(def jar-path
  (get-target-path uberjar-name "main.jar"))

(def java-image
  (str
    java-name
    ":8u111-jdk@sha256:c1ff613e8ba25833d2e1940da0940c3824f03f802c449f3d1815a66b7f8c0e9d"))

(def get-from-tos
  (partial map (comp (partial s/transform* s/LAST get-code-path)
                     (partial repeat 2))))

(def orchestration-dockerfile
  (get-dockerfile {:image    java-image
                   :from-tos (get-from-tos #{jar-path
                                             (helpers/get-resources-path)})
                   :port     helpers/orchestration-port
                   :cmd      [java-name
                              "-jar"
                              jar-path
                              helpers/orchestration-name]}))

(def get-prod-path
  (partial get-target-path "prod"))

(def node-name
  "node")

(def node-image
  (str
    node-name
    ":8.11.4@sha256:fd3c42d91fcf6019eec4e6ccd38168628dd4660992a1550a71c7a7e2b0dc2bdd"))

(def main-path
  (get-prod-path helpers/main-file))

(def alteration-cmd
  [node-name main-path])

(def alteration-dockerfile
  (get-dockerfile {:image    node-image
                   :from-tos (get-from-tos #{(get-prod-path) "node_modules"})
                   :port     helpers/alteration-port
                   :cmd      alteration-cmd}))

(def script-name
  "script")

(def conda-image
  "continuumio/miniconda:4.5.4@sha256:19d3eedab8b6301a0e1819476cfc50d53399881612183cf65208d7d43db99cd9")

(def get-python-dockerfile
  #(get-dockerfile
     {:image    conda-image
      :from-tos (get-from-tos #{(helpers/get-resources-path)
                                helpers/python-name
                                script-name})
      :runs     (concat helpers/apt-commands
                        [helpers/conda-command
                         ["/bin/bash"
                          "-c"
                          (helpers/get-shell-command [helpers/source-command
                                                      helpers/spacy-command])]])
      :port     %
      :cmd      [(helpers/get-path script-name
                                   helpers/python-name
                                   (helpers/python-port-name %)
                                   "evaluation.sh")]}))

;docker doesn't seem to support symlinks for -f
;error unable to prepare context: unable to evaluate symlinks in Dockerfile path
;(def get-file
;  #(str "<(echo \"" % "\")"))

(def get-docker-path
  (partial helpers/get-resources-path "docker"))

(def get-dockerfile-path
  (partial (aid/flip get-docker-path) "Dockerfile"))

(defn get-build-arguments
  [s]
  ["build" "-f" (get-dockerfile-path s) "-t" (helpers/get-image s) "."])

(def build-clojurescript
  (partial command/lein "cljsbuild" "once"))

(def parse-image
  (helpers/get-image helpers/parse-name))

(defn wait
  [& more]
  (->> #(apply command/nc "-z" more)
       repeatedly
       (drop-while either/left?)
       first))

(def run-parse
  #(m/>> (command/docker "run"
                         "-d"
                         "-p"
                         (helpers/get-forwarding helpers/parse-port)
                         parse-image)
         (wait "localhost" helpers/parse-port)))

(def build-alteration
  #(m/>> (helpers/install-npm)
         (build-clojurescript helpers/alteration-name)))

(def run-dependencies
  #(m/>> (-> main-path
             command/node
             future
             either/right)
         (run-parse)))

(def build-orchestration
  #(m/>> (build-alteration)
         (run-dependencies)
         (command/lein "run" helpers/preparation-name)
         (build-clojurescript helpers/orchestration-name)
         (command/lein uberjar-name)))

(defn make-+
  [f g]
  (comp (juxt (comp fs/mkdirs
                    fs/parent
                    f)
              (partial apply g))
        vector))

(def spit+
  (make-+ first spit))

(def spit-dockerfiles+
  #(->> helpers/python-port-name
        keys
        (mapcat (juxt helpers/python-port-name
                      get-python-dockerfile))
        (apply array-map
               helpers/orchestration-name
               orchestration-dockerfile
               helpers/alteration-name
               alteration-dockerfile)
        (s/transform s/MAP-KEYS get-dockerfile-path)
        (run! (partial apply spit+))))

(def map->>
  (comp (partial apply m/>>)
        map))

(def push
  #(m/>> (->> env
              :docker-password
              (command/docker "login" "-u" helpers/username "-p"))
         (map->> (comp (partial command/docker "push")
                       helpers/get-image)
                 helpers/image-names)))

(def test-argument-collection
  [["test"] ["doo" node-name "test" "once"]])

(def get-tar-path
  (comp (partial helpers/get-path
                 "https://storage.googleapis.com/wikipediadataset")
        (partial (aid/flip helpers/append-extension) "tar")))

(defn get-download-extract-argument
  [s]
  ["-qO-" s "|" "tar" "x"])

(def download-extract-arguments
  (->> helpers/model-port-name
       vals
       (map (comp get-download-extract-argument get-tar-path))))

(def download-extract
  #(sh/with-sh-dir (helpers/get-resources-path)
                   (map->> (partial apply command/wget)
                           download-extract-arguments)))

(defn run-circleci
  []
  (kubernetes/spit-kubernetes)
  (spit-dockerfiles+)
  (timbre/with-level
    :trace
    (timbre/spy
      (m/>>= (m/>> (aid/casep env
                              :circle-tag (m/>> (download-extract)
                                                (helpers/install-word2vecf))
                              (either/right ""))
                   (->> helpers/parse-name
                        get-build-arguments
                        (apply command/docker))
                   (build-orchestration)
                   (build-alteration)
                   (map->> (partial apply
                                    command/lein)
                           test-argument-collection)
                   (map->> (comp (partial apply
                                          command/docker)
                                 get-build-arguments)
                           helpers/image-names))
             #(aid/casep env
                         :circle-tag (push)
                         (m/pure %))))))
