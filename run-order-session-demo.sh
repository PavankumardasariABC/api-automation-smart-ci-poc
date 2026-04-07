#!/usr/bin/env bash
# Run Order Session automation from monorepo root (OAuth: config/oauth-env.local.properties in the module, or ORDER_SESSION_JWT).
set -euo pipefail
ENV="${1:-qa}"
GROUP="${2:-Smoke}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT/order-session-api-automation"
chmod +x ./gradlew
OAUTH="config/oauth-env.local.properties"
if [[ ! -f "$OAUTH" ]]; then
  echo "ℹ️  No $OAUTH — create it from config/oauth-env.local.properties.example or set ORDER_SESSION_JWT."
fi
echo "▶ env=$ENV group=$GROUP (OAuth merge: $OAUTH if present)"
./gradlew test --no-daemon \
  -Denv="$ENV" \
  -Dgroups="$GROUP"
