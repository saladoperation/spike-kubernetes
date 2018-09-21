(ns spike-kubernetes.helpers
  #?(:clj
     (:require [clojure.java.io :as io]
               [clojure.set :as set]
               [clojure.string :as str]
               [clojure.tools.reader.edn :as edn]
               [aid.core :as aid]
               [cats.builtin]
               [cats.core :as m]
               [cheshire.core :refer :all]
               [clj-http.client :as client]
               [com.rpl.specter :as s]
               [compliment.utils :as utils]
               [environ.core :refer [env]]
               [incanter.core :as incanter]
               [me.raynes.fs :as fs]
               [spike-kubernetes.command :as command]
               [spike-kubernetes.parse.core :as parse])))

(def alteration-port
  3000)

(def app-name
  "app")

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

     (def model-name
       {lm-port lm-name
        8002    "document"})

     (def python-name
       (s/setval parse-port parse-name model-name))

     (def image-names
       (->> python-name
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

     (def confusions-name
       "confusions")

     (def get-resources-path
       (partial get-path "resources"))

     (def n-infimum
       1)

     (def preparation-name
       "preparation")

     (def preparation-filename
       (str preparation-name ".edn"))

     (utils/defmemoized get-preparation
                        []
                        (-> preparation-filename
                            get-resources-path
                            slurp
                            edn/read-string))

     (defn screen
       [sentence]
       (->> (get-preparation)
            :n-upperbound
            (range n-infimum)
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
                     (get ((:alternative (get-preparation)) replacement-source)
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

     (def multiton?
       (comp (partial < 1)
             count))

     (aid/defcurried set-a-text-with-wss
                     [original replacement]
                     ((aid/casep replacement
                                 trim? trim-last
                                 identity)
                       (command/if-then multiton?
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
             parse-remotely))

     (def get-selection-path
       (partial (aid/flip get-resources-path) "selection.json"))

     (def lm-selection-path
       (get-selection-path lm-name))

     (def parse-keywordize
       (partial (aid/flip parse-string) true))

     (utils/defmemoized get-lm-run
                        []
                        (-> lm-selection-path
                            slurp
                            parse-keywordize
                            :run))

     (defn get-runs-path
       [model & more]
       (apply get-resources-path model "runs" more))

     (aid/defcurried get-tuned-path
                     [model extension timestamp]
                     (->> extension
                          (str "tuned.")
                          (get-runs-path model timestamp)))

     (utils/defmemoized get-lm-tuned
                        []
                        (->> (get-lm-run)
                             (get-tuned-path lm-name "edn")
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
       [infimum upperbound]
       (aid/build and
                  (partial <= infimum)
                  (partial > upperbound)))

     (defn <$
       [v fv]
       (m/<$> (constantly v) fv))

     (def unk-index
       0)

     (defn get-mask
       [reference infimum upperbound]
       (aid/case-eval infimum
                      unk-index (-> true
                                    integer
                                    (<$ reference))
                      (map (comp integer
                                 (make-within infimum upperbound))
                           reference)))

     (utils/defmemoized get-index-upperbounds
                        []
                        (->> (get-lm-tuned)
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
     ;(def get-index-upperbounds
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

     (def get-head-upperbound
       #(first (get-index-upperbounds)))

     (def get-tail
       #(->> (get-index-upperbounds)
             (take-while (partial >= %))
             count
             dec))

     (def cut-off
       #((command/if-then (partial < (get-head-upperbound))
                          (comp (partial + (get-head-upperbound))
                                get-tail))
          %))

     (aid/defcurried get-cluster
                     [reference infimum upperbound]
                     {:index     (->> (map *
                                           (get-mask reference
                                                     infimum
                                                     upperbound)
                                           positive-integers)
                                      (filter pos?)
                                      (map dec))
                      :infimum   infimum
                      :length    (- upperbound infimum)
                      :mask      (get-mask reference infimum upperbound)
                      :reference (->>
                                   reference
                                   ((aid/case-eval
                                      infimum
                                      unk-index (partial map
                                                         cut-off)
                                      (partial filter
                                               (make-within infimum
                                                            upperbound))))
                                   (map (partial (aid/flip -) infimum)))})

     (def get-clusters
       #(-> %
            get-cluster
            (map (->> (get-lm-tuned)
                      :cutoffs
                      (cons unk-index))
                 (get-index-upperbounds))))

     (def get-index
       #(-> lm-port
            get-stoi
            (get % unk-index)))

     (def directions
       (s/multi-path :forth :back))

     (def transform-string
       (partial s/transform*
                [directions (s/multi-path :source :reference) s/ALL]
                get-index))

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

     (def get-evaluation-steps
       #(mapcat (comp (partial map get-batch)
                      (partial partition-all (:batch-size (get-lm-tuned))))
                %))

     (def kubernetes-name
       "kubernetes")

     (def get-evaluate-request
       (comp get-json-request
             (partial array-map :action :evaluate :data)))

     (def separate
       (comp (partial apply map merge)
             (partial map (aid/build map
                                     (comp (aid/curry 2 array-map)
                                           first)
                                     last))))

     (def set-negative-log-probability
       (transfer* :negative-log-probability (comp (partial map incanter/sum)
                                                  :output)))
     (def partition-output
       (transfer* :output (aid/build partition
                                     :length
                                     :output)))

     (def grade-lm
       (comp (group-by-vals :index)
             (partial mapcat (comp separate
                                   (partial (aid/flip select-keys)
                                            #{:index
                                              :negative-log-probability
                                              :output
                                              :original
                                              :text_with_ws})
                                   set-negative-log-probability))
             (aid/build (partial map (comp partition-output
                                           merge))
                        identity
                        (comp (partial map (partial array-map :output))
                              :body
                              (partial client/post (get-origin lm-port))
                              get-evaluate-request))))

     (def consolidate-text
       (comp (partial str/join "")
             (partial map :text_with_ws)))

     (def generate-lm-inference
       (comp consolidate-text
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

     (def document-hyperparameter
       (get-hyperparameter "document"))

     (def get-files
       (partial (aid/flip fs/find-files*) fs/file?))))
