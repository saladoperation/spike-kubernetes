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

(def uberjar
  "main.jar")

(def join-lines
  (partial str/join "\n"))

(def generate-dockerfile
  (comp join-lines
        (partial map (partial str/join " "))
        (partial s/transform* [s/ALL s/ALL vector?] generate-string)))

(defn get-dockerfile
  [{:keys [from from-tos port cmd]}]
  (generate-dockerfile (concat [["FROM" from]]
                               (map (partial s/setval*
                                             s/BEFORE-ELEM
                                             "COPY")
                                    from-tos)
                               [["EXPOSE" port]
                                ["CMD" cmd]])))

(def get-target-path
  (partial helpers/join-paths "target"))

(def clojure-dockerfile
  (get-dockerfile
    {:from     "clojure:lein-2.7.1@sha256:2c3fa51b875611e90f68490bc1ea7647edb05c9618420c920ba498f3ed174add"
     :from-tos #{[(get-target-path "uberjar" uberjar) uberjar]}
     :port     helpers/clojure-port
     :cmd      ["java" "-jar" uberjar "serve"]}))

(def javascript
  "main.js")

(def node-modules
  "node_modules")

(def clojurescript-dockerfile
  (get-dockerfile
    {:from     "node:8.11.4@sha256:fd3c42d91fcf6019eec4e6ccd38168628dd4660992a1550a71c7a7e2b0dc2bdd"
     :from-tos #{[(get-target-path "advanced" javascript) javascript]
                 (repeat 2 node-modules)}
     :port     helpers/clojurescript-port
     :cmd      ["node" javascript]}))

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
         (command/lein "uberjar")
         (build-run-docker helpers/clojure-name)))

(def build-clojurescript
  #(m/>> (build-clojurescript* helpers/clojurescript-name)
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
