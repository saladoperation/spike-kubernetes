#!/usr/bin/env bash
rsync_selectively(){
    rsync -azP --exclude=.idea --filter=':- /python/.gitignore' $(pwd) $1
};
rsync_selectively $1
while inotifywait -e create .git; do
    #.gitignore in the project root seems to be used as a filter by default
    rsync_selectively $1
done
