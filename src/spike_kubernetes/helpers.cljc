(ns spike-kubernetes.helpers
  #?(:clj
     (:require [clojure.string :as str]
               [aid.core :as aid]
               [cheshire.core :refer :all]
               [clj-http.client :as client]
               [com.rpl.specter :as s]
               [environ.core :refer [env]]
               [spike-kubernetes.command :as command])))

(def clojurescript-port
  3000)

#?(:clj
   (do
     (def clojure-name
       "clojure")

     (def clojurescript-name
       "clojurescript")

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

     (defn get-image
       [language]
       (str username
            "/"
            language
            (if-else empty?
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
                             (partial apply merge-with (partial deep-merge-with f))
                             (partial apply f)
                             more))

     (def deep-merge
       (partial deep-merge-with (comp last
                                      vector)))
     (def join-paths
       (comp (partial str/join "/")
             vector))

     (def clojure-port
       8080)

     (def parse-name
       "parse")

     (def port
       {clojure-name       clojure-port
        clojurescript-name clojurescript-port
        parse-name         8000})

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
             (partial client/post (get-origin 8000))
             get-json-request))

     (def set-starts
       (comp (partial s/setval* [s/FIRST :start] true)
             (partial map (transfer* :start :is_sent_start))))

     (def quotation-mark?*
       (comp (partial = "\"")
             :lower_))

     (defn make-quotation-mark?
       [tag]
       (aid/build and
                  (comp (partial = tag)
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

     (def proper-tags
       #{"NNP" "NNPS"})

     (defn set-proper
       [tokens token]
       (->> token
            (s/setval :proper (and (->> token
                                        :tag_
                                        (contains? proper-tags))
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
             set-starts))))
