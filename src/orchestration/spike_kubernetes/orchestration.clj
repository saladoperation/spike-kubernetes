(ns spike-kubernetes.orchestration
  (:require [hiccup.core :as hiccup]
            [immutant.web :as web]
            [mount.core :refer [defstate]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response]]
            [spike-kubernetes.helpers :as helpers]))

(def handler
  #(response
     (case (:request-method %)
       :get (hiccup/html
              [:html {:lang "en"}
               [:head
                [:meta {:charset "UTF-8"}]]
               [:body
                [:div {:id helpers/app-name}]
                [:script {:src (helpers/get-path helpers/js-directory
                                                 helpers/main-file)}]]])
       (-> %
           :body
           slurp
           helpers/structure-lm
           helpers/grade-lm
           helpers/generate-lm-inference
           helpers/structure-document
           helpers/grade-document
           helpers/generate-document-inference))))

(def start
  (partial web/run (wrap-resource handler "public") helpers/option))

(defstate server
          :start (start)
          :stop (web/stop))
