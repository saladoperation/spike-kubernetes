#!/usr/bin/env bash
source script/python/helpers.sh
#with export FLASK_DEBUG=1, Flask seems to use almost twice as much memory as without it
gunicorn -b localhost:8001 -t 3000 spike_kubernetes.document.evaluate:app
