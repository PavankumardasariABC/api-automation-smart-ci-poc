# Test buckets — Order Session API

Aligned with the root **Smart CI POC** model: TestNG **groups** + Gradle `-Dgroups=…`, optional `-DtestClass=…`.

| Bucket | What runs | Typical use |
|--------|-----------|-------------|
| **Smoke** | `ExternalApiAuthBootstrapTests` + `OrderSessionApiE2ETests` **401** paths (no bearer on POST/GET) | PR gate, fast demo |
| **Sanity** | Smoke + **JSON data-driven creates** + **GET by id** in `OrderSessionApiE2ETests` when `test.*` IDs are set | Pre-release |
| **Regression** | Full `OrderSessionApiE2ETests` (all OpenAPI operations, negatives, **OrderSession** group) + narrative test | Post-merge |

Group **`OrderSession`** is used on the consolidated E2E class (same idea as **`Billing`** on the root POC template).

## Auth bootstrap

`ExternalApiAuthBootstrapTests` is **first** in `testng.xml`. It documents in Allure how the bearer is obtained (OAuth merge file vs `ORDER_SESSION_JWT`).

## Smart CI (monorepo)

Workflow: `.github/workflows/order-session-api-tests.yml`

- **pull_request** → **Smoke** (unless workflow_dispatch overrides)
- **push** to `main` / `develop` → **Regression**
- **workflow_dispatch** → user picks **dev / qa / stage / beta** and bucket, optional **test class**
- **path filters** — workflow runs when `order-session-api-automation/**` changes
- **Change hint** step lists touched test files (same spirit as root `api-tests.yml` smart step)

## Commands

```bash
./gradlew test -Denv=qa -Dgroups=Smoke
./gradlew test -Denv=stage -Dgroups=Regression
./gradlew test -DtestClass=com.ordersession.tests.PaymentTokenSessionIntegrationTests
```

New test classes **must** be added to `testng.xml`.
