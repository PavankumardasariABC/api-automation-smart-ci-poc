# API Automation Smart CI (POC)

Java **17**, **Gradle**, **TestNG**, **Rest Assured**, and **Allure** API automation with **Smoke / Sanity / Regression** buckets and **GitHub Actions** “smart CI” (PR → Smoke, push → Regression, manual bucket selection).

## Repository layout

| Area | Purpose |
|------|---------|
| Root Gradle project (`src/`, `testng.xml`) | External APIs POC tests (`com.externalAPIs.tests`) |
| [`billing-api-automation/`](billing-api-automation/) | Billing API automation module |
| [`order-session-api-automation/`](order-session-api-automation/) | Order Session / payment-session E2E automation |
| [`presentation/`](presentation/) | Leadership / demo materials |
| [`scripts/`](scripts/) | Helpers (e.g. external API docs, Cursor skills bundle) |
| [`.github/workflows/`](.github/workflows/) | CI workflows per area |

Canonical bucket rules and CI behavior: **[`TEST_BUCKETS.md`](TEST_BUCKETS.md)**.

## Run tests (root project)

```bash
./gradlew test -Dgroups=Smoke
./gradlew test -Dgroups=Regression
```

Module-specific commands and env: see each module’s `README.md` (if present) and `TEST_BUCKETS.md`.

## Cursor Agent Skills (share with your team)

This repo includes **Cursor** project skills under [`.cursor/skills/`](.cursor/skills/) so the assistant follows the same TestNG bucket rules, `testng.xml` registration, Gradle `-Dgroups`, and CI expectations.

To **package those skills** for people who use another repository (zip, internal wiki, email):

1. From the **repository root**, run:

   ```bash
   ./scripts/cursor-skills-plugin/build-bundle.sh
   ```

2. Share **`build/cursor-api-automation-skills-bundle.zip`** (or the folder `build/cursor-api-automation-skills-bundle/`). That output is gitignored; build it when you need a fresh drop.

3. Recipients unzip and run **`./install.sh /path/to/their/project`** as described in **`README.txt`** inside the bundle.

Full recipient and maintainer instructions are in **[`scripts/cursor-skills-plugin/BUNDLE_README.txt`](scripts/cursor-skills-plugin/BUNDLE_README.txt)** (same text as `README.txt` in the zip).

Skills included in the bundle:

- **`api-automation-smart-ci`** — conventions for this automation style and CI.
- **`test-buckets-rules`** — rules aligned with **`TEST_BUCKETS.md`**.

This is not a VS Code Marketplace extension; it is the standard `.cursor/skills/<name>/SKILL.md` layout, packaged for easy sharing.
