(ns spike-kubernetes.test.core
  (:require [clojure.set :as set]
            [cljs.test :as test]
            [aid.core :as aid]
            [doo.runner :refer-macros [doo-all-tests]]
            [spike-kubernetes.core :as core]))

(aid/defcurried test-alternative
                [tags lemma alternative]
                (-> lemma
                    core/get-lm-alternative
                    vals
                    first
                    (select-keys tags)
                    (= alternative)
                    test/is))

(def test-verb-alternative
  (-> core/verb-tags
      (set/difference #{:vbz})
      test-alternative))

(test/deftest do
  (test-verb-alternative "do" {:vbd "did"
                               :vbg "doing"
                               :vbn "done"
                               :vbp "do"}))

(test/deftest be
  (test-verb-alternative "be" {:vbd "were"
                               :vbg "being"
                               :vbn "been"
                               :vbp "are"}))

(test/deftest much
  (test-alternative core/atives "much" {:comparative "more"
                                        :superlative "most"}))

(doo-all-tests #"spike-kubernetes.+")
