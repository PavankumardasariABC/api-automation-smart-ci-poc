#!/usr/bin/env bash
# Package .cursor/skills (api automation) + installer into a shareable zip.
# Run from anywhere; resolves repo root from this script's location.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_DIR="$REPO_ROOT/build/cursor-api-automation-skills-bundle"
ZIP_PATH="$REPO_ROOT/build/cursor-api-automation-skills-bundle.zip"

SKILLS_SRC="$REPO_ROOT/.cursor/skills"
for skill in api-automation-smart-ci test-buckets-rules; do
  if [[ ! -d "$SKILLS_SRC/$skill" ]]; then
    echo "error: missing $SKILLS_SRC/$skill" >&2
    exit 1
  fi
done

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

rsync -a "$SKILLS_SRC/api-automation-smart-ci/" "$OUT_DIR/api-automation-smart-ci/"
rsync -a "$SKILLS_SRC/test-buckets-rules/" "$OUT_DIR/test-buckets-rules/"

cp "$SCRIPT_DIR/install.sh" "$OUT_DIR/install.sh"
chmod +x "$OUT_DIR/install.sh"
cp "$SCRIPT_DIR/BUNDLE_README.txt" "$OUT_DIR/README.txt"

if [[ -f "$REPO_ROOT/TEST_BUCKETS.md" ]]; then
  cp "$REPO_ROOT/TEST_BUCKETS.md" "$OUT_DIR/TEST_BUCKETS.reference.md"
fi

rm -f "$ZIP_PATH"
( cd "$REPO_ROOT/build" && zip -rq "cursor-api-automation-skills-bundle.zip" "cursor-api-automation-skills-bundle" )

echo "Bundle directory: $OUT_DIR"
echo "Zip:            $ZIP_PATH"
echo "Share the zip or the folder; recipients run ./install.sh <project-root>"
