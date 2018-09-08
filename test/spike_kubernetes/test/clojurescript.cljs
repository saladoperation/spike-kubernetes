(ns spike-kubernetes.test.clojurescript
  (:require [cljs.test :as test]
            [aid.core :as aid]
            [spike-kubernetes.clojurescript :as clojurescript]))

(aid/defcurried test-alternative
                [tags lemma alternative]
                (-> lemma
                    clojurescript/get-lm-alternative
                    vals
                    first
                    (select-keys tags)
                    (= alternative)
                    test/is))

(def test-verb-alternative
  (test-alternative clojurescript/verb-tags))

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
  (test-alternative clojurescript/atives "much" {:comparative "more"
                                                 :superlative "most"}))
