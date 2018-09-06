#!/usr/bin/env bash
source script/flask/parse/helpers.sh
#with export FLASK_DEBUG=1, Flask seems to use almost twice as much memory as without it
gunicorn -t 3000 spike_kubernetes.parse:app
