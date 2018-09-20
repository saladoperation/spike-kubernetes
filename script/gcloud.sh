#!/usr/bin/env bash
gcloud auth activate-service-account --key-file=<(echo $GCLOUD_KEY) &&
gcloud -q config set project $GCLOUD_PROJECT &&
gcloud -q config set compute/zone $GCLOUD_COMPUTE_ZONE &&
gcloud container clusters get-credentials $GCLOUD_CLUSTER &&
kubectl apply -f kubernetes.txt
