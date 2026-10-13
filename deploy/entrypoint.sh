#!/bin/sh
# First start on an empty data volume: restore uploaded images from /seed/uploads.tar.gz
# (mounted by compose.prod.yaml). Once the folder has files the archive is never read again.
set -e
uploads="${UPLOAD_DIR:-/app/data/uploads}"
if [ -f /seed/uploads.tar.gz ] && [ -z "$(ls -A "$uploads" 2>/dev/null)" ]; then
    mkdir -p "$uploads"
    tar -xzf /seed/uploads.tar.gz -C "$uploads"
    echo "Restored $(find "$uploads" -type f | wc -l) uploaded files from /seed/uploads.tar.gz"
fi
exec java -XX:MaxRAMPercentage=75 -jar /app/techshop.jar "$@"
