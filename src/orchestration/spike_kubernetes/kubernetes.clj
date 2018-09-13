(ns spike-kubernetes.kubernetes
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cheshire.core :refer :all]
            [spike-kubernetes.helpers :as helpers]))

(def label
  {:label "label"})

(defn get-container
  [k v]
  {:image (helpers/get-identifier v)
   :name  v
   :ports [{:containerPort k}]})

(def deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :spec       {:selector {:matchLabels label}
                :template {:metadata {:labels label}
                           :spec     {:containers
                                      (map (partial apply get-container)
                                           helpers/image-name)}}}})

(def service
  {:apiVersion "v1"
   :kind       "Service"
   :spec       {:ports    [{:port       helpers/orchestration-port
                            :targetPort helpers/orchestration-port}]
                :selector label
                :type     "NodePort"}})

(def get-name
  (comp str/lower-case
        :kind))

(def ingress
  {:apiVersion "extensions/v1beta1"
   :kind       "Ingress"
   :metadata   {:annotations
                {"kubernetes.io/ingress.global-static-ip-name" "ip"}}
   :spec       {:backend {:serviceName (get-name service)
                          :servicePort helpers/orchestration-port}}})

(def transfer-name
  (helpers/transfer* [:metadata :name] get-name))

(def resources*
  (map transfer-name #{ingress service deployment}))

(def get-json-lines
  (comp (partial str/join "\n")
        (partial map generate-string)))

(def spit-kubernetes
  #(->> resources*
        get-json-lines
        (spit "kubernetes.txt")))
