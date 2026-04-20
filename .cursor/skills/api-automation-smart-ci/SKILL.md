---
name: api-automation-smart-ci
description: Follows API automation conventions for the api-automation-smart-ci-poc repo—TestNG groups (Smoke, Sanity, Regression), testng.xml, Gradle -Dgroups, and GitHub Actions CI. Use when adding or changing API tests, test buckets, CI workflow, or when the user asks about this repo's test structure or running tests.
---

# API Automation Smart CI (Project Skill)

## Repo at a glance

- **Stack**: Java 17, Gradle, TestNG, Rest Assured, Allure.
- **Test layout**: `src/test/java/com/externalAPIs/tests/` — packages `auth`, `common`, `Charge`, `billing`. Base: `BaseTestTemplate`. Listener: `AllureTestListener`.
- **Execution**: TestNG suite is driven by `testng.xml`. Gradle builds a dynamic suite from it; `-Dgroups=Smoke|Sanity|Regression` filters by TestNG groups. All tests must be listed in `testng.xml` to run.

## Test buckets (required for every test)

| Bucket        | Use for                         | CI trigger        |
|---------------|----------------------------------|-------------------|
| **Smoke**     | Auth + 1–2 critical read/health  | PR (pre-merge)    |
| **Sanity**    | Smoke + critical create/payment  | Manual only       |
| **Regression**| Every test                      | Push to main/develop |

- Every `@Test` must include **Regression** (and optionally Smoke or Sanity).
- Smoke: only auth and critical health checks (e.g. GetMerchantDetails, GetTransactionDetails priority-1).
- Sanity: Smoke plus a few critical flows (e.g. CreateClient, CreateMerchant, CreateOrganization, CreatePaymentTokenSession).

## When adding or changing tests

1. **Annotate** with the right groups:
   ```java
   @Test(groups = {"Regression"})                    // minimal
   @Test(groups = {"Regression", "Sanity"})         // critical path
   @Test(groups = {"Regression", "Smoke"})          // auth or health check
   ```
2. **Register** the test class in `testng.xml` under the appropriate `<classes>` block (Auth, Common, Charge, Billing). Without this, the test will not run for any bucket.
3. **Conventions**: Use Allure `@Epic`, `@Feature`, `@Story`, `@Description`, `@Severity` where useful. Use `AuthUtils` for tokens; `ConfigManager.get(...)` for URLs; `ResponseStore` for shared IDs. Prefer data providers in `*DataProvider` or `DataProviderUtils` for data-driven tests.

## Running tests

- By bucket: `./gradlew test -Dgroups=Smoke` | `-Dgroups=Sanity` | `-Dgroups=Regression`
- Full suite: `./gradlew test`
- Single class: `./gradlew test --tests "com.externalAPIs.tests.auth.Authorization_TokenTests"`
- Allure report: generated into `reports/current/` (offline: `Allure_Offline_Single_Safe.html`). Gradle task: `generateAllureReport`.

## CI (GitHub Actions)

- **File**: `.github/workflows/api-tests.yml`
- **PR** to `main`/`develop`: runs **Smoke**; no inputs needed.
- **Push** to `main`/`develop`: runs **Regression**.
- **Manual**: workflow_dispatch — choose bucket (Smoke, Sanity, Regression, All), environment (dev/qa/stage/prod), optional test class. Default env when not set (e.g. PR/push): `qa`.
- Do not change the automatic bucket selection (PR → Smoke, push → Regression) unless the user explicitly asks.

## Key files

| File | Purpose |
|------|---------|
| `testng.xml` | Master list of test classes; order and grouping (Auth, Common, Charge, Billing). |
| `build.gradle` | Test task, `-Dgroups`/`-DtestClass` → dynamic suite, Allure, `generateAllureReport`. |
| `TEST_BUCKETS.md` | Bucket definitions, CI behavior, and how to add tests to buckets. |

When in doubt about bucket rules or CI behavior, read `TEST_BUCKETS.md` and the workflow file.
