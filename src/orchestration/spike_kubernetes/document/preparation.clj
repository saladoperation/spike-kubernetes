(ns spike-kubernetes.document.preparation
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [aid.core :as aid]
            [me.raynes.fs :as fs]
            [taoensso.nippy :as nippy]
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

(def structure-document-text
  (comp helpers/structure-document
        :text))

(def make-spit-evaluation
  #(comp (partial spit (helpers/get-evaluation-path %))
         vec
         (partial mapcat structure-document-text)
         (partial filter (comp ({:test       test-ids
                                 :validation validation-ids} %)
                               edn/read-string
                               :id))))

(def freeze-to-file+
  (helpers/make-+ nippy/freeze-to-file))

(def count-lines
  #(with-open [x (io/reader %)]
     (-> x
         line-seq
         count)))

(def organize*
  #(with-open [x (io/reader random-path)]
     (->> x
          line-seq
          (map helpers/parse-keywordize)
          %)))

(defn organize
  []
  (fs/delete-dir (helpers/get-organized-path))
  (->>
    (map make-spit-evaluation [:test :validation])
    (cons
      (comp
        (partial run!
                 ;Clojure seems to be the bottleneck in learning/learn.
                 ;Nippy and memoization seems to be faster than JSON Lines.
                 ;Nippy and memoization
                 ;(-> (get-initial-step)
                 ;    get-training-steps
                 ;    first
                 ;    count
                 ;    time)
                 ;"Elapsed time: 815.124125 msecs"
                 ;=> 2021813
                 ;JSON Lines
                 ;(-> (get-initial-step)
                 ;    get-training-steps
                 ;    first
                 ;    count
                 ;    time)
                 ;"Elapsed time: 5525.949577 msecs"
                 ;=> 2026656
                 (aid/build freeze-to-file+
                            (comp helpers/get-training-path
                                  (partial (aid/flip helpers/append-extension)
                                           helpers/txt-name)
                                  first)
                            last))
        (partial map-indexed vector)
        (partial map structure-document-text)
        (partial remove (comp evaluation-ids
                              edn/read-string
                              :id))))
    (run! organize*))
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
