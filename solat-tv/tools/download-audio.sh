#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/src/main/res/raw
# Licensed, unmodified CC BY-SA 4.0 recording; attribution is in README and the app.
curl --fail --location --retry 2 --connect-timeout 15 --max-time 120 \
  'https://upload.wikimedia.org/wikipedia/commons/7/7d/The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3' \
  --output app/src/main/res/raw/azan.mp3.download
mv app/src/main/res/raw/azan.mp3.download app/src/main/res/raw/azan.mp3
sha256sum app/src/main/res/raw/azan.mp3
