(ns kubernetes
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cheshire.core :refer :all]
            [me.raynes.fs :as fs]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def clojure-port
  8080)

(def transfer-name
  (helpers/transfer* [:metadata :name] (comp str/lower-case
                                             :kind)))

(def ingress
  {:apiVersion "extensions/v1beta1"
   :kind       "Ingress"
   :metadata   {:annotations {"kubernetes.io/ingress.global-static-ip-name"
                              "ip"}}
   :spec       {:backend {:serviceName "backend"
                          :servicePort clojure-port}}})

(def service
  {:apiVersion "v1"
   :kind       "Service"
   :spec       {:type  "NodePort"
                :ports [{:port       80
                         :targetPort clojure-port}]}})

(def label
  {:label "label"})

(def get-container
  (comp (partial (aid/flip dissoc) :port)
        (helpers/transfer* :ports
                           (comp vector
                                 (partial array-map :containerPort)
                                 :port))
        (helpers/transfer* :image (comp helpers/get-image
                                        :name))))

(def deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :spec       {:selector {:matchLabels label}
                :template {:metadata {:labels label}
                           :spec     {:containers
                                      (map get-container
                                           #{{:name helpers/clojure-name
                                              :port clojure-port}})}}}})

(def resources*
  (map transfer-name #{ingress service deployment}))

(defn kubectl-apply
  [m]
  (let [temporary-file (fs/temp-file "spike-kubernetes")]
    (->> m
         generate-string
         (spit temporary-file))
    (command/kubectl "apply" "-f" temporary-file)))

(def all!
  (comp doall
        map))

(all! kubectl-apply resources*)
