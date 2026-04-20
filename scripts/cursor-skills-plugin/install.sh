#!/usr/bin/env bash
# Install Cursor Agent Skills (api-automation-smart-ci, test-buckets-rules) into a project.
# Run this from the unzipped bundle directory, or pass the bundle path as CURSOR_SKILLS_BUNDLE.
#
# Usage:
#   ./install.sh [TARGET_PROJECT_DIR]
#   TARGET_PROJECT_DIR defaults to the current directory (.)
#
set -euo pipefail

BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-.}"
TARGET="$(cd "$TARGET" && pwd)"

for skill in api-automation-smart-ci test-buckets-rules; do
  if [[ ! -d "$BUNDLE_DIR/$skill" ]]; then
    echo "error: missing skill folder: $BUNDLE_DIR/$skill" >&2
    exit 1
  fi
done

mkdir -p "$TARGET/.cursor/skills"
for skill in api-automation-smart-ci test-buckets-rules; do
  rm -rf "$TARGET/.cursor/skills/$skill"
  cp -R "$BUNDLE_DIR/$skill" "$TARGET/.cursor/skills/"
done

# Optional: ship TEST_BUCKETS.reference.md as TEST_BUCKETS.md when not present
if [[ -f "$BUNDLE_DIR/TEST_BUCKETS.reference.md" ]] && [[ ! -f "$TARGET/TEST_BUCKETS.md" ]]; then
  cp "$BUNDLE_DIR/TEST_BUCKETS.reference.md" "$TARGET/TEST_BUCKETS.md"
  echo "Also added TEST_BUCKETS.md at project root (was missing)."
fi

echo "Installed Cursor skills under: $TARGET/.cursor/skills/"
echo "  - api-automation-smart-ci"
echo "  - test-buckets-rules"
