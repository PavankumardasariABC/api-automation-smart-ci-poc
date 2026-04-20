# Order Session API — E2E automation

Java **17**, **Gradle**, **TestNG**, **Rest Assured**, **Allure** — hybrid framework (service layer, data providers, shared config) aligned with **`openapi-spec-5.yaml`**.

## E2E tests (billing template)

- Main class: **`OrderSessionApiE2ETests`** — same style as root POC `CreateBillingAccountTransferTests` (path constants, `headersWithBearer`, JSON `DataProvider`, **401** retry, Allure attachments).
- Scenarios: `src/test/resources/input/order-session/create_payment_token_session_data.json`, `create_wallet_entry_session_data.json`.

## Sharing with your team (config only)

- **Full guide:** [`docs/TEAM_CONFIGURATION.md`](docs/TEAM_CONFIGURATION.md) — QA / Dev / Stage / Beta URLs, org ID, OAuth merge, local overrides.
- **Per-env secrets (gitignored):** copy [`src/test/resources/env/qa.local.properties.example`](src/test/resources/env/qa.local.properties.example) → `qa.local.properties` and set `abcfs.organization.id` (and optional `test.*` IDs).
- **OAuth template (gitignored target):** copy [`config/oauth-env.local.properties.example`](config/oauth-env.local.properties.example) → `config/oauth-env.local.properties`.

## Cursor Agent Skills (monorepo)

If you work inside the **full** `api-automation-smart-ci-poc` repository, Cursor loads shared skills from **`../.cursor/skills/`** (TestNG buckets, `testng.xml`, CI alignment with **`../TEST_BUCKETS.md`**).

To give **another repo** the same Cursor conventions, build the shareable bundle from the **monorepo root** and send the zip:

```bash
cd ..
./scripts/cursor-skills-plugin/build-bundle.sh
```

Share **`build/cursor-api-automation-skills-bundle.zip`**; recipients follow **`README.txt`** inside the bundle. Details: **[`../scripts/cursor-skills-plugin/BUNDLE_README.txt`](../scripts/cursor-skills-plugin/BUNDLE_README.txt)**.

If you use **only** this folder as a standalone Git repo, copy the skill folders from the main repo’s **`.cursor/skills/`** into your project, or unzip the bundle above and run **`install.sh`** against your project root.

## Demo-first layout

| Artifact | Purpose |
|----------|---------|
| `docs/DEMO_RUNBOOK.md` | Step-by-step narration for a test-lead demo |
| `docs/OPENAPI_TEST_MATRIX.md` | Each `operationId` → TestNG class/method |
| `../run-order-session-demo.sh` | **Monorepo root** helper (optional): runs Gradle in this module; OAuth still from `config/oauth-env.local.properties` or `ORDER_SESSION_JWT` |
| Root workflow `.github/workflows/order-session-api-tests.yml` | **Smart CI** (PR→Smoke, push→Regression, manual bucket) |

## Automatic bearer token (OAuth file in this folder)

No manual JWT paste is required when **`config/oauth-env.local.properties`** exists (copy from `config/oauth-env.local.properties.example`):

1. Default `external.api.env.path=config/oauth-env.local.properties` is set in each `src/test/resources/env/*.properties`.
2. Set `order.session.auth.source` to **`GLOFOX`** (default), **`CLIENT`**, or **`SECURE`** — uses merged `base.url` / `auth.url` / `secure.*` keys.
3. Keep `auth.bearer.token=__FETCH_FROM_EXTERNAL__` or omit; token is cached in `ResponseStore` as `OrderSessionAccessToken`.

**Override order:** `ORDER_SESSION_JWT` → `-Dauth.bearer.token` → non-placeholder `auth.bearer.token` → OAuth fetch.

Gradle forwards `-Denv`, `-Dexternal.api.env.path`, `-Dauth.bearer.token`, `-Dauth.creds` into the **test JVM** (see `build.gradle`).

## Retry behaviour

- **`ApiClient`**: up to **3** attempts on **5xx** responses (same idea as the root POC).
- **`ExternalApiTokenProvider`**: up to **3** attempts on token endpoint **5xx / 401 / 403** with short backoff.

## Operations covered

| operationId (OpenAPI) | Path |
|------------------------|------|
| `createPaymentTokenSession` | `POST /payment-session/tokens` |
| `getPaymentTokenSession` | `GET /payment-session/tokens/{id}` |
| `createWalletEntrySession` | `POST /payment-session/wallet-entries` |
| `getWalletEntrySession` | `GET /payment-session/wallet-entries/{id}` |

Headers: `Authorization`, `ABCFS-ORGANIZATION-ID`, `Content-Type: application/json` (POST).

## Monorepo run (recommended)

From repository **root**:

```bash
chmod +x run-order-session-demo.sh
./run-order-session-demo.sh qa Smoke
```

From this folder:

```bash
./gradlew test -Denv=qa -Dgroups=Sanity
```

Set **`abcfs.organization.id`** in this module’s `env/*.properties` before calling real Order Session APIs.

## Standalone Git repository (share only this folder)

**Yes — it can work for your team with only the `order-session-api-automation` folder**, as long as they treat it as the **Gradle project root** (same contents you have now: `gradlew`, `build.gradle`, `src/`, `config/`, `scripts/`, etc.). Nothing else from the monorepo is required **if** they fix config and auth as below.

| Built in | You need to provide |
|----------|---------------------|
| Tests, Gradle wrapper, Allure task, serve script | **JDK 17** on each machine / runner |
| Default `qa.properties` URLs | Real **`abcfs.organization.id`** or **`organization.id`** in `*.local.properties` (or edit env file) |
| OAuth | Create **`config/oauth-env.local.properties`** from the example file, **or** set `ORDER_SESSION_JWT` / `auth.bearer.token` |
| Smoke / 401 tests | Usually work once org header + any token path is valid |
| Create / GET integration | **`test.consumer.id`**, **`test.location.id`**, wallet IDs in config |

**Teammate quick start (standalone):**

1. Unzip / clone so `order-session-api-automation` is the repo root (or `cd` into it).
2. `cp src/test/resources/env/qa.local.properties.example src/test/resources/env/qa.local.properties` and set organization + optional `test.*`.
3. Fix OAuth: **either** (A) copy `config/oauth-env.local.properties.example` → `config/oauth-env.local.properties` and fill URLs/credentials, **or** (B) put a JWT in `auth.bearer.token` or env `ORDER_SESSION_JWT`.
4. `./gradlew test -Denv=qa -Dgroups=Smoke`

Use [`.github/workflows/order-session-api-tests.yml`](.github/workflows/order-session-api-tests.yml) in that repo (paths already assume project root). The monorepo workflow under the parent repo is separate.

## Test buckets

See **`TEST_BUCKETS.md`**. Quick commands:

```bash
./gradlew test -Denv=qa -Dgroups=Smoke
./gradlew test -Denv=dev -Dgroups=Regression
./gradlew test -Denv=qa -DtestClass=com.ordersession.tests.auth.ExternalApiAuthBootstrapTests
```

## Allure

After `./gradlew test`: `./gradlew generateAllureReport`.

**Safari / `file://` and “Loading…” forever:** Allure’s default `index.html` loads JSON from disk; browsers block that under `file://`. Use one of these:

1. Open **`reports/current/Allure_Offline_Single_Safe.html`** (double-click or File → Open). Do not open only the project folder in the address bar.
2. Or serve the folder over HTTP: `./scripts/serve-allure-report.sh` then open the printed URL (defaults to **8765**, or the next free port if 8765 is already in use). To force a port: `./scripts/serve-allure-report.sh 9292`.

See also **`reports/current/HOW_TO_OPEN.txt`** after each report generation.

## CI secrets

- **`ORDER_SESSION_JWT`** — optional; bypasses OAuth when runners cannot reach internal auth URLs.
