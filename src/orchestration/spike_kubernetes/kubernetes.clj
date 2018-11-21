(ns spike-kubernetes.kubernetes
  (:require [clojure.string :as str]
            [aid.core :as aid]
            [cheshire.core :refer :all]
            [spike-kubernetes.command :as command]
            [spike-kubernetes.helpers :as helpers]))

(def label
  {:label "label"})

(defn get-container
  [image-name]
  {:image     (helpers/get-image image-name)
   :name      image-name
   :resources {:limits {"nvidia.com/gpu" (aid/case-eval image-name
                                                        helpers/lm-name 1
                                                        0)}}})
;When grade-lm is run on a GPU:
;(-> "Jaws is a 1975 American thriller film directed by Steven Spielberg and based on Peter Benchley's 1974 novel of the same name. In the story, a giant man-eating great white shark attacks beachgoers on Amity Island, a fictional New England summer resort town, prompting the local police chief to hunt it with the help of a marine biologist and a professional shark hunter. The film stars Roy Scheider as police chief Martin Brody, Robert Shaw as shark hunter Quint, Richard Dreyfuss as oceanographer Matt Hooper, Murray Hamilton as Larry Vaughn, the mayor of Amity Island, and Lorraine Gary as Brody's wife, Ellen. The screenplay is credited to both Benchley, who wrote the first drafts, and actor-writer Carl Gottlieb, who rewrote the script during principal photography."
;    helpers/structure-lm
;    helpers/grade-lm
;    helpers/generate-lm-inference
;    helpers/structure-document
;    helpers/grade-document
;    helpers/generate-document-inference
;    count
;    time)
;"Elapsed time: 23932.976647 msecs"
;=> 760
;When grade-lm is run on 8 CPUs:
;"Elapsed time: 52743.292594 msecs"
;=> 760

(def deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :spec       {:selector {:matchLabels label}
                :template {:metadata {:labels label}
                           :spec     {:containers (map get-container
                                                       helpers/image-names)}}}})

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
  (comp command/join-newline
        (partial map generate-string)))

(def spit-kubernetes
  #(->>
     resources*
     get-json-lines
     (spit (helpers/get-resources-path
             helpers/kubernetes-name
             (helpers/append-extension helpers/kubernetes-name
                                       "txt")))))
