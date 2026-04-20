#!/usr/bin/env bash
# Sync scripts/external-api-docs-content to https://github.com/abcfinancial2/external-api-docs
# Usage: ./scripts/publish-external-api-docs.sh [branch-name]
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/abcfinancial2/external-api-docs.git}"
BRANCH="${1:-feat/api-catalog-from-automation-poc}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTENT_DIR="${SCRIPT_DIR}/external-api-docs-content"
DEST_SUBDIR="docs/api-automation-smart-ci-poc"

if [[ ! -d "$CONTENT_DIR" ]]; then
  echo "Missing content dir: $CONTENT_DIR" >&2
  exit 1
fi

TMP="$(mktemp -d)"
cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT

echo "Cloning $REPO_URL ..."
git clone --depth 1 "$REPO_URL" "$TMP/repo"

cd "$TMP/repo"
DEFAULT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
echo "Default branch: $DEFAULT_BRANCH"

git fetch origin "$DEFAULT_BRANCH" 2>/dev/null || true
git checkout "$DEFAULT_BRANCH"
git pull origin "$DEFAULT_BRANCH" 2>/dev/null || true

git checkout -b "$BRANCH"

mkdir -p "$DEST_SUBDIR"
rsync -a --delete "$CONTENT_DIR/" "$DEST_SUBDIR/"

git add "$DEST_SUBDIR"
if git diff --staged --quiet; then
  echo "No changes to commit (remote already matches local content)."
  exit 0
fi

git commit -m "Add API catalog from api-automation-smart-ci-poc automation

- Root module endpoints (auth, common, charge, billing)
- Order Session payment-session paths and OpenAPI/test matrix reference"

git push -u origin "$BRANCH"

if command -v gh >/dev/null 2>&1; then
  gh pr create --base "$DEFAULT_BRANCH" --head "$BRANCH" \
    --title "Add API catalog from api-automation-smart-ci-poc" \
    --body "Automated export of API surface documentation derived from api-automation-smart-ci-poc (TestNG suites and Order Session module).

Paths are under \`$DEST_SUBDIR\`." || {
    echo "gh pr create failed (PR may already exist or gh needs auth). Branch pushed: $BRANCH" >&2
  }
else
  echo "Install GitHub CLI (gh) for automatic PR creation, or open a PR manually for branch: $BRANCH"
fi

echo "Done. Branch: $BRANCH"
