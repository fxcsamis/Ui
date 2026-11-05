#!/data/data/com.termux/files/usr/bin/bash
# Puts the CONTENTS of vosk-model-small-hi-0.22 into app/src/main/assets/model-hi/ (not the zip, not the top folder).
# Usage:  bash setup-vosk-model.sh ~/storage/downloads/vosk-model-small-hi-0_22.zip
set -e
ZIP="${1:-$HOME/storage/downloads/vosk-model-small-hi-0_22.zip}"
[ -f "$ZIP" ] || { echo "Model zip not found: $ZIP"; exit 1; }
TMP="$(mktemp -d)"
unzip -q "$ZIP" -d "$TMP"
SRC="$(find "$TMP" -maxdepth 2 -type d -name am | head -n 1)"
[ -n "$SRC" ] || { echo "Could not find the model folder (am/) inside the zip"; exit 1; }
SRC="$(dirname "$SRC")"
DEST="app/src/main/assets/model-hi"
rm -rf "$DEST"
mkdir -p "$DEST"
cp -r "$SRC"/. "$DEST"/
rm -rf "$TMP"
echo "Model copied to $DEST:"
ls "$DEST"
test -f "$DEST/am/final.mdl" && test -f "$DEST/graph/Gr.fst" && echo "OK: am/final.mdl and graph/Gr.fst are in place" || { echo "ERROR: model files missing"; exit 1; }
