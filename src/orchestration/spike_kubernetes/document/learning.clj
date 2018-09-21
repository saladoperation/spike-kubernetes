(ns spike-kubernetes.document.learning
  (:require [clojure.tools.reader.edn :as edn]
            [spike-kubernetes.helpers :as helpers]))

(def get-length
  #(-> helpers/length-path
       slurp
       edn/read-string))

(def partition-into-batches
  #(->> %
        (partition (-> (get-length)
                       keys
                       count
                       (quot (:batch-size (helpers/get-document-tuned)))))
        (take (:batch-size (helpers/get-document-tuned)))))
