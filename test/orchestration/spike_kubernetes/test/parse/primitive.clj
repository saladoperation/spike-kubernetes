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

(clojure-test/defspec
  monad-associativity
  num-tests
  (gen/let [a gen/any
            as (gen/vector gen/any)
            parser (gen/elements (conj (map (comp primitive/satisfy
                                                  constantly)
                                            [true false])
                                       (primitive/pure a)
                                       primitive/mempty))]
           (prop/for-all []
                         (->> [(comp primitive/join
                                     (partial m/<$> primitive/join))
                               (ncall 2 primitive/join)]
                              (map #((% (ncall 2 primitive/pure parser)) as))
                              (apply =)))))
