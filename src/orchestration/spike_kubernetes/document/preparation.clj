(ns spike-kubernetes.document.preparation
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]
            [spike-kubernetes.random :as random]))

(def downloaded-path
  (helpers/get-dataset-path "downloaded"))

(defn download
  []
  (fs/delete-dir downloaded-path)
  (helpers/with-sh-dir+ downloaded-path
                        (->> helpers/hyperparameter
                             :dataset
                             helpers/get-cloud-storage-path
                             helpers/get-download-argument
                             (apply command/wget))))

(def test-ids
  #{19961})

(def validation-ids
  #{74830})

(def evaluation-ids
  (set/union test-ids validation-ids))

(def make-spit-evaluation
  #(comp (partial spit (helpers/get-evaluation-path %))
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

(defn organize
  []
  (fs/delete-dir (helpers/get-organized-path))
  (->>
    "extracted"
    helpers/get-dataset-path
    helpers/get-files
    random/shuffle
    (mapcat (comp (partial map (comp (partial s/transform*
                                              :text
                                              helpers/structure-document)
                                     helpers/parse-keywordize))
                  random/shuffle
                  str/split-lines
                  slurp))
    ((apply juxt
            (comp (juxt (partial run!
                                 (aid/build spit-edn-lines+
                                            (comp helpers/get-training-path
                                                  (partial (aid/flip str)
                                                           ".txt")
                                                  first)
                                            last))
                        (comp (partial spit helpers/length-path)
                              (partial apply hash-map)
                              (partial mapcat (juxt first
                                                    (comp count
                                                          last)))))
                  (partial map-indexed vector)
                  (partial map :text)
                  (partial remove (comp evaluation-ids
                                        edn/read-string
                                        :id)))
            (map make-spit-evaluation [:test :validation])))))
