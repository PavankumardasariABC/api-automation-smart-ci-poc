# Test Buckets (Smoke, Sanity, Regression)

Tests are grouped into three buckets so CI can run the right set **before** and **after** PR merge without manual intervention.

## Buckets

| Bucket       | Purpose              | When it runs in CI        | Contents |
|-------------|----------------------|---------------------------|----------|
| **Smoke**   | Fast health check    | **Before merge** (on PR)  | Auth (all token flows) + Get Merchant Details + Get Transaction Details (priority 1) |
| **Sanity**  | Critical path        | Manual / workflow_dispatch| Smoke + Create Client, Create Merchant, Create Organization, Create Payment Token Session |
| **Regression** | Full suite       | **After merge** (push to main/develop) | All tests across Auth, Common, Charge, Billing |

## CI Behavior (no manual intervention)

- **Pull request** to `main` or `develop`  
  → Workflow runs **Smoke** automatically. Merge is blocked until Smoke passes.

- **Push** to `main` or `develop` (e.g. after merge)  
  → Workflow runs **Regression** automatically for full coverage.

- **Manual run** (Actions → "API Automation CI" → Run workflow)  
  → You choose bucket: **Smoke**, **Sanity**, **Regression**, or **All**, plus environment and optional test class.

## How groups are used

- Tests are tagged with TestNG `groups` (e.g. `groups = {"Regression", "Smoke"}`).
- Gradle runs tests with `-Dgroups=Smoke` or `-Dgroups=Regression` etc.; see `build.gradle` and `testng.xml`.
- To run locally:
  - Smoke: `./gradlew test -Dgroups=Smoke`
  - Sanity: `./gradlew test -Dgroups=Sanity`
  - Regression: `./gradlew test -Dgroups=Regression`
  - All: `./gradlew test`

## Adding new tests to a bucket

Add the right group(s) to your `@Test` annotation:

- **Smoke**: Only for auth and 1–2 critical read/health checks.
- **Sanity**: Smoke + a few critical create/payment flows.
- **Regression**: Add `Regression` to every test; add `Smoke` or `Sanity` only if it belongs in those buckets.

Example:

```java
@Test(groups = {"Regression", "Sanity"})
public void createSomething() { ... }
```

Ensure the test class is included in `testng.xml` so it is part of the suite when running by group.
