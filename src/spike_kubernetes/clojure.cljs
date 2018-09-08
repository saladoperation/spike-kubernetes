(ns spike-kubernetes.clojure
  (:require [reagent.core :as r]))

(def app
  [:div
   [:textarea]])

(->> "app"
     js/document.getElementById
     (r/render app))
