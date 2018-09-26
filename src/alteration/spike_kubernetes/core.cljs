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
                (if (and (= tag :vbp) (= verb "be"))
                  "are"
                  (->> tag
                       name
                       str/upper-case
                       (.conjugate (inflectors. verb)))))

(def verb-tags
  #{:vbd :vbg :vbn :vbp})

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

(def act
  ;TODO implement this function
  {:get-alternative (comp (partial apply merge)
                          (partial map get-lm-alternative))})

(def handle
  (aid/build aid/funcall
             (comp act
                   :action)
             :data))

(defn index
  [req res raise]
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
