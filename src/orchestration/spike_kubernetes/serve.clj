(ns spike-kubernetes.serve
  (:require [hiccup.core :as hiccup]
            [immutant.web :as web]
            [mount.core :as mount :refer [defstate]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response]]))

(defn handler
  [request]
  (response (case (:request-method request)
              :get (-> [:html {:lang "en"}
                        [:head
                         [:meta {:charset "UTF-8"}]]
                        [:body
                         [:div {:id "app"}]
                         [:script {:src "js/main.js"}]]]
                       hiccup/html)
              (-> request
                  :body
                  slurp))))

(def start
  (partial web/run (wrap-resource handler "public") {:host "0.0.0.0"}))

(defstate server
          :start (start)
          :stop (web/stop))
