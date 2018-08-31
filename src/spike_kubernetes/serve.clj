(ns spike-kubernetes.serve
  (:require [immutant.web :as web]
            [mount.core :as mount :refer [defstate]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [not-found]]))

(defn handler
  [request]
  ;TODO implement this function
  (not-found "404"))

(def start
  (partial web/run
           (-> handler
               (wrap-resource "public")
               wrap-edn-params)
           {:host "0.0.0.0"}))

(defstate server
          :start (start)
          :stop (web/stop))
