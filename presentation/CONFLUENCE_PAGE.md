# API Automation Smart CI — POC

> **How to use this in Confluence:**  
> 1. Create a new Confluence page and choose **Insert → Markdown** (Confluence Cloud), or paste this content and Confluence will convert it.  
> 2. Or copy sections as needed into existing pages.  
> 3. Replace `[Your Name / Team]` and repository links with your actual values.

**Document owner:** Pavankumar Dasari | Commerce India Team
**Status:** POC Complete | Ready for Review  
**Last updated:** March 2026

---

## 1. Executive Summary

This POC delivers an **end-to-end API test automation and CI pipeline** that:

- **Runs the right tests at the right time** — Fast Smoke on every Pull Request (pre-merge gate), full Regression after merge to main/develop, with no manual intervention.
- **Improves release confidence** — Merge is blocked until Smoke passes; post-merge Regression gives full coverage visibility.
- **Scales with the product** — Test buckets (Smoke, Sanity, Regression) and TestNG groups keep feedback fast while covering Auth, Common, Charge, and Billing APIs.
- **Provides clear visibility** — Allure reports published to GitHub Pages, PR comments with report links, and offline reports for audits.

**Outcome:** A single pipeline that gates merges automatically, runs full regression on main/develop, and supports manual runs (Smoke, Sanity, Regression, or All) with environment and optional test-class selection.

---

## 2. Business Value & Why This POC

| Benefit | Description |
|--------|-------------|
| **Faster feedback** | Smoke runs in minutes on every PR; no need to run the full suite before merge. |
| **No manual gates** | PR → Smoke and Push → Regression are automatic; no one has to remember to trigger the right suite. |
| **Clear failure ownership** | Allure reports and failure categorization (auth, data, microservice, environment) help triage quickly. |
| **Reusable patterns** | Template-based tests, shared token refresh, env-based config, and data providers reduce duplication and maintenance. |
| **Audit trail** | Reports archived per run (GitHub Pages + artifacts), with offline Allure for compliance or offline review. |

---

## 3. Architecture & Technology Stack

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DEVELOPER / PR / MERGE                                │
└─────────────────────────────────────────────────────────────────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
   Pull Request              Push to main/develop    Manual (Actions)
         │                        │                        │
         ▼                        ▼                        ▼
   ┌──────────┐            ┌──────────────┐        ┌─────────────────┐
   │  SMOKE   │            │  REGRESSION  │        │ Smoke / Sanity / │
   │ (pre-merge)           │ (post-merge)  │        │ Regression / All │
   └────┬─────┘            └──────┬───────┘        └────────┬────────┘
        │                          │                         │
        └──────────────────────────┼─────────────────────────┘
                                   ▼
                    ┌──────────────────────────────┐
                    │   GitHub Actions (self-hosted)│
                    │   • JDK 17 + Gradle           │
                    │   • TestNG + Rest Assured     │
                    │   • Allure CLI                │
                    └──────────────┬───────────────┘
                                   ▼
                    ┌──────────────────────────────┐
                    │   Test execution              │
                    │   • testng.xml + dynamic suite│
                    │   • -Dgroups=Smoke|Sanity|... │
                    └──────────────┬───────────────┘
                                   ▼
                    ┌──────────────────────────────┐
                    │   Allure Report               │
                    │   • Artifact (15 days)        │
                    │   • GitHub Pages (latest +    │
                    │     per-run)                  │
                    │   • PR comment with links     │
                    └──────────────────────────────┘
```

### 3.2 Technology Stack

| Layer | Technology | Version / Notes |
|-------|------------|------------------|
| **Language** | Java | 17 |
| **Build** | Gradle | Wrapper (project-defined) |
| **Test framework** | TestNG | 7.10.2 |
| **API client** | Rest Assured | 5.4.0 |
| **Reporting** | Allure | 2.27.0 (CLI + adapter) |
| **CI** | GitHub Actions | Self-hosted runner |
| **Config** | Properties files | env-specific (dev, qa, stage, prod) |

### 3.3 Repository Structure (Relevant to POC)

| Path | Purpose |
|------|---------|
| `src/main/java/com/externalAPIs/` | Config (ApiClient, ConfigManager), reporting (AllureTestListener), store (ResponseStore), utils (DataProviderUtils) |
| `src/test/java/com/externalAPIs/tests/` | Test classes: auth, common, Charge, billing; BaseTestTemplate |
| `src/test/resources/env/` | Environment properties (dev.properties, qa.properties, etc.) |
| `src/test/resources/input/` | JSON data for data-driven tests |
| `testng.xml` | Master suite: all test classes and Allure listener |
| `build.gradle` | Test task, dynamic suite generation from `-Dgroups` / `-DtestClass`, Allure report generation |
| `.github/workflows/api-tests.yml` | CI: PR, push, workflow_dispatch; test selection; Allure publish and PR comment |

---

## 4. Test Strategy — Test Buckets (Smoke, Sanity, Regression)

Tests are grouped into **three buckets** so CI runs the right set before and after merge without manual intervention.

### 4.1 Bucket Definitions

| Bucket | Purpose | When it runs in CI | Contents |
|--------|---------|--------------------|----------|
| **Smoke** | Fast health check | **Before merge** (on every PR) | Auth (all token flows) + Get Merchant Details + Get Transaction Details (priority 1) |
| **Sanity** | Critical path | Manual / workflow_dispatch only | Smoke + Create Client, Create Merchant, Create Organization, Create Payment Token Session |
| **Regression** | Full suite | **After merge** (push to main/develop) | All tests across Auth, Common, Charge, Billing |

### 4.2 Why This Split?

- **Smoke** — Minimal set to validate “can we auth and read critical entities?” so PRs get a quick pass/fail.
- **Sanity** — Critical create/payment flows for manual verification or pre-release checks.
- **Regression** — Full coverage after merge to catch regressions across all modules.

### 4.3 How Groups Are Used

- Every test is tagged with TestNG `groups` (e.g. `groups = {"Regression", "Smoke"}`).
- Gradle runs tests with `-Dgroups=Smoke`, `-Dgroups=Sanity`, or `-Dgroups=Regression`; the build generates a dynamic suite from `testng.xml` and the selected groups.
- **Rule:** Every test must have at least **Regression**. Smoke and Sanity are added only for tests that belong in those buckets (see TEST_BUCKETS.md in repo).

---

## 5. CI/CD Pipeline — End-to-End Behavior

### 5.1 Triggers and Test Selection (No Manual Intervention)

| Event | Branch | What runs | Purpose |
|-------|--------|------------|---------|
| **Pull request opened/updated** | main, develop | **Smoke** | Pre-merge gate; merge blocked until Smoke passes |
| **Push** (e.g. after merge) | main, develop | **Regression** | Full suite post-merge |
| **Manual run** | User selects branch | User selects: **Smoke**, **Sanity**, **Regression**, or **All** | Ad-hoc validation; optional environment and test class |

### 5.2 Pipeline Steps (High Level)

1. **Checkout** — Repository (and branch for manual runs).
2. **Setup** — JDK + Gradle (with cache).
3. **Install Allure CLI** — Cached to speed up runs.
4. **Smart test selection** — (Optional) Detects changed test files; for PR/push the bucket is chosen by event (Smoke vs Regression).
5. **Run tests** — `./gradlew test` with `-Dgroups=...` or full suite; parallel workers for speed.
6. **Generate Allure report** — Always (if results exist), including offline Safari-safe HTML.
7. **Publish** — Copy to `reports/site`, upload artifact (15-day retention), publish to GitHub Pages (latest + per-run).
8. **PR comment** — For pull requests, post a comment with report links and suite name.
9. **Fail build** — If any test failed, mark the workflow as failed after report generation.

### 5.3 Environments and Secrets

- **Environments:** dev, qa, stage, prod (selected in manual run; default for PR/push is **qa**).
- **Secrets:** `AUTH_USERNAME`, `AUTH_PASSWORD` (and any others) used for auth in tests; configured in GitHub repository/environment secrets.

### 5.4 Concurrency

- One run per ref at a time (`api-tests-${{ github.ref }}`); new runs cancel in-progress runs for the same branch.

---

## 6. Reporting & Visibility

### 6.1 Allure Reports

- **Generated** after every test run (when allure-results exist).
- **Offline report** — Single HTML file with embedded data (UTF-8 and Safari safe) for local or offline review.
- **CI artifact** — Full Allure report uploaded (retention 15 days); can be downloaded and opened locally.
- **GitHub Pages** — Published to `gh-pages` branch:
  - **Latest:** `reports/latest/` (overwritten each run).
  - **Per run:** `reports/runs/<run_number>/` for history.

### 6.2 PR Comment

On every PR run, the workflow posts a comment with:

- Branch and suite (e.g. Smoke for PR).
- Links to Latest Report and This Run on GitHub Pages.
- Link to the workflow run (to download artifact).
- Short note on how to view the report locally (unzip, run script, open in browser).

### 6.3 Failure Categorization (for triage)

Failures can be interpreted by type:

| Type | Typical cause | Action |
|------|----------------|--------|
| **Auth (401)** | Token expired / invalid | Check token refresh (AuthUtils) and credentials |
| **Data (4xx validation)** | Request payload or IDs | Check input data and ResponseStore (shared IDs) |
| **Microservice (5xx)** | Backend/API down or error | Check service health and logs |
| **Environment** | Wrong URL, missing config | Check env (dev/qa/stage/prod) and config properties |

---

## 7. How to Run Tests

### 7.1 Local (Developer Machine)

**By bucket:**

```bash
# Smoke only
./gradlew test -Dgroups=Smoke

# Sanity
./gradlew test -Dgroups=Sanity

# Regression (full suite by group)
./gradlew test -Dgroups=Regression

# All tests (no group filter)
./gradlew test
```

**By environment:**

```bash
./gradlew test -Dgroups=Smoke -Denv=qa
```

**Single test class:**

```bash
./gradlew test --tests "com.externalAPIs.tests.auth.Authorization_TokenTests"
```

**Generate and open Allure report (e.g. macOS):**

- Report is generated into `reports/current/` (and `Allure_Offline_Single_Safe.html`).
- Optional: use project script (e.g. `serve-allure-report.sh`) and open in browser.

### 7.2 In CI (GitHub Actions)

- **PR:** Push to a branch and open/update a PR to main or develop → Smoke runs automatically.
- **Push:** Merge to main/develop → Regression runs automatically.
- **Manual:** Actions → “API Automation CI” → Run workflow → choose branch, environment, bucket (Smoke/Sanity/Regression/All), and optionally a test class.

---

## 8. Test Coverage & Structure

### 8.1 Test Packages and Classes

| Package | Description | Example classes |
|---------|-------------|------------------|
| **auth** | All token flows | Authorization_TokenTests, Secure_TokenTests, Glofox_ClientTokenTests, Glofox_SecureClientTokenTests |
| **common** | Core entities and setup | CreateClientTests, CreateMerchantTests, CreateOrganizationTests, CreateLocationTests, CreateCompanyTests, CreateClientProfileTests, CreateProcessorTests, CreateTerminalTests, CreatePaymentSessionConsumerTests, CreateBankPaymentTokenTests, CreateClientBankAccountTests, UpdateMerchantTests |
| **Charge** | Payment and merchant/transaction reads | GetMerchantDetailsTests, GetTransactionDetailsTests, CreatePaymentTokenSessionTests, CreateAchPaymentChargeTests, CreateRefundPaymentTests |
| **billing** | Billing flows | CreateBillingAccountTransferTests (with BillingDataProvider) |

All classes are registered in `testng.xml` so they are included when running by group or full suite.

### 8.2 Group Assignment (Summary)

- **Smoke:** Auth tests (all four token classes) + GetMerchantDetailsTests (priority 1) + GetTransactionDetailsTests (priority 1).
- **Sanity:** Smoke + CreateClientTests, CreateMerchantTests, CreateOrganizationTests, CreatePaymentTokenSessionTests.
- **Regression:** All tests have `Regression`; many also have `Smoke` or `Sanity` as above.
- **Billing:** Dedicated `Billing` group for billing-only runs (e.g. `-Dgroups=Billing`).

### 8.3 Conventions

- **BaseTestTemplate** — Suite-level setup/teardown and env logging.
- **Allure** — Steps, Epic/Feature/Story, Severity, Description for traceability.
- **AuthUtils** — Centralized token retrieval/refresh for secure endpoints.
- **ConfigManager** — Base URLs and config from env-specific properties.
- **ResponseStore** — Shared IDs (e.g. client, merchant) across tests.
- **Data providers** — DataProviderUtils and *DataProvider classes with JSON input under `src/test/resources/input/`.

---

## 9. Adding New Tests — Governance

To keep buckets and CI behavior consistent:

1. **Annotate** the test with the correct TestNG groups:
   - **Regression** — Required for every test.
   - **Smoke** — Only for auth and 1–2 critical read/health checks.
   - **Sanity** — Smoke + critical create/payment flows (as per TEST_BUCKETS.md).

2. **Register** the new test class in `testng.xml` under the appropriate section (Auth, Common, Charge, Billing). Without this, the test will not run for any bucket.

3. **Example:**

   ```java
   @Test(groups = {"Regression", "Sanity"})
   public void createSomething() { ... }
   ```

4. **Documentation** — Keep TEST_BUCKETS.md in the repo in sync with bucket contents and rules (see repo file TEST_BUCKETS.md).

---

## 10. Security & Configuration

### 10.1 Secrets

- Auth and other sensitive values are stored in **GitHub Secrets** (and/or environment secrets), not in code or config committed to the repo.
- CI injects them via `env` (e.g. `AUTH_USERNAME`, `AUTH_PASSWORD`).

### 10.2 Environment Configuration

- **Per-env files:** `src/test/resources/env/dev.properties`, `qa.properties`, etc.
- **Selection:** `-Denv=dev|qa|stage|prod` (default in CI for PR/push: qa).
- Base URLs and service endpoints are read from these properties via ConfigManager.

### 10.3 Self-Hosted Runner

- The workflow runs on a **self-hosted** GitHub Actions runner; ensure runner environment and network access align with security policies for hitting dev/qa/stage/prod APIs.

---

## 11. Future Roadmap (Optional)

- **Gates** — Enforce “Smoke must pass” as a required status check for merge (branch protection).
- **Failure dashboards** — Aggregate failure reasons (auth vs data vs 5xx) for trending.
- **Smart selection on PR** — Run only tests affected by changed files (foundation already in workflow; can be extended).
- **Auto-rerun** — Optional automatic rerun of flaky tests before failing the build.
- **More environments** — Add or refine stage/prod runs with appropriate safeguards.

---

## 12. Appendix

### 12.1 Key Repo Files

| File | Purpose |
|------|---------|
| `TEST_BUCKETS.md` | Bucket definitions, CI behavior, and rules for adding tests |
| `testng.xml` | Master list of test classes and Allure listener |
| `build.gradle` | Test task, dynamic suite, Allure, generateAllureReport |
| `.github/workflows/api-tests.yml` | Full CI: triggers, test selection, Allure publish, PR comment |

### 12.2 Glossary

| Term | Meaning |
|------|---------|
| **Smoke** | Minimal test set for quick health check (auth + critical reads) |
| **Sanity** | Smoke + critical create/payment flows |
| **Regression** | Full test suite across all modules |
| **Dynamic suite** | Gradle generates a TestNG suite XML from testng.xml + `-Dgroups` / `-DtestClass` |
| **Allure** | Test reporting framework; generates HTML reports and supports history/trends |

### 12.3 References

- **Repo:** [Link to your GitHub repository]
- **Latest Allure report (GitHub Pages):** `https://<org>.github.io/<repo>/reports/latest/`
- **Presentation / demo:** See `presentation/` folder (e.g. index.html, README) for leadership walkthrough and demo steps.

---

*This Confluence page reflects the API Automation Smart CI POC as of March 2026. For exact group assignments and latest test list, refer to the repository and TEST_BUCKETS.md.*
