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
       (comp (partial client/post (get-origin 8000))
             get-json-request))))
