---
name: test-buckets-rules
description: Enforces Test Buckets (Smoke, Sanity, Regression) rules from TEST_BUCKETS.md. Use when adding or reviewing tests, assigning groups, validating CI behavior, or ensuring TEST_BUCKETS.md and the codebase stay aligned with the documented rules.
---

# Test Buckets Rules (from TEST_BUCKETS.md)

Use this skill to ensure every test and the report follow the canonical bucket definitions and CI behavior. When in doubt, treat [TEST_BUCKETS.md](../../../TEST_BUCKETS.md) as the source of truth.

## Bucket definitions (must match TEST_BUCKETS.md)

| Bucket        | Purpose                | When it runs in CI              | Contents |
|---------------|------------------------|----------------------------------|----------|
| **Smoke**     | Fast health check      | **Before merge** (on PR)         | Auth (all token flows) + Get Merchant Details + Get Transaction Details (priority 1) |
| **Sanity**    | Critical path          | Manual / workflow_dispatch only  | Smoke + Create Client, Create Merchant, Create Organization, Create Payment Token Session |
| **Regression**| Full suite             | **After merge** (push to main/develop) | All tests across Auth, Common, Charge, Billing |

## CI behavior (no manual intervention)

- **Pull request** to `main` or `develop` → Workflow runs **Smoke**. Merge blocked until Smoke passes.
- **Push** to `main` or `develop` (e.g. after merge) → Workflow runs **Regression**.
- **Manual run** (Actions → "API Automation CI" → Run workflow) → User chooses bucket: Smoke, Sanity, Regression, or All; plus environment and optional test class.

## Rules for tests

1. **Groups**: Tests are tagged with TestNG `groups` (e.g. `groups = {"Regression", "Smoke"}`). Gradle uses `-Dgroups=Smoke` etc.; see `build.gradle` and `testng.xml`.

2. **Adding new tests** — assign the right group(s):
   - **Smoke**: Only auth and 1–2 critical read/health checks (e.g. Get Merchant Details, Get Transaction Details priority 1).
   - **Sanity**: Smoke plus a few critical create/payment flows (Create Client, Create Merchant, Create Organization, Create Payment Token Session).
   - **Regression**: Add `Regression` to **every** test; add `Smoke` or `Sanity` only if the test belongs in those buckets.

3. **Example**:
   ```java
   @Test(groups = {"Regression", "Sanity"})
   public void createSomething() { ... }
   ```

4. **testng.xml**: The test class must be included in `testng.xml` so it runs when executing by group. Without this, the test will not run for any bucket.

## Validation checklist

When adding or changing tests, or when verifying the report/codebase:

- [ ] Every `@Test` has at least **Regression** in its groups.
- [ ] Smoke group is used only for: auth (all token flows), Get Merchant Details, Get Transaction Details (priority 1).
- [ ] Sanity group is used for: Smoke tests + Create Client, Create Merchant, Create Organization, Create Payment Token Session.
- [ ] New test class is listed in `testng.xml` under the correct package (Auth, Common, Charge, Billing).
- [ ] Local run commands match the report: `./gradlew test -Dgroups=Smoke`, `-Dgroups=Sanity`, `-Dgroups=Regression`, or `./gradlew test` for all.
- [ ] TEST_BUCKETS.md table and "Adding new tests to a bucket" section are consistent with the above rules.

## Running by bucket (for reference)

- Smoke: `./gradlew test -Dgroups=Smoke`
- Sanity: `./gradlew test -Dgroups=Sanity`
- Regression: `./gradlew test -Dgroups=Regression`
- All: `./gradlew test`

## Key files

| File | Purpose |
|------|---------|
| `TEST_BUCKETS.md` | Source of truth for bucket definitions, CI behavior, and how to add tests. |
| `testng.xml` | Master list of test classes; required for any test to run by group. |
| `.github/workflows/api-tests.yml` | CI: PR → Smoke, push → Regression, manual → choose bucket. |

When updating TEST_BUCKETS.md, keep the bucket table, CI behavior section, "How groups are used", and "Adding new tests to a bucket" in sync with this skill and with `api-automation-smart-ci` skill.
