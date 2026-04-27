# Member transfer API — automation

Standalone **Gradle** module (same pattern as [order-session-api-automation](../order-session-api-automation) in the monorepo: own `build.gradle`, `testng.xml`, `gradlew`, layered env config, Allure).

**Stack:** Java **17**, TestNG, Rest Assured, Allure, Gson.

## Viewing the Allure report (no endless “Loading…”)

| Symptom | Cause | Fix |
|--------|--------|-----|
| `file:///.../index.html` shows **Loading...** | Safari/Chrome block `fetch` to `data/*.json` on **file://** | Our `generateAllureReport` **embeds** JSON into `index.html` — use output under **`reports/current/`** from this project, not a raw `allure generate` folder only. |
| You opened `allure-report/index.html` | That folder is often an unpatched Allure build | Run `./gradlew generateAllureReport` and open **`reports/current/index.html`**, or use HTTP (below). |

**Option A — double‑click / file:// (after Gradle report):**  
`./gradlew test` then `./gradlew generateAllureReport` → open `reports/current/index.html` (or run `./gradlew openAllureReport` on macOS).

**Option B — HTTP (always works for any Allure output):**  
`./scripts/serve-allure-report.sh` → open `http://127.0.0.1:8765/index.html` (uses `allure-report/` if `reports/current` is missing; or set `ALLURE_REPORT_DIR`).

## Layout

| Item | Role |
|------|------|
| `src/main/java/.../config/ApiClient.java` | HTTP + 5xx retry (3 attempts) |
| `src/main/java/.../config/ConfigManager.java` | `env` + optional `.local` + `external.api.env.path` merge |
| `src/main/java/.../store/ResponseStore.java` | Cached token, bulkId |
| `src/main/java/.../auth/MemberTransferAuth.java` | Optional suite-wide token prefetch |
| `src/main/java/.../support/MemberTransferSupport.java` | URLs, headers, request bodies |
| `src/test/java/.../tests/*` | Auth, inquiry, status, E2E |
| `src/test/java/.../tests/MemberTransferKafkaPublishPositiveTests.java` | Verifies inquiry event is published to Kafka topic `bulk-account-transfer` |
| `src/test/java/.../tests/MemberTransferKafkaPublishNegativeTests.java` | Verifies inquiry event is not published to wrong/legacy topics |
| `src/test/resources/env/*.properties` | Per-env defaults |
| `config/member-transfer-env.local.properties.example` | Optional external merge (copy to gitignored `member-transfer-env.local.properties`) |
| `scripts/serve-allure-report.sh` | Serve `reports/current` over HTTP (Safari-safe) |
| `TEST_BUCKETS.md` | Group tags and Gradle examples |

## Run (in this directory)

```bash
chmod +x gradlew
./gradlew test -Denv=qa
./gradlew test -Denv=qa -Dgroups=MT_Smoke
./gradlew test -Denv=qa -Dgroups=Kafka
./gradlew printEnv
./gradlew generateAllureReport
./scripts/serve-allure-report.sh
```

Forward secrets without committing them: copy `src/test/resources/env/qa.local.properties.example` → `qa.local.properties` and set `member.transfer.auth.credentials=USER:pass`.

**Cursor skills (monorepo only):** when this repo is nested under `api-automation-smart-ci-poc`, the parent’s `.cursor/skills/` and `TEST_BUCKETS.md` still describe shared CI. For a **standalone** clone, copy that repo’s `TEST_BUCKETS` convention or this module’s `TEST_BUCKETS.md`.

## API flow under test

1. `POST` `{auth.url}` — `client_credentials` with Basic `member.transfer.auth.credentials`
2. `POST` `{base.url}/api/member-transfer/billing-account-batch-transfers/inquiry` — `Authorization: Bearer` + `ABCFS-TENANT-ID`
3. `GET` `{base.url}/api/member-transfer/billing-account-batch-transfers/status/{bulkId}?...`

## Pushing to your member-transfer app repo

Use **this folder** as the repo root (or subtree): keep `build.gradle`, `settings.gradle`, `gradlew*`, `gradle/`, and `testng.xml` with sources as committed; ensure `.gitignore` excludes `*.local.properties` and `build/`.
