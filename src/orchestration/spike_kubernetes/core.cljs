(ns spike-kubernetes.core
  (:require [ajax.core :refer [POST]]
            [com.rpl.specter :as s]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]
            [spike-kubernetes.helpers :as helpers]))

(def doc
  (r/atom {:source    ""
           :reference ""}))

(def form-template
  [:textarea {:field :text
              :id    :source}])

(def handler
  (partial swap! doc (comp (partial apply s/setval* :reference)
                           reverse
                           vector)))

(defn app
  []
  [:div
   [bind-fields form-template doc]
   [:button {:on-click #(POST "/" {:handler handler
                                   :body    (:source @doc)})}]
   [:p (:reference @doc)]])

(->> helpers/app-name
     js/document.getElementById
     (r/render [app]))
