(ns spike-kubernetes.test.core
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [spike-kubernetes.test.clojurescript]))

(doo-all-tests #"spike-kubernetes\.test.+")

