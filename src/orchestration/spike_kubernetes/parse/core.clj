(ns spike-kubernetes.parse.core
  (:refer-clojure :exclude [= some])
  (:require [spike-kubernetes.parse.derived :as derived]
            [spike-kubernetes.parse.primitive :as primitive]))

(def pure
  primitive/pure)

(def mempty
  primitive/mempty)

(def satisfy
  primitive/satisfy)

(def many
  derived/many)

(def some
  derived/some)

(def parse
  derived/parse)

(def any
  derived/any)

(def =
  derived/=)
