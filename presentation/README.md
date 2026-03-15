# Leadership Presentation — API Automation Framework

This folder contains a **single-page website** for presenting the API Automation Framework to **VP**, **Indian General Manager**, and other leadership.

## How to present

1. **Open the page**
   - Double-click `index.html` to open in a browser, or
   - From project root: `open presentation/index.html` (macOS), or
   - Use a simple local server for a clean URL:
     ```bash
     cd presentation && python3 -m http.server 8080
     ```
     Then open: **http://localhost:8080**

2. **Sections to walk through**
   - **Advantages** — Why this framework (template reuse, token refresh, Allure, TDD).
   - **Speed** — Comparison with legacy MT scripts; faster runs with groups/class selection.
   - **CI/CD & PR** — PR-triggered runs, smart selection, rerun without manual intervention.
   - **Failure categorization** — Microservice vs data vs auth vs environment.
   - **Future** — CI/CD gates, failure buckets, smart selection, auto-rerun.
   - **Demo** — Billing Account Transfer, bearer token refresh, Gradle commands.

3. **Live demo (optional)**
   - Run: `./gradlew test -Dgroups=Billing`
   - Open Allure report: `reports/current/Allure_Offline_Single_Safe.html`
   - Show PR workflow and report link in GitHub Actions (if repo is connected).

## Content summary

- **Advantages:** Template reuse, bearer token auto-refresh, dynamic test selection, Allure, env-based config, TDD coverage.
- **Speed:** API-only, group/class filters, faster feedback vs long-running MT scripts.
- **CI/CD:** PR builds, Allure on GitHub Pages, PR comment with report links; rerun = re-run workflow (no manual re-trigger).
- **Failure buckets:** Microservice (5xx), Data (4xx validation), Auth (401 + auto-refresh), Environment (config).
- **Future:** Gates, failure dashboards, smart selection on PR, auto-rerun on failure.
