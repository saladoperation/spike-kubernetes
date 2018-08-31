(ns spike-kubernetes.serve
  (:require [hiccup.core :as hiccup]
            [immutant.web :as web]
            [mount.core :as mount :refer [defstate]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response]]))

(defn handler
  [request]
  ;TODO implement this function
  (-> [:html {:lang "en"}
       [:head
        [:meta {:charset "UTF-8"}]]
       [:body
        [:div {:id "app"}]
        [:script {:src "js/main.js"}]]]
      hiccup/html
      response))

(def start
  (partial web/run
           (-> handler
               (wrap-resource "public")
               wrap-edn-params)
           {:host "0.0.0.0"}))

(defstate server
          :start (start)
          :stop (web/stop))
