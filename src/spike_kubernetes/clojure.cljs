(ns spike-kubernetes.clojure
  (:require [reagent.core :as r]))

(def app
  [:div
   [:textarea]])

(r/render app (js/document.getElementById "app"))
