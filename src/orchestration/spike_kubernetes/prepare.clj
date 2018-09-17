(ns spike-kubernetes.prepare
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

(def confusion
  (->> ["directed" "undirected"]
       (map (comp (partial map (comp arrange-line
                                     (partial s/transform*
                                              [s/ALL
                                               s/ALL]
                                              helpers/parse-remotely)
                                     parse-line))
                  (partial mapcat str/split-lines)
                  (partial map slurp)
                  get-files
                  (partial helpers/get-resources-path helpers/confusions-name)))
       (map map [(comp (partial apply combo/cartesian-product)
                       (partial split-at 1))
                 combo/permutations])
       (map get-graph [graph/digraph graph/graph])
       (apply graph/digraph)))

(def n-upperbound
  (->> confusion
       graph/nodes
       (map (comp count
                  first))
       (apply max)
       inc))

(def confusions
  (->> confusion
       graph/edges
       (map (aid/build array-map
                       (comp (partial map :lemma_)
                             first
                             first)
                       vector))
       (apply merge-with concat)))


(def get-edn-request
  (comp (partial array-map :as :clojure :body)
        pr-str))

(def post-macchiato
  (partial client/post (helpers/get-origin helpers/alteration-port)))

(def alternative
  (->> confusion
       graph/nodes
       vec
       flatten
       (map :lower_)
       set
       (array-map :action :get-alternative :data)
       get-edn-request
       post-macchiato
       :body))

(def prepare
  #(spit (helpers/get-path "resources" helpers/prepared-filename)
         {:n-upperbound n-upperbound
          :confusions   confusions
          :alternative  alternative}))
