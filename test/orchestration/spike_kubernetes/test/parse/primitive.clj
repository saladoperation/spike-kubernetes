(ns spike-kubernetes.test.parse.primitive
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test :as clojure-test]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [aid.core :as aid]
            [cats.core :as m]
            [spike-kubernetes.parse.primitive :as primitive]))

(def num-tests
  10)

(aid/defcurried ncall
                [n f x]
                (nth (iterate f x) n))

(def parser
  (gen/bind gen/any
            #(gen/elements (conj (map (comp primitive/satisfy
                                            constantly)
                                      [true false])
                                 (primitive/pure %)
                                 primitive/mempty))))

(defn identity*
  [f]
  (prop/for-all [parser* parser
                 as (gen/vector gen/any)]
                (->> [f
                      identity]
                     (map #((% parser*) as))
                     (apply =))))

(def monad-identity
  #(identity* (comp primitive/join
                    (% primitive/pure))))

(clojure-test/defspec monad-left
                      num-tests
                      (monad-identity identity))

(clojure-test/defspec monad-right
                      num-tests
                      (monad-identity (m/curry 2 m/<$>)))

(clojure-test/defspec
  monad-associativity
  num-tests
  (prop/for-all [parser* parser
                 as (gen/vector gen/any)]
                (->> [(comp primitive/join
                            (partial m/<$> primitive/join))
                      (ncall 2 primitive/join)]
                     (map #((% (ncall 2 primitive/pure parser*)) as))
                     (apply =))))

(clojure-test/defspec
  semigroup
  num-tests
  (prop/for-all [[left-parser middle-parser right-parser] (gen/vector parser 3)
                 as (gen/vector gen/any)]
                (->> [(m/<> (m/<> left-parser middle-parser)
                            right-parser)
                      (m/<> left-parser
                            (m/<> middle-parser right-parser))]
                     (map (partial (aid/flip aid/funcall) as))
                     (apply =))))

(clojure-test/defspec
  monoid-left
  num-tests
  (prop/for-all [parser* parser
                 as (gen/vector gen/any)]
                (->> [(partial m/<> primitive/mempty)
                      identity]
                     (map #((% parser*) as))
                     (apply =))))
