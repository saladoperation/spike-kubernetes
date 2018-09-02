(ns spike-kubernetes.web
  (:require [reagent.core :as r]))

(def app
  [:div
   [:textarea]])

(r/render app (js/document.getElementById "app"))
