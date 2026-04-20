#!/usr/bin/env bash
# Run Billing API automation (separate module). From repo root.
# Prerequisites:
#   - cd billing-api-automation && cp src/test/resources/env/qa.local.properties.example src/test/resources/env/qa.local.properties
#   - Set billing.organization.id and billing.oauth.password (or export BILLING_OAUTH_PASSWORD).
#   - Optional: test.billing.account.id, billing.location.id for nested / transaction tests.
set -euo pipefail
ENV="${1:-qa}"
GROUP="${2:-Smoke}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT/billing-api-automation"
chmod +x ./gradlew
echo "▶ Billing API automation — env=$ENV group=$GROUP"
./gradlew test --no-daemon \
  -Denv="$ENV" \
  -Dgroups="$GROUP"
