# API Automation Smart CI — POC Documentation

> **Using this in Confluence:** Create a new page and paste this content. In Confluence Cloud use **Insert → Markdown** (or paste; headings and tables will be preserved). For the E2E diagram in Section 3, keep the code block or redraw using Confluence’s diagram macro.

**Document Owner:** Pavankumar Dasari || Commerce India Team
**Last Updated:** March 2026  
**Status:** POC Complete — Ready for Review

---

## 1. Executive Summary

This POC delivers an **end-to-end API test automation framework** with **smart CI/CD integration** for External APIs (Auth, Common, Charge, Billing). It enables:

- **Automated quality gates** — Pull requests run a fast **Smoke** suite before merge; pushes to main/develop run full **Regression** after merge, with no manual intervention.
- **Structured test buckets** — Smoke (health check), Sanity (critical path), Regression (full suite) so the right tests run at the right time.
- **Professional reporting** — Allure reports with request/response attachments, published to GitHub Pages and linked in PR comments.
- **Environment-aware execution** — Dev, QA, Stage, Prod configs; bearer token auto-refresh and retry for transient failures.

The framework is built with **Java 17, Gradle, TestNG, Rest Assured, and Allure**, and runs on **GitHub Actions** (self-hosted runner) with optional manual runs for any bucket and environment.

---

## 2. Business Problem & Goals

| Problem | Goal |
|--------|------|
| Manual test execution and long feedback cycles | Automated runs on every PR and push |
| No clear gate before merging code | Smoke tests block merge until critical paths pass |
| Lack of visibility into API test results | Allure reports on GitHub Pages + PR comment with links |
| Inconsistent environments and token handling | Env-based config (dev/qa/stage/prod) and centralized token refresh |
| Ad-hoc test selection | Defined buckets (Smoke, Sanity, Regression) with clear CI behavior |

---

## 3. Solution Overview (E2E Flow)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        API AUTOMATION SMART CI — E2E FLOW                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  DEVELOPER                    GITHUB ACTIONS                    REPORTING    │
│  ─────────                    ─────────────                    ─────────    │
│                                                                              │
│  • Push branch    ──────►     Trigger: pull_request (to main/develop)        │
│  • Open PR        ──────►     → Run SMOKE tests (Auth + Get Merchant/        │
│                               → Get Transaction Details)                     │
│                               → Merge blocked until Smoke passes             │
│                                        │                                      │
│                                        ▼                                      │
│                               Generate Allure Report                         │
│                               Upload artifact + Publish to GitHub Pages      │
│                               Comment on PR with report links    ──────►     │
│                                                                   Allure     │
│  • Merge PR       ──────►     Trigger: push (to main/develop)     Report     │
│                               → Run REGRESSION (full suite)                  │
│                               → Same report pipeline                         │
│                                                                              │
│  • Manual run     ──────►     workflow_dispatch: choose bucket                │
│  (Actions → Run)              (Smoke / Sanity / Regression / All),           │
│                                environment (dev/qa/stage/prod),              │
│                                optional single test class                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Architecture & Technology Stack

### 4.1 Technology Stack

| Layer | Technology | Version / Notes |
|-------|------------|-----------------|
| Language | Java | 17 |
| Build | Gradle | Wrapper (8.x) |
| Test framework | TestNG | 7.10.2 |
| API client | Rest Assured | 5.4.0 |
| Reporting | Allure | 2.27.0 |
| CI/CD | GitHub Actions | Self-hosted runner |
| Repo | GitHub | main, develop branches |

### 4.2 Project Structure (High Level)

```
api-automation-smart-ci-poc/
├── .github/workflows/
│   └── api-tests.yml          # Single workflow: PR → Smoke, Push → Regression, Manual → choice
├── src/
│   ├── main/java/com/externalAPIs/
│   │   ├── config/             # ConfigManager (env props), ApiClient (HTTP + retry + Allure)
│   │   ├── store/              # ResponseStore (tokens, IDs, request/response persistence)
│   │   ├── reporting/          # AllureTestListener (suite lifecycle, attachments)
│   │   └── utils/              # DataProviderUtils, etc.
│   └── test/java/com/externalAPIs/tests/
│       ├── BaseTestTemplate    # Suite-level setup/teardown
│       ├── auth/               # Token tests + AuthUtils (cached token refresh)
│       ├── common/             # Create Client, Merchant, Organization, Location, etc.
│       ├── Charge/             # Get Merchant/Transaction, Payment Token Session, ACH, Refund
│       └── billing/           # Create Billing Account Transfer (data-driven)
├── src/test/resources/
│   ├── env/                    # dev.properties, qa.properties, (stage, prod)
│   └── input/                  # JSON test data (e.g. billing)
├── testng.xml                  # Master list of all test classes (Auth, Common, Charge, Billing)
├── build.gradle                # Test task, -Dgroups/-DtestClass → dynamic suite, Allure
├── TEST_BUCKETS.md             # Bucket definitions and CI behavior (source of truth)
└── reports/                   # Allure output (current, history), analytics
```

### 4.3 Key Components

| Component | Purpose |
|-----------|---------|
| **ConfigManager** | Loads `env/{dev\|qa\|stage\|prod}.properties`; provides base URLs, auth URLs, credentials. |
| **ApiClient** | Single entry for GET/POST/PUT/DELETE; retry on 5xx; Allure request/response attachments; ResponseStore logging. |
| **ResponseStore** | Thread-safe in-memory + file persistence for tokens, IDs, last request/response; used by AuthUtils and listeners. |
| **AuthUtils** | Fetches and caches Client, Secure, Glofox Client, Glofox Secure tokens; used by all API tests that need bearer auth. |
| **AllureTestListener** | Suite start/end; clears ResponseStore at start; integrates with Allure lifecycle. |
| **testng.xml** | Defines suite, listener, and all test classes; Gradle builds a dynamic suite from it when `-Dgroups` or `-DtestClass` is set. |

---

## 5. Test Buckets (Smoke, Sanity, Regression)

Tests are tagged with TestNG **groups**. CI runs the appropriate bucket automatically or via manual selection.

### 5.1 Bucket Definitions

| Bucket | Purpose | When it runs in CI | Contents |
|--------|---------|--------------------|----------|
| **Smoke** | Fast health check | **Before merge** (on PR to main/develop) | Auth (all token flows) + Get Merchant Details + Get Transaction Details (priority 1) |
| **Sanity** | Critical path | **Manual only** (workflow_dispatch) | Smoke + Create Client, Create Merchant, Create Organization, Create Payment Token Session |
| **Regression** | Full suite | **After merge** (push to main/develop) | All tests across Auth, Common, Charge, Billing |

### 5.2 CI Behavior (No Manual Intervention for PR/Push)

- **Pull request** to `main` or `develop`  
  → Workflow runs **Smoke** automatically. **Merge is blocked until Smoke passes.**

- **Push** to `main` or `develop` (e.g. after merge)  
  → Workflow runs **Regression** automatically for full coverage.

- **Manual run** (Actions → "API Automation CI" → Run workflow)  
  → User chooses: **Smoke**, **Sanity**, **Regression**, or **All**; plus **environment** (dev/qa/stage/prod) and optional **test class**.

### 5.3 How Groups Are Used

- Every `@Test` must include **Regression** if it should be in the full suite; add **Smoke** or **Sanity** only when the test belongs in those buckets.
- Gradle runs with `-Dgroups=Smoke` or `-Dgroups=Regression` etc.; `build.gradle` builds a dynamic TestNG suite from `testng.xml` and applies the group filter.
- **Local commands:**
  - Smoke: `./gradlew test -Dgroups=Smoke`
  - Sanity: `./gradlew test -Dgroups=Sanity`
  - Regression: `./gradlew test -Dgroups=Regression`
  - All: `./gradlew test`

### 5.4 Test Coverage by Package

| Package | Scope | Examples |
|---------|--------|----------|
| **auth** | All token flows | Authorization_TokenTests, Secure_TokenTests, Glofox_ClientTokenTests, Glofox_SecureClientTokenTests |
| **common** | Core entities | CreateClient, CreateMerchant, CreateOrganization, CreateLocation, CreateCompany, UpdateMerchant, CreateClientProfile, CreateProcessor, CreateTerminal, CreatePaymentSessionConsumer, CreateBankPaymentToken, CreateClientBankAccount |
| **Charge** | Payments & reads | GetMerchantDetails, GetTransactionDetails, CreatePaymentTokenSession, CreateAchPaymentCharge, CreateRefundPayment |
| **billing** | Billing flows | CreateBillingAccountTransferTests (data-driven) |

---

## 6. CI/CD Pipeline (GitHub Actions) — Detail

### 6.1 Workflow File

- **File:** `.github/workflows/api-tests.yml`
- **Name:** "API Automation CI"

### 6.2 Triggers

| Trigger | Condition | Default behavior |
|---------|-----------|------------------|
| `pull_request` | Branches: main, develop | Run **Smoke** (no inputs) |
| `push` | Branches: main, develop | Run **Regression** |
| `workflow_dispatch` | Manual | User selects branch, **environment** (dev/qa/stage/prod), **test_group** (Smoke/Sanity/Regression/All), optional **test_class** |

### 6.3 Pipeline Steps (Summary)

| Step | Description |
|------|-------------|
| 1. Checkout | Checkout repo (branch from input or ref). |
| 2. Setup JDK + Gradle | Gradle Actions setup with wrapper, cache. |
| 3. Install Allure CLI | Cached Allure 2.27.0 for report generation. |
| 4. Smart Test Selection | (Optional) Detects changed test files; can drive custom group (currently PR/push logic overrides for Smoke/Regression). |
| 5. Run Tests | **PR → Smoke**, **Push → Regression**, **Manual → selected bucket**. Optional single class via `--tests`. Parallel workers: 4. |
| 6. Generate Allure Report | Always runs (if results exist); Gradle task `generateAllureReport`. |
| 7. Prepare Allure Site | Copy report to `reports/site` for publishing. |
| 8. Upload Artifact | Full Allure report as artifact (retention 15 days). |
| 9. Publish to GitHub Pages | Latest report → `reports/latest`; per-run → `reports/runs/<run_number>`. |
| 10. Comment on PR | On PR, post comment with branch, suite name, links to Latest Report and This Run, plus artifact download. |
| 11. Fail Build | If any test failed, fail the job (after report generation). |

### 6.4 Environment & Secrets

- **Environment:** Selected via workflow input (manual) or default `qa` for PR/push.
- **Secrets used:** `AUTH_USERNAME`, `AUTH_PASSWORD`, `GITHUB_TOKEN` (for Pages and PR comment).
- **Runner:** Self-hosted (configurable per org).

---

## 7. Reporting (Allure)

- **Results:** Collected in `build/allure-results` during test run.
- **Generation:** Gradle task `generateAllureReport` produces report in `reports/current/` and an offline-safe single HTML (`Allure_Offline_Single_Safe.html`) for Safari/local use.
- **CI:** Report is published to GitHub Pages and linked in the PR comment (latest + per-run).
- **Artifact:** Full report zip available in Actions run for 15 days.
- **Content:** Each request/response is attached in Allure (via ApiClient + Allure RestAssured filter); steps and severity/epic/feature/story available for filtering.

---

## 8. Configuration & Security

### 8.1 Environment Configuration

- **Location:** `src/test/resources/env/<env>.properties` (e.g. `dev.properties`, `qa.properties`).
- **Selection:** System property `env` (e.g. `-Denv=qa`); default in code is `dev`/`qa` as per project.
- **Typical keys:** `base.url`, `secure.base.url`, `auth.url`, `secure.auth.url`, `credentials`, `client.credentials`, and any service-specific URLs (e.g. transactions, merchant gateway).

### 8.2 Security Practices

- **Credentials:** Not hardcoded in repo; loaded from env properties or CI secrets.
- **Tokens:** Cached in ResponseStore; masked in Allure attachments (e.g. Authorization header shown as `****`).
- **HTTPS:** Relaxed SSL in Rest Assured for test environments only; configurable per need.

---

## 9. How to Run (Quick Reference)

| Goal | Command |
|------|---------|
| Smoke (local) | `./gradlew test -Dgroups=Smoke` |
| Sanity (local) | `./gradlew test -Dgroups=Sanity` |
| Regression (local) | `./gradlew test -Dgroups=Regression` |
| Full suite (local) | `./gradlew test` |
| Single class | `./gradlew test --tests "com.externalAPIs.tests.auth.Authorization_TokenTests"` |
| With env | `./gradlew test -Dgroups=Smoke -Denv=qa` |
| Generate report only | `./gradlew generateAllureReport` |

---

## 10. Failure Handling & Retry

- **ApiClient:** Retries on 5xx (up to 2 retries) to handle transient server errors.
- **Auth:** AuthUtils fetches and caches tokens; tests can reuse cached token; failure in token generation is reported in Allure.
- **Test run:** `configfailurepolicy="continue"` in TestNG so suite continues on config failures; at the end, the workflow fails the job if any test failed.

---

## 11. Future Enhancements (Roadmap)

- **Stricter gates:** Enforce Sanity on certain branches or before production deploy.
- **Failure buckets:** Categorize failures (e.g. microservice 5xx vs data 4xx vs auth vs environment) and dashboard.
- **Smart selection on PR:** Run only tests affected by changed files (build on existing “Smart Test Selection” step).
- **Auto-rerun:** Automatically re-run failed tests once (flaky handling).
- **More environments:** Expand stage/prod usage with appropriate secrets and configs.

---

## 12. Appendix

### A. Key Files Reference

| File | Purpose |
|------|---------|
| `testng.xml` | Master list of test classes; required for any test to run by group. |
| `build.gradle` | Test task, dynamic suite generation from `-Dgroups`/`-DtestClass`, Allure, `generateAllureReport`. |
| `TEST_BUCKETS.md` | Source of truth for bucket definitions, CI behavior, and how to add tests. |
| `.github/workflows/api-tests.yml` | CI: PR → Smoke, push → Regression, manual → choose bucket/env/class. |

### B. Adding a New Test to a Bucket

1. **Implement** the test class under the correct package (auth, common, Charge, billing).
2. **Annotate** with the right groups, e.g.  
   `@Test(groups = {"Regression", "Sanity"})` for critical path, or  
   `@Test(groups = {"Regression", "Smoke"})` for auth/health only.
3. **Register** the class in `testng.xml` under the appropriate `<classes>` block.
4. Use **AuthUtils** for tokens, **ConfigManager** for URLs, **ApiClient** for HTTP, **ResponseStore** for shared IDs; add **Allure** steps/attachments as needed.

### C. Glossary

| Term | Meaning |
|------|---------|
| Smoke | Minimal set of tests (auth + 1–2 critical reads) run on every PR. |
| Sanity | Smoke + critical create/payment flows; used for manual or scheduled runs. |
| Regression | Full suite; run after merge to main/develop. |
| workflow_dispatch | Manual trigger of the GitHub Actions workflow with user inputs. |

---

*This document is the single reference for the API Automation Smart CI POC and can be used as the Confluence page content. Copy the sections into Confluence (e.g. paste as Markdown or convert to Confluence wiki/Storage format as needed).*
