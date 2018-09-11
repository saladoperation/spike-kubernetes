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
    :lemma_ "model"
    :lower_ "model"
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

(test/deftest aritcle-source
  (test-article-sentence :source
                         ["it" "'re" "state" "-" "of" "-" "art" "model"]))

(test/deftest aritcle-reference
  (test-article-sentence :reference [0 1 4 0 0 0 2 0]))

(test/deftest aritcle-hyphen
  (test-article-sentence :hyphen
                         [false false false false true false true false]))

(def alternative-sentence
  [{:dep_   "ROOT"
    :lemma_ "be"
    :lower_ "are"
    :tag_   "VBP"}
   {:dep_   "expl"
    :lemma_ "there"
    :lower_ "there"
    :tag_   "EX"}
   {:dep_   "amod"
    :lemma_ "many"
    :lower_ "many"
    :tag_   "JJ"}
   {:dep_   "attr"
    :lemma_ "hero"
    :lower_ "heroes"
    :tag_   "NNS"}
   {:dep_   "punct"
    :lemma_ "?"
    :lower_ "?"
    :tag_   "."}])

(def test-alternative-sentence
  (test-arrangement alternative-sentence))

(test/deftest alternative-source
  (test-alternative-sentence :source ["are" "there" "much" "hero" "?"]))

(test/deftest alternative-reference
  (test-alternative-sentence :reference [0 0 1 1 0]))

(def proper-sentence
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
   {:dep_   "attr"
    :lemma_ "lisp"
    :lower_ "lisp"
    :tag_   "NNP"}
   {:dep_   "punct"
    :lemma_ "."
    :lower_ "."
    :tag_   "."}])

(def test-proper-sentence
  (test-arrangement proper-sentence))

(test/deftest proper-mask
  (test-proper-sentence :mask [false false false true false]))

(test/deftest proper-source
  (test-proper-sentence :source ["it" "'re" "a" "lisp" "."]))
