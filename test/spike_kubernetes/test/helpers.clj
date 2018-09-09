(ns spike-kubernetes.test.helpers
  (:require [clojure.test :as test]
            [aid.core :as aid]
            [spike-kubernetes.helpers :as helpers]))

(def article-sentence
  [{:dep_   "nsubj"
    :lemma_ "-PRON-"
    :lower_ "it"
    :tag_   "PRP"}
   {:dep_   "ROOT"
    :lemma_ "be"
    :lower_ "'s"
    :tag_   "VBZ"}
   {:dep_   "det"
    :lemma_ "a"
    :lower_ "a"
    :tag_   "DT"}
   {:dep_   "nmod"
    :lemma_ "state"
    :lower_ "state"
    :tag_   "NN"}
   {:dep_   "punct"
    :lemma_ "-"
    :lower_ "-"
    :tag_   "HYPH"}
   {:dep_   "prep"
    :lemma_ "of"
    :lower_ "of"
    :tag_   "IN"}
   {:dep_   "punct"
    :lemma_ "-"
    :lower_ "-"
    :tag_   "HYPH"}
   {:dep_   "det"
    :lemma_ "the"
    :lower_ "the"
    :tag_   "DT"}
   {:dep_   "punct"
    :lemma_ "-"
    :lower_ "-"
    :tag_   "HYPH"}
   {:dep_   "pobj"
    :lemma_ "art"
    :lower_ "art"
    :tag_   "NN"}
   {:dep_   "attr"
    :lemma_ "algorithm"
    :lower_ "algorithm"
    :tag_   "NN"}])

(aid/defcurried test-arrangement
                [sentence k vs]
                (->> sentence
                     helpers/arrange-tokens
                     (map k)
                     (= vs)
                     test/is))

(def test-article-sentence
  (test-arrangement article-sentence))

(test/deftest aritcle-source-arrangement
  (test-article-sentence :source
                         ["it" "'re" "state" "-" "of" "-" "art" "algorithm"]))

(test/deftest aritcle-reference-arrangement
  (test-article-sentence :reference [0 1 4 0 0 0 2 0]))

(test/deftest aritcle-hyphen-arrangement
  (test-article-sentence :hyphen
                         [false false false false true false true false]))
