(ns spike-kubernetes.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.tools.reader :as reader]
            [aid.core :as aid]
            [en-inflectors]
            [macchiato.server :as server]
            [macchiato.util.response :as r]
            [oops.core :refer [ocall+]]
            [spike-kubernetes.helpers :as helpers]))

(def inflectors
  (.-Inflectors en-inflectors))

(aid/defcurried conjugate
                [verb tag]
                ;TODO delete if when en-inflectors is fixed
                (cond (and (= tag :vbp) (= verb "be")) "are"
                      (and (= tag :vbz) (= verb "are")) "is"
                      :else (->> tag
                                 name
                                 str/upper-case
                                 (.conjugate (inflectors. verb)))))

(def verb-tags
  #{:vbd :vbg :vbn :vbp :vbz})

(aid/defcurried inflect
                [lemma k]
                (aid/casep verb-tags
                           k (conjugate lemma k)
                           (-> lemma
                               inflectors.
                               (ocall+ (name k)))))

(def atives
  #{:comparative :superlative})

(def lm-tags
  (set/union verb-tags atives))

(defn get-lm-alternative
  [lemma]
  {lemma (->> lm-tags
              (mapcat (juxt identity (inflect lemma)))
              (apply array-map))})

(def bijection
  (set/map-invert helpers/bijection))

(def surjection
  {"'re" "'s"
   "'ve" "'s"})

(def plural-noun?
  (aid/build and
             helpers/noun?
             (partial (aid/flip str/ends-with?) "S")))

(def singular-noun?
  (aid/build and
             helpers/noun?
             (complement plural-noun?)))

(def pluralize
  #(-> %
       inflectors.
       .toPlural))

(def get-document-alternative
  #(->> %
        :whitespace_
        (str (get (merge bijection surjection)
                  (:source %)
                  (case (:source %)
                    "it" (if (and (= (:dep_ %)
                                     "nsubj")
                                  (not= (:head_tag_ %)
                                        "VBG"))
                           "they"
                           "them")
                    "its" (case (:tag_ %)
                            "PRP" "theirs"
                            "their")
                    (aid/casep
                      (:tag_ %)
                      (aid/build or
                                 plural-noun?
                                 (partial = "VBZ"))
                      (:lower_ %)
                      ((aid/casep (:tag_ %)
                                  singular-noun? pluralize
                                  (partial = "VBP") ((aid/flip inflect) :vbz)
                                  identity)
                        (:source %))))))))

(def set-alternatives
  (partial map
           (helpers/transfer* :alternative
                              get-document-alternative)))

(def handle
  (aid/build aid/funcall
             (comp {:get-alternative  (comp (partial apply merge)
                                            (partial map get-lm-alternative))
                    :set-alternatives set-alternatives}
                   :action)
             :data))

(defn index
  [req res _]
  (let [data (atom "")]
    (-> req
        :body
        (.on "data" (partial swap! data str)))
    (-> req
        :body
        (.on "end" #(->> @data
                         reader/read-string
                         handle
                         pr-str
                         r/ok
                         res)))))

(def start
  #(server/start {:handler index
                  :port    helpers/alteration-port}))

(set! *main-cli-fn* start)
