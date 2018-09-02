#!/usr/bin/env bash
lein test &&
lein cljsbuild once prod &&
lein uberjar &&
docker build -f docker/clojure/Dockerfile -t relationship/spike-kubernetes . &&
docker run -d relationship/spike-kubernetes
