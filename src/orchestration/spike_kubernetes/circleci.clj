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

(def jar-name
  "main.jar")

(def generate-dockerfile
  (comp helpers/join-lines
        (partial map command/join-lexemes)
        (partial s/transform* [s/ALL s/ALL vector?] generate-string)))

(def get-code-path
  (partial helpers/get-path "code"))

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
  (partial helpers/get-path "target"))

(def java-name
  "java")

(def uberjar-name
  "uberjar")

(def java-image
  (str
    java-name
    ":8u111-jdk@sha256:c1ff613e8ba25833d2e1940da0940c3824f03f802c449f3d1815a66b7f8c0e9d"))

(def orchestration-dockerfile
  (get-dockerfile {:image    java-image
                   :from-tos #{[(get-target-path uberjar-name jar-name)
                                (get-code-path jar-name)]}
                   :port     helpers/orchestration-port
                   :cmd      [java-name "-jar" jar-name helpers/serve-name]}))

(def get-from-tos
  (partial map (comp (partial s/transform* s/LAST get-code-path)
                     (partial repeat 2))))

(def node-modules-name
  "node_modules")

(def get-prod-path
  (partial get-target-path "prod"))

(def node-name
  "node")

(def node-image
  (str
    node-name
    ":8.11.4@sha256:fd3c42d91fcf6019eec4e6ccd38168628dd4660992a1550a71c7a7e2b0dc2bdd"))

(def main-path
  (get-prod-path "main.js"))

(def alteration-cmd
  [node-name (get-prod-path "main.js")])

(def alteration-dockerfile
  (get-dockerfile {:image    node-image
                   :from-tos (get-from-tos #{(get-prod-path) node-modules-name})
                   :port     helpers/alteration-port
                   :cmd      alteration-cmd}))

(def python-name
  "python")

(def script-name
  "script")

(def conda-image
  "continuumio/miniconda:4.5.4@sha256:19d3eedab8b6301a0e1819476cfc50d53399881612183cf65208d7d43db99cd9")

(def get-shell-script
  (comp (partial str/join " && ")
        (partial map command/join-lexemes)))

(def get-python-dockerfile
  #(get-dockerfile
     {:image    conda-image
      :from-tos (get-from-tos #{python-name script-name})
      :run      (get-shell-script [["conda"
                                    "env"
                                    "create"
                                    "-f"
                                    (helpers/get-path python-name
                                                      "environments/cpu.yml")]
                                   ["source" "activate" "spike-kubernetes"]
                                   [python-name "-m" "spacy" "download" "en"]])
      :port     %
      :cmd      [(helpers/get-path script-name
                                   python-name
                                   (helpers/image-name %)
                                   "prod.sh")]}))

;docker doesn't seem to support symlinks for -f
;error unable to prepare context: unable to evaluate symlinks in Dockerfile path
;(def get-file
;  #(str "<(echo \"" % "\")"))

(def get-docker-path
  (partial helpers/get-resources-path "docker"))

(def get-dockerfile-path
  (partial (aid/flip get-docker-path) "Dockerfile"))

(defn get-build-command
  [s]
  ["build" "-f" (get-dockerfile-path s) "-t" (helpers/get-image s) "."])

(def build-clojurescript
  (partial command/lein "cljsbuild" "once"))

(def build-orchestration
  #(m/>> (build-clojurescript helpers/orchestration-name)
         (command/lein uberjar-name)))

(def build-alteration
  #(m/>> (command/lein "npm" "install")
         (build-clojurescript helpers/alteration-name)))

(def build-programs
  #(m/>> (build-orchestration)
         (build-alteration)
         ;This conditional reduces bandwidth usage.
         (aid/casep env
                    :circle-tag (install/install-word2vecf)
                    (either/right ""))))

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
  #(->> helpers/python-name
        keys
        (mapcat (juxt helpers/python-name
                      get-python-dockerfile))
        (apply array-map
               helpers/orchestration-name
               orchestration-dockerfile
               helpers/alteration-name
               alteration-dockerfile)
        (s/transform s/MAP-KEYS get-dockerfile-path)
        (run! (partial apply spit+))))

(def get-forwarding
  (comp (partial str/join ":")
        (partial repeat 2)))

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
                         (get-forwarding helpers/parse-port)
                         parse-image)
         (wait "localhost" helpers/parse-port)))

(def run-dependencies
  #(m/>> (command/node main-path "&")
         (run-parse)))

(def map->>
  (comp (partial apply m/>>)
        map))

(defn build-images
  []
  (spit-dockerfiles+)
  (->> helpers/image-name
       vals
       (map->> (comp (partial apply command/docker)
                     get-build-command))))

(def push
  #(m/>> (->> env
              :docker-password
              (command/docker "login" "-u" helpers/username "-p"))
         (->> helpers/image-name
              vals
              (map->> (comp (partial command/docker "push")
                            helpers/get-image)))))

(def test-commands
  [["test"] ["doo" node-name "test" "once"]])

(defn run-circleci
  []
  (kubernetes/spit-kubernetes)
  (timbre/with-level :trace
                     (timbre/spy (m/>>= (m/>> (build-programs)
                                              (build-images)
                                              (run-dependencies)
                                              (map->> (partial apply
                                                               command/lein)
                                                      test-commands))
                                        #(aid/casep env
                                                    :circle-tag (push)
                                                    (m/pure %))))))
