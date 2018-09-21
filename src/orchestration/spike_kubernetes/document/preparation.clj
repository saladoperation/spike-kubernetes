(ns spike-kubernetes.document.preparation
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def test-ids
  #{19961})

(def validation-ids
  #{74830})

(def evaluation-ids
  (set/union test-ids validation-ids))

(def get-organized-path
  (partial helpers/get-dataset-path "organized"))

(def get-evaluation-organized-path
  (comp get-organized-path
        (partial (aid/flip str) ".edn")))

(def test-name
  "test")

(def validation-name
  "validation")

(def make-spit-evaluation
  #(comp (partial spit (get-evaluation-organized-path %))
         vec
         (partial mapcat :text)
         (partial filter (comp ({test-name       test-ids
                                 validation-name validation-ids} %)
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
  (fs/delete-dir (get-organized-path))
  (->> "extracted"
       helpers/get-dataset-path
       helpers/get-files
       (mapcat (comp (partial map (comp (partial s/transform*
                                                 :text
                                                 helpers/structure-document)
                                        helpers/parse-keywordize))
                     str/split-lines
                     slurp))
       ((apply juxt
               (comp (partial run!
                              (aid/build spit-edn-lines+
                                         (comp (partial get-organized-path
                                                        "training")
                                               (partial (aid/flip str)
                                                        ".txt")
                                               :id)
                                         :text))
                     (partial remove (comp evaluation-ids
                                           edn/read-string
                                           :id)))
               (map make-spit-evaluation [test-name validation-name])))))
