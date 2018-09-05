(ns spike-kubernetes.clojurescript
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [en-inflectors]
            [oops.core :refer [ocall+]]))

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

(def lm-tags
  (set/union verb-tags #{:comparative :superlative}))

(defn get-lm-alternative
  [lemma]
  {lemma (->> lm-tags
              (mapcat (juxt identity (inflect lemma)))
              (apply array-map))})
