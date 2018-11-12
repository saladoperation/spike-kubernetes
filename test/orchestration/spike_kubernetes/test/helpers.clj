(ns spike-kubernetes.test.helpers
  (:require [clojure.test :as test]
            [aid.core :as aid]
            [com.rpl.specter :as s]
            [spike-kubernetes.helpers :as helpers]))

(def article-sentence
  [{:dep_          "nsubj"
    :is_sent_start nil
    :lemma_        "-PRON-"
    :lower_        "it"
    :tag_          "PRP"}
   {:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "be"
    :lower_        "'s"
    :tag_          "VBZ"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "a"
    :lower_        "a"
    :tag_          "DT"}
   {:dep_          "nmod"
    :is_sent_start nil
    :lemma_        "state"
    :lower_        "state"
    :tag_          "NN"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "-"
    :lower_        "-"
    :tag_          "HYPH"}
   {:dep_          "prep"
    :is_sent_start nil
    :lemma_        "of"
    :lower_        "of"
    :tag_          "IN"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "-"
    :lower_        "-"
    :tag_          "HYPH"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "the"
    :lower_        "the"
    :tag_          "DT"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "-"
    :lower_        "-"
    :tag_          "HYPH"}
   {:dep_          "pobj"
    :is_sent_start nil
    :lemma_        "art"
    :lower_        "art"
    :tag_          "NN"}
   {:dep_          "attr"
    :is_sent_start nil
    :lemma_        "model"
    :lower_        "model"
    :tag_          "NN"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "."
    :lower_        "."
    :tag_          "."}])

(aid/defcurried test-arrangement
                [sentence k vs]
                (->> sentence
                     helpers/arrange-document-tokens
                     (map k)
                     (= vs)
                     test/is))

(def test-article-sentence
  (test-arrangement article-sentence))

(test/deftest aritcle-source
  (test-article-sentence :source
                         ["it" "'re" "state" "-" "of" "-" "art" "model" "."]))

(test/deftest aritcle-reference
  (test-article-sentence :reference [0 1 4 0 0 0 2 0 0]))

(test/deftest aritcle-hyphen
  (test-article-sentence :hyphen
                         [false false false false true false true false false]))

(def alternative-sentence
  [{:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "be"
    :lower_        "are"
    :tag_          "VBP"}
   {:dep_          "expl"
    :is_sent_start nil
    :lemma_        "there"
    :lower_        "there"
    :tag_          "EX"}
   {:dep_          "amod"
    :is_sent_start nil
    :lemma_        "many"
    :lower_        "many"
    :tag_          "JJ"}
   {:dep_          "attr"
    :is_sent_start nil
    :lemma_        "coder"
    :lower_        "coders"
    :tag_          "NNS"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "?"
    :lower_        "?"
    :tag_          "."}])

(def test-alternative-sentence
  (test-arrangement alternative-sentence))

(test/deftest alternative-source
  (test-alternative-sentence :source ["are" "there" "much" "coder" "?"]))

(test/deftest alternative-reference
  (test-alternative-sentence :reference [0 0 1 1 0]))

(def proper-sentence
  [{:dep_          "nsubj"
    :is_sent_start nil
    :lemma_        "-PRON-"
    :lower_        "it"
    :tag_          "PRP"}
   {:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "be"
    :lower_        "'s"
    :tag_          "VBZ"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "a"
    :lower_        "a"
    :tag_          "DT"}
   {:dep_          "attr"
    :is_sent_start nil
    :lemma_        "lisp"
    :lower_        "lisp"
    :tag_          "NNP"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "."
    :lower_        "."
    :tag_          "."}])

(def test-proper-sentence
  (test-arrangement proper-sentence))

(test/deftest proper-mask
  (test-proper-sentence :mask [false false false true false]))

(test/deftest proper-source
  (test-proper-sentence :source ["it" "'re" "a" "lisp" "."]))

(def compound-proper-sentence
  [{:dep_          "nsubj"
    :is_sent_start nil
    :lemma_        "-PRON-"
    :lower_        "i"
    :tag_          "PRP"}
   {:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "be"
    :lower_        "'m"
    :tag_          "VBP"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "a"
    :lower_        "a"
    :tag_          "DT"}
   {:dep_          "compound"
    :is_sent_start nil
    :lemma_        "lisp"
    :lower_        "lisp"
    :tag_          "NNP"}
   {:dep_          "attr"
    :is_sent_start nil
    :lemma_        "programmer"
    :lower_        "programmer"
    :tag_          "NN"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "."
    :lower_        "."
    :tag_          "."}])

(def test-compound-proper-sentence
  (test-arrangement compound-proper-sentence))

(test/deftest compound-proper-mask
  (test-compound-proper-sentence :mask [false false false false false]))

(test/deftest compound-proper-source
  (test-compound-proper-sentence :source ["i" "'m" "lisp" "programmer" "."]))

(def quote-sentence
  [{:dep_          "punct"
    :is_sent_start nil
    :lemma_        "\""
    :lower_        "\""
    :tag_          "``"}
   {:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "bring"
    :lower_        "bring"
    :tag_          "VB"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "the"
    :lower_        "the"
    :tag_          "DT"}
   {:dep_          "dobj"
    :is_sent_start nil
    :lemma_        "pain"
    :lower_        "pain"
    :tag_          "NN"}
   {:dep_          "advmod"
    :is_sent_start nil
    :lemma_        "forward"
    :lower_        "forward"
    :tag_          "RB"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "."
    :lower_        "."
    :tag_          "."}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "\""
    :lower_        "\""
    :tag_          "''"}])

(defn test-quote-sentence
  [k f]
  (->> quote-sentence
       f
       (test-arrangement quote-sentence k)))

(test/deftest quote-mask
  (test-quote-sentence :mask (comp (partial s/setval* s/FIRST false)
                                   (partial helpers/<$ true))))

(test/deftest quote-source
  (test-quote-sentence :source (partial map :lower_)))

(def start-sentence
  [{:dep_          "advmod"
    :is_sent_start nil
    :lemma_        "the"
    :lower_        "the"
    :tag_          "DT"}
   {:dep_          "amod"
    :is_sent_start nil
    :lemma_        "more"
    :lower_        "more"
    :tag_          "JJR"}
   {:dep_          "det"
    :is_sent_start nil
    :lemma_        "the"
    :lower_        "the"
    :tag_          "DT"}
   {:dep_          "ROOT"
    :is_sent_start nil
    :lemma_        "merrier"
    :lower_        "merrier"
    :tag_          "NN"}
   {:dep_          "punct"
    :is_sent_start nil
    :lemma_        "."
    :lower_        "."
    :tag_          "."}])

(test/deftest start
  (test-arrangement start-sentence :start [true nil nil]))

(defn test-structure
  [original candidate]
  (-> candidate
      ((->> original
            helpers/structure-lm
            flatten
            (mapcat :text_with_ws)
            set))
      test/is))

(test/deftest one-one
  (test-structure "I accept." "I agree."))

(test/deftest one-two-empty
  (test-structure "That was for years." "That was years ago."))

(test/deftest one-two-non-empty-non-split
  (test-structure "I opened the light." "I turned on light."))

(test/deftest one-two-non-empty-split
  (test-structure "I opened the light." "I turned light on."))

(test/deftest two-one
  (test-structure "I married with you." "I married you."))

(test/deftest two-two
  (test-structure "Do you fear?" "Are you afraid?"))

(test/deftest be
  (test-structure "I fear." "I am afraid."))

(test/deftest comparative
  (test-structure "I'm nicer with you." "I'm nicer to you."))

(test/deftest superlative
  (test-structure "I'm nicest with you." "I'm nicest to you."))

(defn test-generate
  [m sentence]
  (-> m
      helpers/generate-document-inference
      (= sentence)
      test/is))

(test/deftest insertion-lower-hyphen
  (test-generate {:alternative               ["they" "'s " "states" "-" "of" "-" "arts " "models" "."]
                  :character-with-whitespace ["it" "'re " "state" "-" "of" "-" "art " "model" "."]
                  :hyphen                    [false false false false true false true false false]
                  :inference                 [0 1 4 0 0 0 2 0 0]
                  :is_lower                  [false true true false true false true true false]
                  :is_title                  [true false false false false false false false false]
                  :mask                      [false false false false false false false false false]
                  :proper                    [false false false false false false false false false]
                  :start                     [true nil nil nil nil nil nil nil nil]
                  :text_with_ws              ["It" "'s " "state" "-" "of" "-" "art " "model" "."]}
                 "It's a state-of-the-art model."))

(test/deftest insertion-title
  (test-generate {:alternative               ["answers " "is " "no" "."]
                  :character-with-whitespace ["answer " "are " "no" "."]
                  :hyphen                    [false false false false]
                  :inference                 [2 1 0 0]
                  :is_lower                  [false true true false]
                  :is_title                  [true false false false]
                  :mask                      [false false false false]
                  :proper                    [false false false false]
                  :start                     [true nil nil nil]
                  :text_with_ws              ["Answer " "is " "no" "."]}
                 "The answer is no."))

(test/deftest deletion-common-title
  (test-generate {:alternative               ["codings " "is " "arts" "."]
                  :character-with-whitespace ["coding " "are " "art" "."]
                  :hyphen                    [false false false false]
                  :inference                 [0 1 4 0]
                  :is_lower                  [true true true false]
                  :is_title                  [false false false false]
                  :mask                      [false false false false]
                  :proper                    [false false false false]
                  :start                     [true nil nil nil]
                  :text_with_ws              ["coding " "is " "art" "."]}
                 "Coding is a art."))

(test/deftest deletion-common-upper
  (test-generate {:alternative               ["dnas " "sucks" "."]
                  :character-with-whitespace ["dna " "suck" "."]
                  :hyphen                    [false false false]
                  :inference                 [0 1 0]
                  :is_lower                  [false true false]
                  :is_title                  [false false false]
                  :mask                      [false false false]
                  :proper                    [false false false]
                  :start                     [true nil nil]
                  :text_with_ws              ["DNA " "sucks" "."]}
                 "DNA sucks."))

(test/deftest deletion-proper
  (test-generate {:alternative               ["iphones " "sucks" "."]
                  :character-with-whitespace ["iphone " "suck" "."]
                  :hyphen                    [false false false]
                  :inference                 [1 0 0]
                  :is_lower                  [false true false]
                  :is_title                  [false false false]
                  :mask                      [false false false]
                  :proper                    [true false false]
                  :start                     [true nil nil]
                  :text_with_ws              ["iPhones " "suck" "."]}
                 "iPhones suck."))
