(ns spike-kubernetes.core
  (:require [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]))

(def doc
  (r/atom {:source    ""
           :reference ""}))

(def form-template
  [:textarea {:field :text
              :id    :source}])

(defn app
  []
  [:div
   [bind-fields form-template doc]
   [:button]
   [:p (:reference @doc)]])

(->> "app"
     js/document.getElementById
     (r/render [app]))
