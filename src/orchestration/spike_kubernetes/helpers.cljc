(ns spike-kubernetes.helpers
  #?(:clj
     (:require [clojure.java.io :as io]
               [clojure.set :as set]
               [clojure.string :as str]
               [clojure.tools.reader.edn :as edn]
               [aid.core :as aid]
               [cats.core :as m]
               [cheshire.core :refer :all]
               [clj-http.client :as client]
               [clojure.math.combinatorics :as combo]
               [com.rpl.specter :as s]
               [compliment.utils :as utils]
               [environ.core :refer [env]]
               [loom.graph :as graph]
               [me.raynes.fs :as fs]
               [spike-kubernetes.command :as command]
               [spike-kubernetes.parse.core :as parse])))

(def alteration-port
  3000)

#?(:clj
   (do
     (def orchestration-name
       "orchestration")

     (def alteration-name
       "alteration")

     (def username
       "relationship")

     (aid/defcurried if-else
                     [if-function else-function then]
                     (command/if-then-else if-function
                                           identity
                                           else-function
                                           then))

     (def singleton?
       (comp (partial = 1)
             count))

     (def get-image
       #(str username "/" % (if-else empty?
                                     (partial str ":")
                                     (cond (:circle-tag env) (-> env
                                                                 :circle-tag
                                                                 (subs 1))
                                           (singleton? *command-line-args*) ""
                                           :else (last *command-line-args*)))))

     (aid/defcurried transfer*
                     [apath f m]
                     (s/setval apath (f m) m))

     (defn deep-merge-with
       [f & more]
       (command/if-then-else (partial every? map?)
                             (partial apply
                                      merge-with
                                      (partial deep-merge-with f))
                             (partial apply f)
                             more))

     (def deep-merge
       (partial deep-merge-with (comp last
                                      vector)))
     (def join-paths
       (comp (partial str/join "/")
             vector))

     (def orchestration-port
       8080)

     (def parse-port
       8000)

     (def parse-name
       "parse")

     (def python-name
       {parse-port parse-name
        8001       "lm"
        8002       "document"})

     (def image-name
       (merge {orchestration-port orchestration-name
               alteration-port    alteration-name}
              python-name))

     (def get-origin
       (partial str "http://localhost:"))

     (def get-json-request
       (comp (partial array-map :as :json :content-type :json :body)
             generate-string))

     (def parse-remotely
       (comp :body
             (partial client/post (get-origin parse-port))
             get-json-request))

     (def set-starts
       (comp (partial s/setval* [s/FIRST :start] true)
             (partial map (transfer* :start :is_sent_start))))

     (def quotation-mark?*
       (comp (partial = "\"")
             :lower_))

     (def make-quotation-mark?
       #(aid/build and
                   (comp (partial = %)
                         :tag_)
                   quotation-mark?*))

     (def opening?
       (make-quotation-mark? "``"))

     (def closing?
       (make-quotation-mark? "''"))

     (defn last?
       [pred coll]
       (and (-> coll
                empty?
                not)
            (-> coll
                last
                pred)))

     (defn set-quote
       [tokens token]
       (->> token
            (s/setval :quote
                      (last? (aid/build or
                                        opening?
                                        (aid/build and
                                                   :quote
                                                   (complement closing?)))
                             tokens))
            (conj tokens)))

     (def set-quotes
       (partial reduce set-quote []))

     (def reverse-reduce
       (comp reverse
             (partial apply reduce)
             (partial s/transform* s/LAST reverse)
             vector))

     (def noun-prefix
       "NN")

     (defn set-proper
       [tokens token]
       (->> token
            (s/setval :proper (and (-> token
                                       :tag_
                                       (str/starts-with? (str noun-prefix "P")))
                                   (or (-> token
                                           :dep_
                                           (not= "compound"))
                                       (last? :proper tokens))))
            (conj tokens)))

     (def set-propers
       (partial reverse-reduce set-proper []))

     (def hyphen?
       (comp (partial = "-")
             :lower_))

     (def article-code
       {"the" 1
        "a"   2
        "an"  2})

     (def articles
       (-> article-code
           keys
           set))

     (def article-removal?
       (comp articles
             :lower_))

     (def hyphen-removal?
       (aid/build and
                  hyphen?
                  (comp pos?
                        :article)))

     (def get-diff
       (command/if-then-else article-removal?
                             (comp (partial zipmap
                                            [:article :article-title])
                                   (juxt (comp article-code
                                               :lower_)
                                         :is_title))
                             (comp (partial s/setval* :hyphen true)
                                   (partial (aid/flip select-keys)
                                            #{:article :article-title}))))

     (def removable?
       (complement (aid/build or
                              :proper
                              :quote)))

     (defn set-remove-token
       [tokens token]
       (cond (empty? tokens) [token]
             (and ((aid/build or
                              article-removal?
                              hyphen-removal?)
                    (last tokens))
                  (removable? token))
             (s/transform s/LAST
                          (comp (partial merge token)
                                get-diff)
                          tokens)
             :else (->> token
                        (s/setval :hyphen (-> tokens
                                              last
                                              hyphen?))
                        (conj tokens))))


     (def set-remove-tokens
       (comp (partial reduce set-remove-token [])
             (partial map (partial merge {:article       0
                                          :article-title false
                                          :hyphen        false}))))

     (def arrange-tokens*
       (comp set-remove-tokens
             set-propers
             set-quotes
             set-starts))

     (def uncountable
       {"few"    "little"
        "fewer"  "less"
        "fewest" "least"
        "many"   "much"})

     (def be
       {"is"  "are"
        "was" "were"})

     (def bijective-singular
       {"these" "this"
        "those" "that"})

     (def bijection
       (merge uncountable be bijective-singular))

     (def it
       "it")

     (def its
       "its")

     (def surjection
       {"they"   it
        "their"  its
        "them"   it
        "theirs" its})

     (def verb-prefix
       "VB")

     (def altering-tags
       #{(str noun-prefix "S") (str verb-prefix "Z")})

     (def possessive-s
       "'s")

     (def get-source
       #(->> %
             (command/if-then-else (comp altering-tags
                                         :tag_)
                                   :lemma_
                                   :lower_)
             (get (merge bijection
                         surjection
                         {possessive-s (aid/case-eval (:lemma_ %)
                                                      possessive-s possessive-s
                                                      "be" "'re"
                                                      "'ve")})
                  (:lower_ %))))

     (def set-source
       (transfer* :source get-source))

     (def set-reference
       (transfer* :reference
                  (aid/build +
                             (comp {true  1
                                    false 0}
                                   (partial apply not=)
                                   (partial s/select*
                                            (s/multi-path :source :lower_)))
                             (comp (partial * 2)
                                   :article))))

     (def set-mask
       (transfer* :mask (aid/build or
                                   :quote
                                   :proper)))

     (def arrange-tokens
       (comp (partial map (comp set-mask
                                set-reference
                                set-source))
             arrange-tokens*))

     (def partition-sentences
       (comp (partial map flatten)
             (partial partition 2)
             (partial partition-by :start)))

     (def sos
       "<sos>")

     (def set-sos
       (partial s/setval* s/BEFORE-ELEM {:forth        {:source sos}
                                         :lemma_       sos
                                         :proper       false
                                         :quote        false
                                         :text_with_ws ""}))

     (def set-forth-source
       (transfer* [:forth :source] get-source))

     (def set-original
       (partial s/setval* :original true))

     (def arrange-original-sentence
       (comp (partial map set-original)
             set-sos
             (partial map set-forth-source)))

     (def get-files
       (partial (aid/flip fs/find-files*) fs/file?))

     (def parse-cell
       (command/if-then-else (partial (aid/flip str/starts-with?) "[")
                             edn/read-string
                             vector))

     (def parse-line
       (comp (partial map parse-cell)
             (partial (aid/flip str/split) #"\t")))

     (def arrange-line
       (partial s/transform*
                [s/ALL
                 s/ALL]
                (comp set-remove-tokens
                      (partial map
                               (comp (partial s/setval*
                                              (s/multi-path :original
                                                            :proper
                                                            :quote)
                                              false)
                                     set-forth-source)))))

     (defn get-graph
       [f coll]
       (->> coll
            (map (partial apply f))
            (apply f)))

     (def get-confusion
       #(->> ["directed" "undirected"]
             (map (comp (partial map (comp arrange-line
                                           (partial s/transform*
                                                    [s/ALL
                                                     s/ALL]
                                                    parse-remotely)
                                           parse-line))
                        (partial mapcat str/split-lines)
                        (partial map slurp)
                        get-files
                        io/resource
                        (partial join-paths "lm" "confusions")))
             (map map [(comp (partial apply combo/cartesian-product)
                             (partial split-at 1))
                       combo/permutations])
             (map get-graph [graph/digraph graph/graph])
             (apply graph/digraph)))

     (def minimum-n
       1)

     (utils/defmemoized get-ns
                        []
                        (->> (get-confusion)
                             graph/nodes
                             (map (comp count
                                        first))
                             (apply max)
                             inc
                             (range minimum-n)))

     (utils/defmemoized get-confusions
                        []
                        (->> (get-confusion)
                             graph/edges
                             (map (aid/build array-map
                                             (comp (partial map :lemma_)
                                                   first
                                                   first)
                                             vector))
                             (apply merge-with concat)))

     (defn screen
       [sentence]
       (->> (get-ns)
            (mapcat #(->> sentence
                          (map :lemma_)
                          (partition % 1)))
            set
            (mapcat (get-confusions))))

     (def many-any
       (parse/many parse/any))

     (def verb?
       (partial (aid/flip str/starts-with?) verb-prefix))

     (def starts-with-verb?
       (comp verb?
             :tag_
             first))

     (def get-discriminative-token
       (command/if-then-else starts-with-verb?
                             first
                             last))

     (def get-edn-request
       (comp (partial array-map :as :clojure :body)
             pr-str))

     (def post-macchiato
       (partial client/post (get-origin alteration-port)))

     (utils/defmemoized get-alternative
                        []
                        (->> (get-confusion)
                             graph/nodes
                             vec
                             flatten
                             (map :lower_)
                             set
                             (array-map :action :get-alternative :data)
                             get-edn-request
                             post-macchiato
                             :body))

     (def condense-tag
       #(if (and (or (str/starts-with? % "J")
                     (str/starts-with? % "R"))
                 (-> %
                     count
                     (= 3)))
          (if (str/ends-with? % "R")
            :comparative
            :superlative)
          (-> %
              str/lower-case
              keyword)))

     (def ambiguous-endings
       #{"D" "N"})

     (aid/defcurried ends-with-one-of?
                     [s substrs]
                     (->> substrs
                          (map (partial str/ends-with? s))
                          (some true?)))

     (def make-verb-ending?
       #(aid/build and
                   verb?
                   ((aid/flip ends-with-one-of?) %)))

     (def ambiguous-tag?
       (make-verb-ending? ambiguous-endings))

     (def get-priority-tag
       #(command/if-then-else
          (aid/build and
                     (comp ambiguous-tag?
                           :tag_)
                     (aid/build =
                                :lemma_
                                (aid/build aid/funcall
                                           (comp condense-tag
                                                 :tag_)
                                           (comp (get-alternative)
                                                 :lemma_))))
          (constantly verb-prefix)
          :tag_
          %))

     (def pronoun?
       (partial (aid/flip str/starts-with?) "PR"))

     (def unalterable-tag?
       (aid/build or
                  pronoun?
                  (make-verb-ending? (set/union #{"G" "Z"} ambiguous-endings))
                  (comp (partial (aid/flip str/ends-with?) "e")
                        str
                        condense-tag)))

     (def unalterable-tags?
       (comp (partial some true?)
             (partial map (aid/build or
                                     empty?
                                     (comp unalterable-tag?
                                           get-priority-tag
                                           get-discriminative-token)))
             vector))

     (def conjunction?
       (aid/build or
                  (aid/build and
                             (comp (partial = "mark")
                                   :dep_)
                             (comp (partial = "IN")
                                   :tag_))
                  (comp (partial = "CC")
                        :tag_)))

     (aid/defcurried make-replaceable?
                     [k x]
                     (aid/build and
                                (comp (partial = x)
                                      k)
                                (complement conjunction?)
                                removable?
                                :original))

     (def noun?
       (partial (aid/flip str/starts-with?) noun-prefix))

     (def get-replaceable
       #(comp parse/satisfy
              (make-replaceable? %)
              %))

     (def replaceable-lower
       (get-replaceable :lower_))

     (def replaceable-lemma
       (get-replaceable :lemma_))

     (def replaceable-tokens
       (partial map (command/if-then-else noun?
                                          replaceable-lemma
                                          replaceable-lower)))

     (aid/defcurried get-variant-source
                     [original replacement-source]
                     (get ((get-alternative) replacement-source)
                          (-> original
                              :tag_
                              condense-tag)
                          replacement-source))

     (defn make-parse-b
       [originals replacements]
       (let [starts-with-verb (-> originals
                                  first
                                  starts-with-verb?)]
         (->> originals
              first
              ((aid/build (partial s/setval* (if starts-with-verb
                                               s/BEFORE-ELEM
                                               s/AFTER-ELEM))
                          (comp (if (->> [originals replacements]
                                         (map first)
                                         (apply unalterable-tags?))
                                  replaceable-lower
                                  replaceable-lemma)
                                (if starts-with-verb
                                  first
                                  last))
                          (comp replaceable-tokens
                                (if starts-with-verb
                                  rest
                                  drop-last))))
              (apply (aid/lift-a vector)))))

     (def non-particle-adverb-tag?
       (comp (partial (aid/flip str/starts-with?)
                      "RB")
             :tag_))

     (aid/defcurried if-else
                     [if-function else-function then]
                     (command/if-then-else if-function
                                           identity
                                           else-function
                                           then))

     (def whitespace
       " ")

     (def ensure-whitespace
       (if-else empty?
                (partial s/transform*
                         [s/LAST :text_with_ws]
                         (if-else (partial (aid/flip str/ends-with?) whitespace)
                                  (partial (aid/flip str) whitespace)))))

     (defn make-parse-singleton-c
       [child? originals replacements]
       (m/mlet [xs (-> non-particle-adverb-tag?
                       parse/satisfy
                       parse/many)
                ys (-> child?
                       complement
                       parse/satisfy
                       parse/many)
                z (parse/satisfy
                    (aid/build and
                               child?
                               (complement :quote)
                               (aid/build or
                                          (constantly (empty? xs))
                                          (comp (partial = (-> xs
                                                               last
                                                               :head_i))
                                                :i))
                               (comp not
                                     (->> ys
                                          (filter non-particle-adverb-tag?)
                                          (map :head_i)
                                          set)
                                     :i)
                               (comp (partial not= "punct")
                                     :dep_)))]
               (if (-> replacements
                       last
                       empty?)
                 (m/pure (concat xs
                                 (-> replacements
                                     second
                                     ensure-whitespace)
                                 ys
                                 [z]))
                 ((if (-> replacements
                          first
                          empty?)
                    identity
                    (partial m/<> (m/pure (concat (-> replacements
                                                      last
                                                      ensure-whitespace)
                                                  xs
                                                  ys
                                                  [z]))))
                   (m/pure (concat xs
                                   ys
                                   (ensure-whitespace [z])
                                   (->> replacements
                                        last
                                        (s/setval* [s/LAST
                                                    :text_with_ws
                                                    s/END]
                                                   (:whitespace_ z)))))))))

     (def non-empty-last
       (comp last
             (partial remove empty?)))

     (defn append-whitespace
       [original replacement]
       (s/setval [s/LAST :text_with_ws s/END]
                 (-> original
                     last
                     :whitespace_)
                 replacement))

     (defn make-parse-multiton-c
       [originals replacements]
       ((aid/lift-a concat)
         many-any
         (->>
           originals
           non-empty-last
           replaceable-tokens
           (apply
             (aid/lift-a (fn [& original]
                           (aid/casep replacements
                                      singleton? []
                                      (->> replacements
                                           non-empty-last
                                           (append-whitespace original)))))))))

     (defn make-parse-c
       [child? originals replacements]
       (aid/casep originals
                  singleton? (aid/casep replacements
                                        singleton? (m/pure [])
                                        (make-parse-singleton-c child?
                                                                originals
                                                                replacements))
                  (make-parse-multiton-c originals replacements)))

     (def trim-tags
       #{"," "." "-"})

     (def trim?
       (comp trim-tags
             :tag_
             first))

     (def trim-last
       (partial s/transform* [s/LAST :text_with_ws] str/trimr))

     (aid/defcurried set-a-text-with-wss
                     [original replacement]
                     ((aid/casep replacement
                                 trim? trim-last
                                 identity)
                       (command/if-then (comp (partial < 1)
                                              count)
                                        ensure-whitespace
                                        original)))

     (aid/defcurried set-b-text-with-wss
                     [original replacement]
                     (->> replacement
                          (map (transfer* :text_with_ws
                                          (aid/build str
                                                     (comp :source
                                                           :forth)
                                                     :whitespace_)))
                          (append-whitespace original)
                          ((if (-> original
                                   first
                                   :start)
                             (partial s/transform*
                                      [s/FIRST
                                       :text_with_ws]
                                      str/capitalize)
                             identity))))

     (def make-set-variant-source
       #(partial s/transform*
                 [(aid/casep %
                             starts-with-verb? s/FIRST
                             s/LAST)
                  :forth
                  :source]
                 (-> %
                     get-discriminative-token
                     get-variant-source)))

     (defn get-variant-parser
       [originals replacements]
       ;TODO optimize this function
       (m/mlet [a many-any
                b (make-parse-b originals replacements)
                c (make-parse-c (comp (partial = (-> b
                                                     first
                                                     :i))
                                      :head_i)
                                originals
                                replacements)
                d many-any]
               (m/pure (concat (->> replacements
                                    first
                                    (set-a-text-with-wss a))
                               (->> replacements
                                    first
                                    ((if-else (partial unalterable-tags?
                                                       (first originals))
                                              (make-set-variant-source b)))
                                    (set-b-text-with-wss b))
                               ((aid/casep d
                                           trim? trim-last
                                           identity)
                                 c)
                               d))))

     (defn get-variants*
       [originals replacements sentence]
       (-> (get-variant-parser originals replacements)
           (parse/parse sentence)))

     (defn recursively-get-variants*
       [original replacement before accumulation]
       (let [after (->> before
                        (mapcat (partial get-variants*
                                         original
                                         replacement))
                        set)]
         (aid/casep after
                    empty? accumulation
                    (->> (concat accumulation after)
                         set
                         (recur original replacement after)))))

     (def get-variants
       (aid/build mapcat
                  (aid/curriedfn [sentence [originals replacements]]
                                 (->> sentence
                                      (get-variants* originals replacements)
                                      (repeat 2)
                                      (apply recursively-get-variants*
                                             originals
                                             replacements)))
                  screen))

     (def consolidate-into-vector
       (comp (partial apply merge-with concat)
             (partial s/transform* [s/ALL s/MAP-VALS] vector)))

     (def consolidate-into-sentence
       (comp (partial s/transform* :text_with_ws (comp vector
                                                       str/join))
             consolidate-into-vector))

     (def eos
       "<eos>")

     (def get-key-map
       (comp (partial apply array-map)
             (aid/build concat
                        identity
                        reverse)))

     (aid/defcurried flip-keys
                     [m ks]
                     (->> ks
                          get-key-map
                          (set/rename-keys m)))

     (def set-character
       (transfer* :character :source))

     (def set-back
       (transfer* :back (comp set-character
                              (partial s/transform* s/MAP-VALS reverse)
                              ((aid/flip flip-keys) #{:source :reference})
                              :forth)))

     (def augment-forth
       (partial s/transform*
                :forth
                (comp (transfer* :reference
                                 (comp (partial s/setval* s/AFTER-ELEM eos)
                                       rest
                                       :source))
                      set-character
                      consolidate-into-vector)))

     (def consolidate-original
       (partial s/transform* :original (comp vector
                                             (partial every? true?))))

     (def arrange-candidate-sentence
       (comp consolidate-original
             set-back
             augment-forth
             consolidate-into-sentence))

     (defn set-index
       [index sentences]
       (s/setval [s/ALL :index] [index] sentences))

     (def mapcat-indexed
       (comp (partial apply concat)
             map-indexed))

     (def group-by-vals
       (aid/curry 2 (comp vals
                          group-by)))

     (def structure-evaluation-sentences
       (comp (group-by-vals (comp count
                                  :lemma_))
             (partial mapcat-indexed set-index)
             (partial map (comp (partial map arrange-candidate-sentence)
                                (aid/build cons
                                           identity
                                           get-variants)
                                arrange-original-sentence))
             partition-sentences
             arrange-tokens*
             parse-remotely))))
