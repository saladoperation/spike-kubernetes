(ns spike-kubernetes.helpers
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [com.rpl.specter :as s]
    #?@(:clj [
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.set :as set]
            [clojure.tools.reader.edn :as edn]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer :all]
            [cats.builtin]
            [cats.core :as m]
            [cats.monad.either :as either]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            [compliment.utils :as utils]
            [environ.core :refer [env]]
            [incanter.core :as incanter]
            [me.raynes.fs :as fs]
            [superstring.core :as superstring]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.parse.core :as parse]])))

(def alteration-port
  3000)

(def app-name
  "app")

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

(def noun-prefix
  "NN")

(def noun?
  (partial (aid/flip str/starts-with?) noun-prefix))

(aid/defcurried transfer*
                [apath f m]
                (s/setval apath (f m) m))

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
       #(str username
             "/"
             %
             (if-else empty?
                      (partial str ":")
                      (get env
                           :circle-tag
                           (aid/casep *command-line-args*
                                      singleton? ""
                                      (last *command-line-args*))))))

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
     (def get-path
       (comp (partial str/join "/")
             vector))

     (def orchestration-port
       8080)

     (def parse-port
       8000)

     (def parse-name
       "parse")

     (def lm-port
       8001)

     (def lm-name
       "lm")

     (def document-name
       "document")

     (def document-port
       8002)

     (def model-port-name
       {lm-port       lm-name
        document-port document-name})

     (def python-port-name
       (s/setval parse-port parse-name model-port-name))

     (def image-names
       (->> python-port-name
            vals
            set
            (set/union #{orchestration-name
                         alteration-name})))

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

     (def set-proper-noun
       (transfer* :proper-noun (comp (partial (aid/flip str/starts-with?)
                                              (str noun-prefix "P"))
                                     :tag_)))

     (def set-proper
       (transfer* :proper (aid/build or
                                     :proper-noun
                                     (comp (partial contains? #{"PERSON"
                                                                "NORP"
                                                                "FAC"
                                                                "ORG"
                                                                "GPE"
                                                                "LOC"
                                                                "PRODUCT"
                                                                "EVENT"
                                                                "WORK_OF_ART"
                                                                "LAW"
                                                                "LANGUAGE"})
                                           :ent_type_))))

     (def reverse-reduce
       (comp reverse
             (partial apply reduce)
             (partial s/transform* s/LAST reverse)
             vector))

     (defn set-masked-proper-noun
       [tokens token]
       (->> token
            (s/setval :masked-proper-noun
                      (and (:proper-noun token)
                           (or (-> token
                                   :dep_
                                   (not= "compound"))
                               (last? :masked-proper-noun tokens))))
            (conj tokens)))

     (def set-masked-proper-nouns
       (partial reverse-reduce set-masked-proper-noun []))

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
                             (comp (partial zipmap [:article :start])
                                   (juxt (comp article-code
                                               :lower_)
                                         :start))
                             (comp (partial s/setval* :hyphen true)
                                   (partial (aid/flip select-keys)
                                            #{:article}))))

     (def mask?
       (aid/build or
                  :masked-proper-noun
                  :quote))

     (def removable?
       (complement mask?))

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
             (partial map (partial merge {:article 0
                                          :hyphen  false}))))

     (def arrange-tokens
       (comp set-remove-tokens
             set-masked-proper-nouns
             (partial map (comp set-proper
                                set-proper-noun))
             set-quotes
             set-starts))

     (def surjection
       {"they"   "it"
        "their"  "its"
        "them"   "it"
        "theirs" "its"})

     (def verb-prefix
       "VB")

     (def altering-tags
       #{(str noun-prefix "S") (str verb-prefix "Z")})

     (def get-source
       #(->> %
             (command/if-then-else (comp altering-tags
                                         :tag_)
                                   :lemma_
                                   :lower_)
             (get (merge bijection
                         surjection
                         {"'s" (aid/case-eval (:lemma_ %)
                                              "'s" "'s"
                                              "be" "'re"
                                              "'ve")})
                  (:lower_ %))))

     (def set-source
       (transfer* :source get-source))

     (def integer
       {true  1
        false 0})

     (def set-reference
       (transfer* :reference
                  (aid/build +
                             (comp integer
                                   (partial apply not=)
                                   (partial s/select*
                                            (s/multi-path :source :lower_)))
                             (comp (partial * 2)
                                   :article))))

     (def set-mask
       (transfer* :mask mask?))

     (def set-character-with-whitespace
       (transfer* :character-with-whitespace (aid/build str
                                                        :source
                                                        :whitespace_)))

     (def arrange-document-tokens
       (comp (partial map (comp set-mask
                                set-reference
                                set-character-with-whitespace
                                set-source))
             arrange-tokens))

     (def partition-sentences
       (comp (partial map flatten)
             (partial partition 2)
             (partial partition-by :start)))

     (def sos
       "<sos>")

     (def set-sos
       (partial s/setval* s/BEFORE-ELEM {:forth              {:source sos}
                                         :lemma_             sos
                                         :masked-proper-noun false
                                         :quote              false
                                         :text_with_ws       ""}))

     (def set-forth-source
       (transfer* [:forth :source] get-source))

     (def set-original
       (partial s/setval* :original true))

     (def arrange-original-sentence
       (comp (partial map set-original)
             set-sos
             (partial map set-forth-source)))

     (def confusions-name
       "confusions")

     (def get-resources-path
       (partial get-path "resources"))

     (def n-minimum
       1)

     (def preparation-name
       "preparation")

     (def append-extension
       (aid/curry 2 (comp (partial str/join ".")
                          vector)))

     (def edn-name
       "edn")

     (def preparation-filename
       (append-extension preparation-name edn-name))

     (utils/defmemoized get-preparation
                        []
                        (-> preparation-filename
                            get-resources-path
                            slurp
                            edn/read-string))

     (defn screen
       [sentence]
       (->> (get-preparation)
            :n-supremum
            (range n-minimum)
            (mapcat #(->> sentence
                          (map :lemma_)
                          (partition % 1)))
            set
            (mapcat (:confusions (get-preparation)))))

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
          (aid/build
            and
            (comp ambiguous-tag?
                  :tag_)
            (aid/build =
                       :lemma_
                       (aid/build aid/funcall
                                  (comp condense-tag
                                        :tag_)
                                  (comp (:alternative (get-preparation))
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

     (aid/defcurried get-replaceable
                     [ks m]
                     (parse/satisfy
                       (aid/build and
                                  (comp (partial some true?)
                                        (apply juxt
                                               (map #(comp (partial = (% m))
                                                           %)
                                                    ks)))
                                  (complement conjunction?)
                                  removable?
                                  :original)))

     (def replaceable-lower
       (get-replaceable [:lower_]))

     (def replaceable-lower-lemma
       (get-replaceable [:lower_ :lemma_]))

     (def replaceable-tokens
       (partial map (command/if-then-else noun?
                                          replaceable-lower-lemma
                                          replaceable-lower)))

     (aid/defcurried make-parse-original
                     [f originals replacements]
                     (let [starts-with-verb (-> originals
                                                f
                                                starts-with-verb?)]
                       (->>
                         originals
                         f
                         ((aid/build (partial s/setval* (if starts-with-verb
                                                          s/BEFORE-ELEM
                                                          s/AFTER-ELEM))
                                     (comp (if (->> [originals replacements]
                                                    (map first)
                                                    (apply unalterable-tags?))
                                             replaceable-lower
                                             replaceable-lower-lemma)
                                           (if starts-with-verb
                                             first
                                             last))
                                     (comp replaceable-tokens
                                           (if starts-with-verb
                                             rest
                                             drop-last))))
                         (apply (aid/lift-a vector)))))

     (def make-parse-b
       (make-parse-original first))

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

     (def trim-lowers
       #{"!" "%" ")" "," "-" "." ":" ";" "?"})

     (def trim?
       (aid/build or
                  empty?
                  (comp trim-lowers
                        :lower_
                        first)))

     (def trim-last
       (partial s/transform* [s/LAST :text_with_ws] str/trimr))

     (def ensure-whitespace
       (if-else (aid/build or
                           empty?
                           (comp (partial = "")
                                 :text_with_ws
                                 last))
                (partial s/transform*
                         [s/LAST :text_with_ws]
                         (if-else (comp #{\  \(}
                                        last)
                                  (partial (aid/flip str) " ")))))

     (defn concatenate-blocks*
       [reduction element]
       (concat ((aid/casep reduction
                           trim? trim-last
                           ensure-whitespace)
                 element)
               reduction))

     (defn concatenate-blocks
       [& more]
       (m/pure (reduce concatenate-blocks* (reverse more))))

     (defn make-parse-singleton-c
       [child? replacements]
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
                                                               :head_
                                                               :i))
                                                :i))
                               (comp not
                                     (->> ys
                                          (filter non-particle-adverb-tag?)
                                          (map (comp :i
                                                     :head_))
                                          set)
                                     :i)
                               (comp (partial not= "punct")
                                     :dep_)))]
               (if (-> replacements
                       last
                       empty?)
                 (concatenate-blocks xs
                                     (second replacements)
                                     ys
                                     [z])
                 ((if (-> replacements
                          first
                          empty?)
                    identity
                    (partial m/<> (concatenate-blocks (last replacements)
                                                      xs
                                                      ys
                                                      [z])))
                   (concatenate-blocks xs
                                       ys
                                       [z]
                                       (last replacements))))))

     (aid/defcurried get-variant-source
                     [original replacement-source]
                     (get ((:alternative (get-preparation)) replacement-source)
                          (-> original
                              :tag_
                              condense-tag)
                          replacement-source))

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

     (def set-lower-text-with-wss
       (partial map (transfer* :text_with_ws (aid/build str
                                                        (comp :source
                                                              :forth)
                                                        :whitespace_))))

     (aid/defcurried set-b-text-with-wss
                     [original replacement]
                     (->> replacement
                          set-lower-text-with-wss
                          ((if (-> original
                                   first
                                   :start)
                             (partial s/transform*
                                      [s/FIRST :text_with_ws]
                                      str/capitalize)
                             identity))))

     (defn make-parse-multiton-c
       [originals replacements]
       ((aid/lift-a concat)
         many-any
         (->>
           (make-parse-original last originals replacements)
           ((aid/lift-a #(aid/casep replacements
                                    singleton? []
                                    (->> replacements
                                         last
                                         ((if-else (partial unalterable-tags?
                                                            (last originals))
                                                   (make-set-variant-source %)))
                                         set-lower-text-with-wss)))))))

     (defn make-parse-c
       [child? originals replacements]
       (aid/casep originals
                  singleton? (aid/casep replacements
                                        singleton? (m/pure [])
                                        (make-parse-singleton-c child?
                                                                replacements))
                  (make-parse-multiton-c originals replacements)))

     (defn get-variant-parser
       [originals replacements]
       ;TODO optimize this function
       (m/mlet
         [a many-any
          b (make-parse-b originals replacements)
          c (make-parse-c (comp (partial = (-> b
                                               first
                                               :i))
                                :i
                                :head)
                          originals
                          replacements)
          d many-any]
         (concatenate-blocks a
                             (->> replacements
                                  first
                                  ((if-else (partial unalterable-tags?
                                                     (first originals))
                                            (make-set-variant-source b)))
                                  (set-b-text-with-wss b))
                             c
                             d)))

     (defn get-variants*
       [originals replacements sentence]
       (-> (get-variant-parser originals replacements)
           (parse/parse sentence)))

     (defn recursively-get-variants*
       [originals replacements before accumulation]
       (let [after (->> before
                        (mapcat (partial get-variants*
                                         originals
                                         replacements))
                        set)]
         (aid/casep after
                    empty? accumulation
                    (->> (concat accumulation after)
                         set
                         (recur originals replacements after)))))

     (def get-variants
       (aid/build mapcat
                  (aid/curriedfn [sentence [originals replacements]]
                                 (recursively-get-variants* originals
                                                            replacements
                                                            [sentence]
                                                            []))
                  screen))

     (def merge-into-vector
       ;Replacing into with (comp vec concat) seems too slow to wait for the benchmark to finish.
       (comp (partial apply merge-with into)
             (partial s/transform* [s/ALL s/MAP-VALS] vector)))

     (def concatenate-into-sentence
       (comp (partial s/transform* :text_with_ws (comp vector
                                                       str/join))
             merge-into-vector))

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
                      merge-into-vector)))

     (def transform-original
       (partial s/transform* :original (comp vector
                                             (partial every? true?))))

     (def arrange-candidate-sentence
       (comp transform-original
             set-back
             augment-forth
             concatenate-into-sentence))

     (defn set-index
       [index sentences]
       (s/setval [s/ALL :index] [index] sentences))

     (def mapcat-indexed
       (comp (partial apply concat)
             map-indexed))

     (def group-by-vals
       (aid/curry 2 (comp vals
                          group-by)))

     (def structure-lm
       (comp (group-by-vals (comp count
                                  :lemma_))
             (partial mapcat-indexed set-index)
             (partial map (comp (partial map arrange-candidate-sentence)
                                (aid/build cons
                                           identity
                                           get-variants)
                                arrange-original-sentence))
             partition-sentences
             arrange-tokens
             parse-remotely))

     (def get-selection-path
       (partial (aid/flip get-resources-path) "selection.json"))

     (def parse-keywordize
       (partial (aid/flip parse-string) true))

     (def get-run
       (memoize (comp :run
                      parse-keywordize
                      slurp
                      get-selection-path)))

     (defn get-runs-path
       [model-name & more]
       (apply get-resources-path model-name "runs" more))

     (aid/defcurried get-tuned-path
                     [model-name* timestamp extension]
                     (->> extension
                          (append-extension "tuned")
                          (get-runs-path model-name* timestamp)))

     (def slurp-read-string
       (comp read-string
             slurp))

     (utils/defmemoized get-tuned
                        [model-name*]
                        (-> (get-tuned-path model-name*
                                            (get-run model-name*)
                                            edn-name)
                            slurp
                            edn/read-string))

     (def stoi-request
       {:body         (generate-string {:action :get-stoi})
        :content-type :json})

     (utils/defmemoized get-stoi
                        [port]
                        (-> port
                            get-origin
                            (client/post stoi-request)
                            :body
                            parse-string))

     (def integers
       (range))

     (def positive-integers
       (map inc integers))

     (defn make-within
       [minimum supremum]
       (aid/build and
                  (partial <= minimum)
                  (partial > supremum)))

     (defn <$
       [v fv]
       (m/<$> (constantly v) fv))

     (def lm-unk-index
       0)

     (defn get-mask
       [reference minimum supremum]
       (aid/case-eval minimum
                      lm-unk-index (-> true
                                       integer
                                       (<$ reference))
                      (map (comp integer
                                 (make-within minimum supremum))
                           reference)))

     (utils/defmemoized get-index-suprema
                        []
                        (->> lm-name
                             get-tuned
                             :cutoffs
                             (s/setval s/AFTER-ELEM (-> lm-port
                                                        get-stoi
                                                        keys
                                                        count))))
     ;get-evaluation-steps seems faster with memoization than without it.
     ;(-> "I opened it."
     ;    structure-evaluation-sentences
     ;    get-evaluation-steps
     ;    generate-string
     ;    count
     ;    time)
     ;"Elapsed time: 2503.398474 msecs"
     ;=> 7922
     ;(def get-index-suprema
     ;  #(->> (get-lm-tuned)
     ;        :cutoffs
     ;        (s/setval s/AFTER-ELEM (-> lm-port
     ;                                   get-stoi
     ;                                   keys
     ;                                   count))))
     ;
     ;(-> "I opened it."
     ;    structure-evaluation-sentences
     ;    get-evaluation-steps
     ;    generate-string
     ;    count
     ;    time)
     ;"Elapsed time: 13249.900044 msecs"
     ;=> 7922

     (def get-head-supremum
       #(first (get-index-suprema)))

     (def get-tail
       #(->> (get-index-suprema)
             (take-while (partial >= %))
             count
             dec))

     (def cut-off
       #((command/if-then (partial < (get-head-supremum))
                          (comp (partial + (get-head-supremum))
                                get-tail))
          %))

     (aid/defcurried get-cluster
                     [reference minimum supremum]
                     {:index     (->> (map *
                                           (get-mask reference
                                                     minimum
                                                     supremum)
                                           positive-integers)
                                      (filter pos?)
                                      (map dec))
                      :minimum   minimum
                      :length    (- supremum minimum)
                      :mask      (get-mask reference minimum supremum)
                      :reference (->> reference
                                      ((aid/case-eval
                                         minimum
                                         lm-unk-index (partial map
                                                               cut-off)
                                         (partial filter
                                                  (make-within minimum
                                                               supremum))))
                                      (map (partial (aid/flip -) minimum)))})

     (def get-clusters
       #(-> %
            get-cluster
            (map (->> lm-name
                      get-tuned
                      :cutoffs
                      (cons lm-unk-index))
                 (get-index-suprema))))

     (def get-lm-index
       #(-> lm-port
            get-stoi
            (get % lm-unk-index)))

     (utils/defmemoized get-document-unk-index
                        []
                        (-> document-port
                            get-stoi
                            keys
                            count))
     ;(defn get-document-unk-index
     ;  []
     ;  (-> document-port
     ;      get-stoi
     ;      keys
     ;      count))
     ;(time (get-document-unk-index))
     ;"Elapsed time: 39.880357 msecs"
     ;=> 174015

     (def get-document-index
       #(-> document-port
            get-stoi
            (get % (get-document-unk-index))))

     (def directions
       (s/multi-path :forth :back))

     (def transform-string
       (partial s/transform*
                [directions (s/multi-path :source :reference) s/ALL]
                get-lm-index))

     (def get-batch
       (comp (partial transfer* :product (aid/build *
                                                    :batch-size
                                                    :length))
             (partial transfer* :length (comp count
                                              first
                                              :source
                                              :forth))
             (partial transfer* :batch-size (comp count
                                                  :index))
             (partial s/transform*
                      directions
                      (partial transfer* :clusters (comp get-clusters
                                                         :reference)))
             (partial apply deep-merge-with concat)
             (partial map (comp (partial s/transform*
                                         [directions
                                          (s/multi-path :source :character)]
                                         vector)
                                transform-string))))

     (def get-lm-evaluation-steps
       #(mapcat (comp (partial map get-batch)
                      (partial partition-all (-> lm-name
                                                 get-tuned
                                                 :batch-size)))
                %))

     (def kubernetes-name
       "kubernetes")

     (def get-evaluate-request
       (comp get-json-request
             (partial array-map :action :evaluate :data)))

     (def separate*
       (comp (partial apply map merge)
             (partial map (aid/build map
                                     (comp (aid/curry 2 array-map)
                                           first)
                                     last))))

     (def filter-map
       (comp (partial into {})
             filter))

     (def separate
       (comp separate*
             (partial filter-map (comp sequential?
                                       val))))

     (def set-negative-log-probability
       (transfer* :negative-log-probability (comp (partial map incanter/sum)
                                                  :output)))
     (def partition-output
       (transfer* :output (aid/build partition
                                     :length
                                     :output)))

     (def grade-lm
       (comp (partial sort-by (comp :index
                                    first))
             (group-by-vals :index)
             (partial mapcat (comp separate
                                   set-negative-log-probability))
             (aid/build (partial map (comp partition-output
                                           merge))
                        identity
                        (comp (partial map (partial array-map :output))
                              :body
                              (partial client/post (get-origin lm-port))
                              get-evaluate-request))
             get-lm-evaluation-steps))

     (def join-text
       (comp str/join
             (partial map :text_with_ws)))

     (def generate-lm-inference
       (comp join-text
             (partial map
                      (partial apply min-key :negative-log-probability))))

     (def main-file
       "main.js")

     (def js-directory
       "js")

     (def get-hyperparameter
       (comp edn/read-string
             slurp
             io/resource
             (partial (aid/flip get-path) "hyperparameter.edn")))

     (def hyperparameter
       (get-hyperparameter document-name))

     (def get-files
       (partial (aid/flip fs/find-files*) fs/file?))

     (def get-dataset-path
       (partial get-resources-path
                document-name
                "datasets"
                (:dataset hyperparameter)))

     (def get-edn-request
       (comp (partial array-map :as :clojure :body)
             pr-str))

     (def post-macchiato
       (partial client/post (get-origin alteration-port)))

     (def alter-remotely
       (comp :body
             post-macchiato
             get-edn-request))

     (def structure-document
       (comp alter-remotely
             (partial array-map :action :set-alternatives :data)
             arrange-document-tokens
             parse-remotely))

     (def make-+
       #(comp (juxt (comp fs/mkdirs
                          fs/parent
                          first)
                    (partial apply %))
              vector))

     (def spit+
       (make-+ spit))

     (def get-organized-path
       (partial get-dataset-path "organized"))

     (def get-training-path
       (partial get-organized-path "training"))

     (def length-path
       (get-organized-path "length.edn"))

     (def cache-path
       "python/.vector_cache")

     (def word2vecf-filename
       "deps.words.bz2")

     (def word2vecf-url
       (get-path "http://u.cs.biu.ac.il/~yogo/data/syntemb" word2vecf-filename))

     (def word2vecf-sha
       "38281adc0a0ee1abf50c2cc6c90372bdef968cb08d4e3a2d4c68c2924b639e64")

     (defmacro with-sh-dir+
       [path & body]
       `(do (fs/mkdirs ~path)
            (sh/with-sh-dir ~path
                            ~@body)))

     (defn >>
       ([mv]
        mv)
       ([mv & more]
        (apply m/>> mv more)))

     (def install-word2vecf
       #(with-sh-dir+
          cache-path
          (>> (command/wget word2vecf-url)
              (aid/case-eval (->> word2vecf-filename
                                  (get-path cache-path)
                                  io/input-stream
                                  hash/sha256
                                  bytes->hex)
                             word2vecf-sha (either/right "")
                             (either/left "Checksums don't match."))
              (command/bzip2 "-df" word2vecf-filename))))

     (def get-document-evaluation-steps
       (comp (partial s/transform* (s/multi-path :source :reference) vector)
             merge-into-vector
             (partial s/transform* [s/ALL :source] get-document-index)))

     (def flatten-sequential
       (partial s/transform* [s/MAP-VALS sequential?] flatten))

     (def grade-document
       (comp flatten-sequential
             (aid/build merge
                        identity
                        (comp :body
                              (partial client/post (get-origin document-port))
                              get-evaluate-request))
             get-document-evaluation-steps))

     (def get-article
       (comp {0 ""
              1 "the"
              2 "a"}
             (partial (aid/flip quot) 2)
             :inference))

     (def title?
       (command/if-then-else :start
                             (comp (partial = "")
                                   get-article)
                             :is_title))

     (def count-common-prefix
       (comp count
             superstring/common-prefix))

     (defn case-inference
       [token s]
       (aid/casep
         token
         (aid/build or
                    :is_lower
                    :is_title)
         ((aid/casep token
                     title? str/capitalize
                     identity)
           s)
         (str (subs (:text_with_ws token)
                    0
                    (count-common-prefix (:character-with-whitespace token)
                                         s))
              (subs s
                    (count-common-prefix (:character-with-whitespace token)
                                         s)
                    (count s)))))

     (def consolidate
       #(aid/casep
          %
          :mask (:text_with_ws %)
          (str
            ((aid/casep %
                        :start str/capitalize
                        identity)
              (get-article %))
            (case (get-article %)
              "" ""
              (aid/casep %
                         :hyphen "-"
                         " "))
            (aid/casep
              %
              :proper
              (:text_with_ws %)
              (case-inference %
                              (command/if-then-else (comp even?
                                                          :inference)
                                                    :character-with-whitespace
                                                    :alternative
                                                    %))))))

     (def generate-document-inference
       (comp str/join
             (partial map consolidate)
             separate))

     (def option
       {:host "0.0.0.0"})

     (def get-evaluation-path
       (comp get-organized-path
             (partial (aid/flip append-extension) edn-name)
             name))

     (defn get-run-path
       [model-name & more]
       (apply get-runs-path
              model-name
              (get-run model-name)
              more))

     (def python-name
       "python")

     (def get-shell-command
       (comp (partial str/join " && ")
             (partial map command/join-whitespace)))

     (def run-commands
       (comp (partial command/bash "-c")
             #(str "\"" % "\"")
             get-shell-command))

     (def environment-name
       "environment")

     (def venv-commands
       [["cd" python-name]
        [python-name "-m" "venv" environment-name]
        ["." (get-path environment-name
                       "bin"
                       "activate")]
        ["pip"
         "install"
         "-r"
         (->> "pu.txt"
              (str (aid/casep (command/hash "nvidia-smi")
                              either/right? "g"
                              "c"))
              (get-path "requirements"))]
        [python-name "-m" "spacy" "download" "en"]])

     (def install-venv
       #(run-commands venv-commands))

     (def get-forwarding
       (comp (partial str/join ":")
             (partial repeat 2)))

     (def install-npm
       #(command/lein "npm" "install"))

     (defn get-download-arguments
       [s]
       ["-qO-" s "|" "tar" "xz"])

     (def get-cloud-storage-path
       (comp (partial get-path
                      "https://storage.googleapis.com/wikipediadataset")
             (partial (aid/flip append-extension) "tar.gz")))

     (def map->>
       (comp (partial apply >>)
             map))

     (def nippy-name
       "npy")))
