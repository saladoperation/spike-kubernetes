#!/usr/bin/env bash
source script/python/helpers.sh
export FLASK_APP=spike_kubernetes/document/evaluation.py FLASK_DEBUG=1
flask run -p 8002
