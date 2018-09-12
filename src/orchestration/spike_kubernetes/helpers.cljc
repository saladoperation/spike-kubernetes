(ns spike-kubernetes.helpers
  #?(:clj
     (:require [clojure.java.io :as io]
               [clojure.string :as str]
               [clojure.tools.reader.edn :as edn]
               [aid.core :as aid]
               [cheshire.core :refer :all]
               [clj-http.client :as client]
               [com.rpl.specter :as s]
               [environ.core :refer [env]]
               [me.raynes.fs :as fs]
               [spike-kubernetes.command :as command])))

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

     (def parse-name
       "parse")

     (def document-name
       "document")

     (def parse-port
       8000)

     (def port
       {orchestration-name orchestration-port
        alteration-name    alteration-port
        parse-name         parse-port
        document-name      8001})

     (def get-origin
       (partial str "http://localhost:"))

     (def get-json-request
       (comp (partial assoc
                      {:as           :json
                       :content-type :json}
                      :body)
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

     (defn set-proper
       [tokens token]
       (->> token
            (s/setval :proper (and (-> token
                                       :tag_
                                       (str/starts-with? "NNP"))
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
       (partial command/if-then-else
                article-removal?
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

     (def altering-tags
       #{"NNS" "VBZ"})

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

     (def set-sos
       (partial s/setval* s/BEFORE-ELEM {:proper       false
                                         :quote        false
                                         :forth        {:source "sos"}
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
                [s/ALL s/ALL]
                (comp set-remove-tokens
                      (partial map
                               (comp (partial s/setval*
                                              (s/multi-path :original
                                                            :proper
                                                            :quote)
                                              false)
                                     set-forth-source)))))

     (def get-nodes
       (comp (partial map (comp arrange-line
                                (partial s/transform*
                                         [s/ALL s/ALL]
                                         parse-remotely)
                                parse-line))
             (partial mapcat str/split-lines)
             (partial map slurp)
             get-files
             io/resource
             (partial join-paths "lm" "confusions")))

     (def arrange-evaluation-sentences
       ;TODO implement this function
       (comp (partial map arrange-original-sentence)
             partition-sentences
             arrange-tokens*))

     (def structure-evaluation-sentences
       (comp arrange-evaluation-sentences
             parse-remotely))))
