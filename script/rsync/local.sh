#!/usr/bin/env bash
while inotifywait -e create -r resources/document/runs; do
    rsync -azP --exclude=*.pt resources/document/runs share
done
