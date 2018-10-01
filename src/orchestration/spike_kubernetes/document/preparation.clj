(ns spike-kubernetes.document.preparation
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def downloaded-path
  (helpers/get-dataset-path "downloaded"))

(defn download
  []
  (fs/delete-dir downloaded-path)
  (helpers/with-sh-dir+ downloaded-path
                        (->> helpers/hyperparameter
                             :dataset
                             helpers/get-cloud-storage-path
                             helpers/get-download-arguments
                             (apply command/wget))))

(def extracted-path
  (helpers/get-dataset-path "extracted.txt"))

(def extract*
  #(->> %
        str
        (command/python (helpers/get-path helpers/python-name
                                          "WikiExtractor.py")
                        "--json"
                        "-o"
                        "-"
                        ">>"
                        extracted-path)))

(defn extract
  []
  (fs/delete extracted-path)
  (->> downloaded-path
       helpers/get-files
       (helpers/map->> extract*)))

(defn nondeterministically-shuf
  [from to]
  (command/shuf "--random-source" from "-o" to from))

(def random-path
  (helpers/get-dataset-path "random.txt"))

(def randomize
  #(nondeterministically-shuf extracted-path random-path))

(def test-ids
  #{19961})

(def validation-ids
  #{74830})

(def evaluation-ids
  (set/union test-ids validation-ids))

(def make-spit-evaluation
  #(comp (partial vector spit (helpers/get-evaluation-path %))
         vec
         (partial mapcat :text)
         (partial filter (comp ({:test       test-ids
                                 :validation validation-ids} %)
                               edn/read-string
                               :id))))

(defn spit-edn-lines
  [f coll]
  (->> coll
       (map str)
       command/join-newline
       (spit f)))

(def spit-edn-lines+
  (helpers/make-+ spit-edn-lines))

(def count-lines
  #(with-open [x (io/reader %)]
     (-> x
         line-seq
         count)))

(defn organize
  []
  (fs/delete-dir (helpers/get-organized-path))
  (with-open [x (io/reader random-path)]
    (->>
      x
      line-seq
      (map (comp (partial s/transform*
                          :text
                          helpers/structure-document)
                 helpers/parse-keywordize))
      ((aid/build
         concat
         (comp
           (partial
             map
             (juxt (constantly spit-edn-lines+)
                   (comp helpers/get-training-path
                         (partial (aid/flip helpers/append-extension)
                                  helpers/txt-name)
                         first)
                   last))
           (partial map-indexed vector)
           (partial map :text)
           (partial remove (comp evaluation-ids
                                 edn/read-string
                                 :id)))
         (apply juxt (map make-spit-evaluation [:test :validation]))))
      ;Retaining the head seems to give the following error.
      ;java.lang.OutOfMemoryError: GC overhead limit exceeded
      (run! (partial apply aid/funcall))))
  (->> (helpers/get-training-path)
       helpers/get-files
       (mapcat (juxt fs/base-name
                     count-lines))
       (apply hash-map)
       (spit helpers/length-path)))

(def prepare
  (juxt download
        extract
        randomize
        organize))
