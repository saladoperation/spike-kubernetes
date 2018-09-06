(ns spike-kubernetes.test.clojurescript
  (:require [cljs.test :as test]
            [spike-kubernetes.clojurescript :as clojurescript]))

(def is
  "is")

(test/deftest be
  (-> is
      ((clojurescript/get-lm-alternative is))
      :vbd
      (= "was")
      test/is))
