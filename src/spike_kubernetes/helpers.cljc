(ns spike-kubernetes.helpers
  (:require [aid.core :as aid]
            [com.rpl.specter :as s]
            [environ.core :refer [env]]
            [spike-kubernetes.command :as command]))

#?(:clj
   (do
     (def clojure-name
       "clojure")

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

     (def clojure-image
       (get-image clojure-name))

     (aid/defcurried transfer*
                     [apath f m]
                     (s/setval apath (f m) m))))
