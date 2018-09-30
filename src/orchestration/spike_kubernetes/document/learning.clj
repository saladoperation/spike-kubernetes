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
            [ring.util.response :refer [response]]
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
                       (quot (-> helpers/document-name
                                 helpers/get-tuned
                                 :batch-size))))
        (take (-> helpers/document-name
                  helpers/get-tuned
                  :batch-size))))

(def get-gaps
  #(->> %
        :file
        (map (comp (get-length)
                   edn/read-string
                   (partial (aid/flip fs/base-name) true)))
        (reductions +)
        (cons 0)
        (map (partial -
                      (+ (-> helpers/document-name
                             helpers/get-tuned
                             :step-size)
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

(def get-step
  #(->> (str "[" (->> %
                      :file
                      (take (-> %
                                get-document-offset*
                                inc))
                      (map slurp)
                      command/join-newline
                      str/split-lines
                      (drop (:token-offset %))
                      (take (-> helpers/document-name
                                helpers/get-tuned
                                :step-size))
                      str/join)
             "]")
        edn/read-string
        (s/transform [s/ALL :source] helpers/get-document-index)
        helpers/concatenate-into-vector
        (merge {:file            (get-file %)
                :document-offset (get-document-offset %)
                :token-offset    (get-token-offset %)})))

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

(def set-precision
  (helpers/transfer* :precision get-precision))

(def get-validation-loss
  (comp :loss
        :validation))

(def get-training-minimum
  (comp :minimum
        :training))

(def get-minimum
  (command/if-then-else get-training-minimum
                        (aid/build min
                                   get-training-minimum
                                   get-validation-loss)
                        get-validation-loss))

(def get-checkpoints-path
  (partial helpers/get-run-path
           helpers/document-name
           "checkpoints"))

(def recent-name
  "recent")

(def log-recent
  #(-> recent-name
       (helpers/append-extension helpers/edn-name)
       get-checkpoints-path
       (spit (generate-string %))))

(def log-minimum
  #(if ((aid/build =
                   get-validation-loss
                   get-minimum)
         %)
     (->> [helpers/edn-name "pt"]
          (map (fn [extension]
                 (map (comp get-checkpoints-path
                            ((aid/flip helpers/append-extension) extension))
                      [recent-name "minimum"])))
          (run! (partial apply fs/copy)))))

(declare flatten-map)

(def flatten-entry
  #(->>
     %
     val
     (command/if-then-else map?
                           (comp (partial s/setval*
                                          [s/ALL s/BEFORE-ELEM]
                                          (key %))
                                 (command/if-then-else (comp (partial not-every?
                                                                      map?)
                                                             vals)
                                                       vec
                                                       flatten-map))
                           (partial vector (key %)))))

(def flatten-map
  (command/if-then map?
                   (partial mapcat flatten-entry)))

(def nest-vector
  (command/if-then-else (comp (partial = 1)
                              count)
                        first
                        (aid/build array-map
                                   first
                                   (comp nest-vector
                                         rest))))

(defn reorder-keys
  [f m]
  (->> m
       flatten-map
       (map (comp nest-vector
                  (aid/build s/transform*
                             (comp (partial s/srange 0)
                                   dec
                                   count)
                             (constantly f)
                             identity)))
       (apply helpers/deep-merge)))

(def get-training-global-step
  (comp :global_step
        :training))

(def log-tensorboard
  ;TODO implement this function
  (aid/build s/setval*
             (constantly [s/ALL s/AFTER-ELEM])
             get-training-global-step
             (comp vec
                   (partial (aid/flip select-keys) #{:loss :precision})
                   (partial reorder-keys reverse))))

(def make-get-file-to-generation
  #(juxt (comp (partial (aid/flip helpers/append-extension) helpers/txt-name)
               (partial helpers/get-run-path
                        helpers/document-name
                        "generated"
                        (name %))
               str
               get-training-global-step)
         (comp %
               :validation)))

(def get-file-to-generations
  (->> #{:reference :inference}
       (map make-get-file-to-generation)
       (apply juxt)))

(def log-generation
  ;TODO implement this function
  get-file-to-generations)

(def log
  (juxt log-recent
        log-minimum
        log-tensorboard
        log-generation))

(def handler
  #(->>
     {:validation (->> :validation
                       get-evaluation-tokens
                       helpers/grade-document
                       (helpers/transfer* :precision get-precision)
                       (helpers/transfer* :inference
                                          helpers/generate-document-inference)
                       (helpers/transfer* :reference generate-reference))
      :training   (-> %
                      :body
                      slurp
                      parse-string
                      helpers/flatten-sequential
                      set-precision)}
     (helpers/transfer* [:training :minimum] get-minimum)
     (command/effect log)
     :training
     generate-string
     response))

(def start
  (partial web/run handler helpers/option))

(defstate server
          :start (start)
          :stop (web/stop))
