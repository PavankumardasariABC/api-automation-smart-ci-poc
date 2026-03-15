# Test Run Summary — For Presentation

## Latest run (just executed)

| Metric | Value |
|--------|--------|
| **Total tests** | 21 |
| **Passed** | 19 |
| **Failed** | 1 |
| **Skipped** | 1 |
| **Duration** | ~48.5 seconds |
| **Success rate** | 95% |

### Failed test (not from Billing)
- **CreateRefundPaymentTests.createRefundPayment[0]** — Refund API (Charge flow). Failure is likely environment/data specific (e.g. token or reference). Billing Account Transfer tests were not in this run because the suite ran the full Regression flow.

### Where to open the report
1. **Allure report (best for demo)** — open **via a local server** (otherwise Safari shows "Loading..." forever):
   - From project root run: **`./serve-allure-report.sh`**
   - Then in browser open: **http://localhost:8765/Allure_Offline_Single_Safe.html**
   - Or: `cd reports/current && python3 -m http.server 8765` then open the same URL.

2. **Gradle HTML report** (file:// is fine):  
   **`build/reports/tests/test/index.html`**

### For a Billing-only demo run
To get a report with only Billing Account Transfer scenarios (no Refund failure):

```bash
./gradlew test -Dgroups=Billing
```

Then open again: **`reports/current/Allure_Offline_Single_Safe.html`**

### Quick presentation flow
1. Open **presentation/index.html** — explain advantages, speed, CI/CD, failure buckets.
2. Open **reports/current/Allure_Offline_Single_Safe.html** — show test results, steps, attachments.
3. Optionally show **build/reports/tests/test/index.html** for the classic pass/fail summary.
