(ns spike-kubernetes.circleci
  (:require [aid.core :as aid]
            [cats.core :as m]
            [cheshire.core :refer :all]
            [environ.core :refer [env]]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.kubernetes :as kubernetes]
            [spike-kubernetes.command :as command]
            [clojure.string :as str]
            [com.rpl.specter :as s]
            [me.raynes.fs :as fs]))

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
  [{:keys [image from-tos port cmd]}]
  (generate-dockerfile (concat [["FROM" image]]
                               (map (partial s/setval*
                                             s/BEFORE-ELEM
                                             "COPY")
                                    from-tos)
                               [["EXPOSE" port]
                                ["WORKDIR" (get-code-path)]
                                ["CMD" cmd]])))

(def get-target-path
  (partial helpers/join-paths "target"))

(def java
  "java")

(def uberjar
  "uberjar")

(def clojure-dockerfile
  (get-dockerfile
    {:image    (str java
                    ":8u111-jdk@sha256:c1ff613e8ba25833d2e1940da0940c3824f03f802c449f3d1815a66b7f8c0e9d")
     :from-tos #{[(get-target-path uberjar jar) (get-code-path jar)]}
     :port     helpers/clojure-port
     :cmd      [java "-jar" jar "serve"]}))

(def node-modules
  "node_modules")

(def get-prod-path
  (partial get-target-path "prod"))

(def node
  "node")

(def clojurescript-dockerfile
  (get-dockerfile
    {:image    (str node
                    ":8.11.4@sha256:fd3c42d91fcf6019eec4e6ccd38168628dd4660992a1550a71c7a7e2b0dc2bdd")
     :from-tos (map (comp (partial s/transform* s/LAST get-code-path)
                          (partial repeat 2))
                    #{(get-prod-path) node-modules})
     :port     helpers/clojurescript-port
     :cmd      [node (get-prod-path "main.js")]}))

(def get-resources-path
  (comp (partial str/join "/")
        (partial vector "dev-resources")))

;docker doesn't seem to support symlinks for -f
;error unable to prepare context: unable to evaluate symlinks in Dockerfile path
;(def get-file
;  #(str "<(echo \"" % "\")"))

(def get-dockerfile-path
  #(get-resources-path "docker" % "Dockerfile"))

(defn get-build-run-command
  [language]
  [["build"
    "-f"
    (get-dockerfile-path language)
    "-t"
    (helpers/get-image language) "."]
   ["run" "-d" (helpers/get-image language)]])

(def build-run-docker
  (comp (partial apply m/>>)
        (partial map (partial apply command/docker))
        get-build-run-command))

(def build-clojurescript*
  (partial command/lein "cljsbuild" "once"))

(def build-clojure
  #(m/>> (build-clojurescript* helpers/clojure-name)
         (command/lein uberjar)
         (build-run-docker helpers/clojure-name)))

(def build-clojurescript
  #(m/>> (command/lein "npm" "install")
         (build-clojurescript* helpers/clojurescript-name)
         (build-run-docker helpers/clojurescript-name)))

(def push
  (comp (partial command/docker "push")
        helpers/get-image))

(def all!
  (comp doall
        map))

(aid/defcurried effect
                [f x]
                (f x)
                x)

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
  #(->> {helpers/clojure-name       clojure-dockerfile
         helpers/clojurescript-name clojurescript-dockerfile}
        (s/transform s/MAP-KEYS get-dockerfile-path)
        (run! (partial apply spit+))))

(defn run-circleci
  []
  (spit-dockerfiles+)
  (kubernetes/spit-kubernetes)
  (effect println
          (m/>>= (m/>> (command/docker "login"
                                       "-u"
                                       helpers/username
                                       "-p"
                                       (:docker-password env))
                       (build-clojure)
                       (build-clojurescript)
                       (command/lein "test"))
                 #(aid/casep env
                             :circle-tag (->> [helpers/clojure-name
                                               helpers/clojurescript-name]
                                              (all! push)
                                              (apply m/>>))
                             (m/pure %)))))
