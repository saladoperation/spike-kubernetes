(ns spike-kubernetes.install
  (:require [clojure.java.shell :as sh]
            [cats.core :as m]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]))

(def cache-path
  "python/.vector_cache")

(def word2vecf-filename
  "deps.words.bz2")

(def word2vecf-url
  (str "http://u.cs.biu.ac.il/~yogo/data/syntemb/" word2vecf-filename))

(defmacro with-sh-dir+
  [path & body]
  `(do (fs/mkdirs ~path)
       (sh/with-sh-dir ~path
                       ~@body)))

(def install-word2vecf
  #(with-sh-dir+ cache-path (m/>> (command/wget word2vecf-url)
                                  (command/bzip2 "-df" word2vecf-filename))))
