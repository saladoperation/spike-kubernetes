(ns spike-kubernetes.install
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [aid.core :as aid]
            [buddy.core.codecs :refer :all]
            [buddy.core.hash :as hash]
            [cats.core :as m]
            [cats.monad.either :as either]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def cache-path
  "python/.vector_cache")

(def word2vecf-filename
  "deps.words.bz2")

(def word2vecf-url
  (str "http://u.cs.biu.ac.il/~yogo/data/syntemb/" word2vecf-filename))

(def word2vecf-sha
  "38281adc0a0ee1abf50c2cc6c90372bdef968cb08d4e3a2d4c68c2924b639e64")

(defmacro with-sh-dir+
  [path & body]
  `(do (fs/mkdirs ~path)
       (sh/with-sh-dir ~path
                       ~@body)))

(def install-word2vecf
  #(with-sh-dir+ cache-path
                 (m/>> (command/wget word2vecf-url)
                       (aid/case-eval (->> word2vecf-filename
                                           (helpers/get-path cache-path)
                                           io/input-stream
                                           hash/sha256
                                           bytes->hex)
                                      word2vecf-sha (either/right "")
                                      (either/left "Checksums don't match."))
                       (command/bzip2 "-df" word2vecf-filename))))
