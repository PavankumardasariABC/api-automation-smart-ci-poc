# Demo runbook — Order Session automation (test lead)

## What you are showing

1. **Separate module** `order-session-api-automation/` — can be copied out as its own Git repo (see main `README.md`).
2. **Automatic bearer token** from OAuth (`auth.url` / `secure.auth.url` / Glofox `base.url` + `/api/token`) via merged `config/oauth-env.local.properties`, or from **`ORDER_SESSION_JWT`**.
3. **Retries** on API client (transient `5xx`) and on token fetch (configurable attempts).
4. **Smart-style CI** (when using the monorepo workflow): PR → Smoke, push → `main`/`develop` → Regression, manual bucket + env + optional class.
5. **Traceability** — `docs/OPENAPI_TEST_MATRIX.md` maps each `operationId` to tests.

## Preconditions (local demo)

1. `cd order-session-api-automation` (or clone repo containing this folder).
2. Set **`abcfs.organization.id`** in `qa.local.properties` (or edit `qa.properties` for a throwaway demo).
3. Create **`config/oauth-env.local.properties`** from `config/oauth-env.local.properties.example` with valid **`base.url`**, **`auth.url`**, **`credentials`**, **`secure.*`** as needed for `order.session.auth.source`, **or** export **`ORDER_SESSION_JWT`**.
4. Optional: fill **`test.consumer.id`**, **`test.location.id`**, wallet IDs for full **Sanity** E2E creates.

## One-command demo (from monorepo root)

If the parent repo includes `run-order-session-demo.sh`:

```bash
chmod +x run-order-session-demo.sh
./run-order-session-demo.sh qa Smoke
```

Or manually:

```bash
cd order-session-api-automation
./gradlew test -Denv=qa -Dgroups=Smoke
```

## Narration (2 minutes)

- “We load **`config/oauth-env.local.properties`** so **no pasted JWT** is required when that file is filled; `order.session.auth.source=GLOFOX` uses merged `base.url` for the token call.”
- “**Smoke** proves **401** without bearer and **404** on unknown IDs when a token is available.”
- “**Sanity** runs **create + read** for PAYOR token session and wallet entry when test data is configured.”
- “**Regression** adds validation **`400`** paths and extra owner types.”
- “**Allure** report is generated under `reports/current/` after `./gradlew test`.”

## CI secrets (optional)

- **`ORDER_SESSION_JWT`**: bypasses OAuth fetch if your pipeline cannot reach internal auth hosts.
- Otherwise provide **`config/oauth-env.local.properties`** on the runner (e.g. written from secrets) or use a self-hosted runner that can reach your auth URLs.

## Switching auth source

In module env file:

- `order.session.auth.source=GLOFOX` — default; uses merged `base.url`.
- `order.session.auth.source=CLIENT` — uses merged `auth.url` + `credentials` (or `-Dauth.creds=`).
- `order.session.auth.source=SECURE` — uses merged `secure.auth.url` + `secure.username` / `secure.password`.
