(ns spike-kubernetes.circleci
  (:require [clojure.string :as str]
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
            [spike-kubernetes.install :as install]
            [spike-kubernetes.kubernetes :as kubernetes]))

(def jar
  "main.jar")

(def join-lines
  (partial str/join "\n"))

(def generate-dockerfile
  (comp join-lines
        (partial map (partial str/join " "))
        (partial s/transform* [s/ALL s/ALL vector?] generate-string)))

(def get-code-path
  (partial helpers/join-paths "code"))

(defn get-dockerfile
  [{:keys [image from-tos run port cmd] :or {run ":"}}]
  (generate-dockerfile (concat [["FROM" image]]
                               (map (partial s/setval* s/BEFORE-ELEM "COPY")
                                    from-tos)
                               [["WORKDIR" (get-code-path)]
                                ["RUN" ["/bin/bash" "-c" run]]
                                ["EXPOSE" port]
                                ["CMD" cmd]])))

(def get-target-path
  (partial helpers/join-paths "target"))

(def java
  "java")

(def uberjar
  "uberjar")

(def java-image
  (str
    java
    ":8u111-jdk@sha256:c1ff613e8ba25833d2e1940da0940c3824f03f802c449f3d1815a66b7f8c0e9d"))

(def orchestration-dockerfile
  (get-dockerfile {:image    java-image
                   :from-tos #{[(get-target-path uberjar jar)
                                (get-code-path jar)]}
                   :port     helpers/orchestration-port
                   :cmd      [java "-jar" jar "serve"]}))

(def clojure-image
  "circleci/clojure:lein-2.8.1-node@sha256:1d232488cb86d9174393202867bb2a4d1229edc7dd602eda69025fb99954c92a")

(def integration-dockerfile
  (get-dockerfile {:image    clojure-image
                   :from-tos #{["." (get-code-path)]}
                   :port     helpers/orchestration-port
                   :cmd      ["lein" "test"]}))

(def get-from-tos
  (partial map (comp (partial s/transform* s/LAST get-code-path)
                     (partial repeat 2))))

(def node-modules
  "node_modules")

(def get-prod-path
  (partial get-target-path "prod"))

(def node
  "node")

(def node-image
  (str
    node
    ":8.11.4@sha256:fd3c42d91fcf6019eec4e6ccd38168628dd4660992a1550a71c7a7e2b0dc2bdd"))

(def alteration-dockerfile
  (get-dockerfile {:image    node-image
                   :from-tos (get-from-tos #{(get-prod-path) node-modules})
                   :port     helpers/alteration-port
                   :cmd      [node (get-prod-path "main.js")]}))

(def python
  "python")

(def script
  "script")

(def conda-image
  "continuumio/miniconda:4.5.4@sha256:19d3eedab8b6301a0e1819476cfc50d53399881612183cf65208d7d43db99cd9")

(def get-shell-script
  (comp (partial str/join " && ")
        (partial map (partial str/join " "))))

(def get-python-dockerfile
  #(get-dockerfile
     {:image    conda-image
      :from-tos (get-from-tos #{python script})
      :run      (get-shell-script [["conda"
                                    "env"
                                    "create"
                                    "-f"
                                    (helpers/join-paths python
                                                        "environments/cpu.yml")]
                                   ["source" "activate" "spike-kubernetes"]
                                   [python "-m" "spacy" "download" "en"]])
      :port     %
      :cmd      [(helpers/join-paths script
                                     python
                                     (helpers/image-name %)
                                     "prod.sh")]}))

(def get-resources-path
  (comp (partial str/join "/")
        (partial vector "dev-resources")))

;docker doesn't seem to support symlinks for -f
;error unable to prepare context: unable to evaluate symlinks in Dockerfile path
;(def get-file
;  #(str "<(echo \"" % "\")"))

(def get-docker-path
  (partial get-resources-path "docker"))

(def get-dockerfile-path
  (partial (aid/flip get-docker-path) "Dockerfile"))

(defn get-build-command
  [s]
  ["build" "-f" (get-dockerfile-path s) "-t" (helpers/get-image s) "."])

(def build-clojurescript
  (partial command/lein "cljsbuild" "once"))

(def build-orchestration
  #(m/>> (build-clojurescript helpers/orchestration-name)
         (command/lein uberjar)))

(def build-alteration
  #(m/>> (command/lein "npm" "install")
         (build-clojurescript helpers/alteration-name)))

(defn make-+
  [f g]
  (comp (juxt (comp fs/mkdirs
                    fs/parent
                    f)
              (partial apply g))
        vector))

(def spit+
  (make-+ first spit))

(def integration-name
  "integration")

(def spit-dockerfiles+
  #(->> helpers/python-name
        keys
        (mapcat (juxt helpers/python-name
                      get-python-dockerfile))
        (apply array-map
               helpers/orchestration-name
               orchestration-dockerfile
               integration-name
               integration-dockerfile
               helpers/alteration-name
               alteration-dockerfile)
        (s/transform s/MAP-KEYS get-dockerfile-path)
        (run! (partial apply spit+))))

(def integration-image-name
  (s/setval helpers/orchestration-port integration-name helpers/image-name))

(def get-forwarding
  (comp (partial str/join ":")
        (partial repeat 2)))

(def docker
  "docker")

(defn get-run
  [port]
  [docker
   "run"
   (aid/case-eval port
                  helpers/orchestration-port ""
                  "-d")
   "-p"
   (get-forwarding port)
   (integration-image-name port)])

(def integration-ports
  [helpers/alteration-port helpers/parse-port helpers/orchestration-port])

(def docker-script-path
  "script/docker.sh")

(def docker-image-path
  (get-docker-path "image.tar"))

(defn spit-docker-script
  []
  (->> integration-ports
       (map get-run)
       (cons [docker "load" "<" docker-image-path])
       get-shell-script
       (spit docker-script-path))
  (command/chmod "+x" docker-script-path))

(def map->>
  (comp (partial apply m/>>)
        map))

(def save-command
  (concat ["save"]
          (map (comp helpers/get-image
                     integration-image-name)
               integration-ports)
          [">" docker-image-path]))

(defn run-circleci
  []
  (spit-dockerfiles+)
  (kubernetes/spit-kubernetes)
  (timbre/with-level
    :trace
    (timbre/spy
      (m/>>= (m/>> (spit-docker-script)
                   (->> env
                        :docker-password
                        (command/docker "login"
                                        "-u"
                                        helpers/username
                                        "-p"))
                   (build-orchestration)
                   (build-alteration)
                   ;This conditional reduces the bandwidth usage.
                   (aid/casep env
                              :circle-tag (install/install-word2vecf)
                              (either/right ""))
                   (->> helpers/image-name
                        vals
                        (cons integration-name)
                        (map->> (comp (partial apply command/docker)
                                      get-build-command)))
                   (command/lein "doo" node "test" "once")
                   (apply command/docker save-command))
             #(aid/casep env
                         :circle-tag (->> helpers/image-name
                                          vals
                                          (map->> (comp (partial command/docker
                                                                 "push")
                                                        helpers/get-image)))
                         (m/pure %))))))
