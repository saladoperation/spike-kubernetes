(ns spike-kubernetes.command
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [aid.core :as aid]
            [cats.monad.either :as either]
            [com.rpl.specter :as s]
            [me.raynes.fs :as fs]))

(aid/defcurried if-then-else
                ;TODO move this function to aid
                [if-function then-function else-function x]
                ((aid/build if
                            if-function
                            then-function
                            else-function)
                  x))

(aid/defcurried if-then
                ;TODO move this function to aid
                [if-function then-function else]
                (if-then-else if-function then-function identity else))

(def err
  #"stty: 'standard input': Inappropriate ioctl for device\n")

(defn execute
  [shell & more]
  (->> more
       (concat ["source" (fs/expand-home (str "~/." shell "rc")) "&&"])
       (str/join " ")
       (sh/sh shell "-c")
       (s/transform :err (fn [s]
                           (str/replace s err "")))
       ((if-then (comp empty?
                       :err)
                 (partial (aid/flip dissoc) :err)))))

(def escape
  (if-then (partial re-find #"^\d")
           (partial str "*")))

(def out
  (comp str/trim-newline
        :out))

(defn monadify
  [shell command]
  (comp (aid/build if
                   (comp zero?
                         :exit)
                   (comp either/right
                         out)
                   (comp either/left
                         (juxt out
                               (comp str/trim-newline
                                     :err))))
        (partial execute shell command)))

(defn make-defcommand
  [shell]
  (fn [command]
    (eval `(def ~(-> command
                     escape
                     symbol)
             (monadify ~shell ~command)))))

(defn defcommands
  [shell]
  (->> "compgen -bc"
       (execute shell)
       :out
       str/split-lines
       set
       (run! (make-defcommand shell))))

(def ent
  (->> "user.name"
       System/getProperty
       (sh/sh "getent" "passwd")))

(def parse-ent
  (comp second
        (partial re-find #"\/([^/]+)\n")
        :out))

(-> ent
    parse-ent
    defcommands)
