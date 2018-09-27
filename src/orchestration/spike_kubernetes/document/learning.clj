(ns spike-kubernetes.document.learning
  (:require [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]
            [aid.core :as aid]
            [cheshire.core :refer :all]
            [com.rpl.specter :as s]
            [compliment.utils :as utils]
            [immutant.web :as web]
            [langohr.basic :as lb]
            [langohr.channel :as lc]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [me.raynes.fs :as fs]
            [mount.core :refer [defstate]]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(utils/defmemoized get-length
                   []
                   (-> helpers/length-path
                       slurp
                       edn/read-string))

(def partition-into-batches
  #(->> %
        (partition (-> (get-length)
                       keys
                       count
                       (quot (:batch-size (helpers/get-document-tuned)))))
        (take (:batch-size (helpers/get-document-tuned)))))

(def get-gaps
  #(->> %
        :file
        (map (comp (get-length)
                   edn/read-string
                   (partial (aid/flip fs/base-name) true)))
        (reductions +)
        (cons 0)
        (map (partial -
                      (+ (:step-size (helpers/get-document-tuned))
                         (:token-offset %))))
        (take-while (complement neg?))))

(def get-document-offset*
  (comp dec
        count
        get-gaps))

(def get-file
  (aid/build drop
             get-document-offset*
             :file))

(def get-token-offset
  (comp last
        get-gaps))

(def get-document-offset
  (aid/build +
             get-document-offset*
             :document-offset))

(defn get-step
  [m]
  (->> (str "[" (->> m
                     :file
                     (take (-> m
                               get-document-offset*
                               inc))
                     (map slurp)
                     command/join-newline
                     str/split-lines
                     (drop (:token-offset m))
                     (take (:step-size (helpers/get-document-tuned)))
                     str/join)
            "]")
       edn/read-string
       (s/transform [s/ALL :source] helpers/get-document-index)
       helpers/concatenate-into-vector
       (merge {:file            (get-file m)
               :document-offset (get-document-offset m)
               :token-offset    (get-token-offset m)})))

(def get-training-steps
  (comp
    (partial map (comp generate-string
                       (partial (aid/flip dissoc) :file)))
    (aid/build (partial map (partial s/setval* :global_step))
               (comp #(map (partial + %) helpers/integers)
                     :global_step)
               (comp (partial map helpers/concatenate-into-vector)
                     rest
                     (partial iterate (partial map get-step))
                     helpers/separate
                     (helpers/transfer* :file
                                        #(->> (helpers/get-training-path)
                                              helpers/get-files
                                              partition-into-batches
                                              (map drop
                                                   (:document-offset %))))))))

(def queue-name
  "queue")

(defstate connection
          :start (rmq/connect)
          :stop (rmq/close connection))

(defstate channel
          :start (lc/open connection))

(def publish-if-zero!
  #(if (->> queue-name
            (lq/message-count channel)
            zero?)
     (lb/publish channel "" queue-name %)
     (recur %)))

(def get-evaluation-tokens
  (comp edn/read-string
        slurp
        helpers/get-evaluation-path))

(def generate-reference
  (comp str/join
        :text_with_ws))

(def get-precision
  (comp (partial apply /)
        (partial map count)
        (juxt (partial filter (aid/build =
                                         :inference
                                         :reference))
              identity)
        (partial remove :mask)
        helpers/separate))

(def handler
  ;TODO implement this function
  #(get-evaluation-tokens :validation))

(def start
  (partial web/run handler helpers/option))

(defstate server
          :start (start)
          :stop (web/stop))
