(ns spike-kubernetes.document.learning
  (:require [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]
            [clojure.core.memoize :as memoize]
            [aid.core :as aid]
            [clj-http.client :as client]
            [cheshire.core :refer :all]
            [com.rpl.specter :as s]
            [compliment.utils :as utils]
            [immutant.web :as web]
            [langohr.basic :as lb]
            [langohr.channel :as lc]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [ring.util.response :refer [response]]
            [taoensso.nippy :as nippy]
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
                   fs/base-name))
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

(def minute
  (* 60 1000))

(def thaw-from-file-memoize
  (memoize/ttl nippy/thaw-from-file :ttl/threshold minute))

(def get-step
  #(->> %
        :file
        (take (-> %
                  get-document-offset*
                  inc))
        (mapcat thaw-from-file-memoize)
        (drop (:token-offset %))
        (take (-> helpers/document-name
                  helpers/get-tuned
                  :step-size))
        (s/transform [s/ALL :source] helpers/get-document-index)
        helpers/merge-into-vector
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
               (comp (partial map helpers/merge-into-vector)
                     rest
                     (partial iterate (partial map get-step))
                     helpers/separate
                     (helpers/transfer* :file
                                        #(->> (helpers/get-training-path)
                                              helpers/get-files
                                              partition-into-batches
                                              (map cycle)
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
        (juxt (partial filter (aid/build or
                                         :mask
                                         (aid/build =
                                                    :inference
                                                    :reference)))
              identity)
        helpers/separate
        (partial (aid/flip select-keys) #{:mask :inference :reference})))

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

(def get-recent-edn-path
  #(-> recent-name
       (helpers/append-extension helpers/edn-name)
       get-checkpoints-path))

(def log-recent
  #(->> %
        generate-string
        (spit (get-recent-edn-path))))

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
  (comp (partial client/post (helpers/get-origin helpers/document-port))
        helpers/get-json-request
        (partial array-map :action :log-tensorboard :data)
        (aid/build s/setval*
                   (constantly [s/ALL s/AFTER-ELEM])
                   get-training-global-step
                   (comp vec
                         (partial (aid/flip select-keys) #{:loss :precision})
                         (partial reorder-keys reverse)))))

(def make-get-file-to-generation
  #(juxt (comp (partial (aid/flip helpers/append-extension) "txt")
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

(def log-generations
  (comp (partial run! (partial apply helpers/spit+))
        get-file-to-generations))

(def log
  (juxt log-recent
        log-minimum
        log-tensorboard
        log-generations))

(def handler
  #(->>
     {:validation (->> :validation
                       get-evaluation-tokens
                       helpers/grade-document
                       set-precision
                       (helpers/transfer* :inference
                                          helpers/generate-document-inference)
                       (helpers/transfer* :reference generate-reference))
      :training   (-> %
                      :body
                      slurp
                      helpers/parse-keywordize
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

(def get-initial-offset
  #(-> helpers/document-name
       helpers/get-tuned
       :batch-size
       (repeat 0)))

(def get-initial-step
  #(aid/casep (get-recent-edn-path)
              fs/exists? (->> (get-recent-edn-path)
                              slurp
                              helpers/parse-keywordize
                              :training
                              (s/transform :global_step inc))
              {:token-offset    (get-initial-offset)
               :document-offset (get-initial-offset)
               :global_step     0}))

(defn learn
  []
  (mount/start)
  (->> (get-initial-step)
       get-training-steps
       (run! publish-if-zero!)))
