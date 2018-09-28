(ns spike-kubernetes.document.tuning
  (:require [aid.core :as aid]
            [cats.monad.either :as either]
            [cheshire.core :refer :all]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [com.rpl.specter :as s]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def get-timestamp
  (partial f/unparse (f/formatters :basic-date-time-no-ms)))

(def get-selection
  (partial array-map :recent true :run))

(defn tune
  []
  (aid/mlet
    [commit (command/git "rev-parse" "HEAD")]
    (->
      (t/now)
      get-timestamp
      ((juxt
         #(->> helpers/hyperparameter
               (s/setval :commit commit)
               ((juxt pr-str
                      generate-string))
               (map helpers/spit+
                    (map (helpers/get-tuned-path helpers/document-name
                                                 %)
                         ["edn" "json"]))
               dorun)
         (comp (partial spit (helpers/get-selection-path helpers/document-name))
               generate-string
               get-selection)))
      either/right)))
