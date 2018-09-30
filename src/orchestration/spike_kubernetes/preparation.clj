(ns spike-kubernetes.preparation
  (:require [clojure.math.combinatorics :as combo]
            [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]
            [aid.core :as aid]
            [clj-http.client :as client]
            [com.rpl.specter :as s]
            [loom.graph :as graph]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def get-files
  (partial (aid/flip fs/find-files*) fs/file?))

(def parse-cell
  (command/if-then-else (partial (aid/flip str/starts-with?) "[")
                        edn/read-string
                        vector))

(def parse-line
  (comp (partial map parse-cell)
        (partial (aid/flip str/split) #"\t")))

(def arrange-line
  (partial s/transform*
           [s/ALL
            s/ALL]
           (comp helpers/set-remove-tokens
                 (partial map
                          (comp (partial s/setval*
                                         (s/multi-path :original
                                                       :proper
                                                       :quote)
                                         false)
                                helpers/set-forth-source)))))

(defn get-graph
  [f coll]
  (->> coll
       (map (partial apply f))
       (apply f)))

(def get-confusion
  #(->> ["directed" "undirected"]
        (map (comp (partial map (comp arrange-line
                                      (partial s/transform*
                                               [s/ALL
                                                s/ALL]
                                               helpers/parse-remotely)
                                      parse-line))
                   (partial mapcat str/split-lines)
                   (partial map slurp)
                   get-files
                   (partial helpers/get-resources-path
                            helpers/confusions-name)))
        (map map [(comp (partial apply combo/cartesian-product)
                        (partial split-at 1))
                  combo/permutations])
        (map get-graph [graph/digraph graph/graph])
        (apply graph/digraph)))

(def get-n-supremum
  #(->> (get-confusion)
        graph/nodes
        (map (comp count
                   first))
        (apply max)
        inc))

(def get-confusions
  #(->> (get-confusion)
        graph/edges
        (map (aid/build array-map
                        (comp (partial map :lemma_)
                              first
                              first)
                        vector))
        (apply merge-with concat)))

(def get-alternative
  #(->> (get-confusion)
        graph/nodes
        vec
        flatten
        (map :lower_)
        set
        (array-map :action :get-alternative :data)
        helpers/alter-remotely))

(def prepare
  #(spit (helpers/get-resources-path helpers/preparation-filename)
         {:n-supremum  (get-n-supremum)
          :confusions  (get-confusions)
          :alternative (get-alternative)}))
