# Billing Account Transfer API – Automation Template

This folder is a **reusable template** for API automation. Use it as a reference to add new APIs with minimal changes.

## API Under Test

**Create Billing Account Transfer**

- **Method:** POST  
- **Path:** `{baseUrl}/api/abcpg/billing-accounts/{billingAccountId}/transfer`  
- **Auth:** Bearer token  
- **Required header:** `ABCFS-ORGANIZATION-ID`  
- **Path param:** `billingAccountId` (UUID)  
- **Query param:** `dryRun` (boolean, default false)  
- **Body:** `locationId` (required, UUID), `metadata` (optional key-value)

## What’s in This Template

| Item | Purpose |
|------|--------|
| `CreateBillingAccountTransferTests.java` | Test class with TDD-style scenarios (positive + negative + chained GET) |
| `BillingTransferResponseStore.java` | Persists 2xx transfer/dry-run fields to `ResponseStore` for reuse |
| `BillingDataProvider.java` | Data provider for JSON-driven scenarios (reads target `locationId` from store) |
| `create_billing_account_transfer_data.json` | Test data for valid transfer / dry-run cases |

## Scenarios Covered (TDD)

1. **Positive**
   - Valid transfer (execute) → 2xx, schema validated  
   - Valid dry run → 2xx, eligibility only  
   - Minimal payload (only `locationId`) → 2xx  

2. **Negative**
   - Missing `locationId` → 400/422  
   - Invalid `locationId` format → 400/404/422  
   - Non-existent `billingAccountId` → 404/400  
   - No Authorization → 401  
   - No `ABCFS-ORGANIZATION-ID` → 400/403  
   - Invalid metadata (e.g. key > 255 chars) → 400/422  
   - Dry run returns eligibility status  

## How to Run

From project root:

```bash
# All tests (default suite)
./gradlew test

# Only Billing tests
./gradlew test -Dgroups=Billing

# Specific class
./gradlew test -DtestClass=com.externalAPIs.tests.billing.CreateBillingAccountTransferTests

# With env and billing account ID (for real backend)
./gradlew test -Denv=qa -Dbilling.account.id=<your-billing-account-uuid> -Dgroups=Billing
```

Environment variable alternative for CI:

```bash
export BILLING_ACCOUNT_ID=6f1e19b3-1ed2-44a2-8de9-3e2c4c2db5e6
./gradlew test -Dgroups=Billing
```

## Reusing This Template for Another API

1. **Copy this package** (`billing` folder) and rename (e.g. `subscription`, `invoicing`).  
2. **Update the test class**
   - Change `BILLING_TRANSFER_PATH` to your endpoint path (use `%s` for path params).  
   - Rename class and methods; keep the same structure (data-driven positive + explicit negative tests).  
3. **Update the data provider**
   - Point to your new JSON path.  
   - Adjust keys (e.g. replace `locationId` with your required fields).  
4. **Update the JSON**
   - Same structure: scenario name, description, payload, expected status range.  
5. **Add your class to `testng.xml`** in the appropriate `<test>` block.

## Dependencies

- **Token:** Secure client token (`AuthUtils.getSecureClientToken()`).  
- **Organization ID:** From `ResponseStore.get("glofoxOrgId")` or default tenant.  
- **Location / transfer chaining:** `dryRunTrue_returnsEligibilityStatus_andPersistsForChaining` (priority 1) writes to `ResponseStore`: `billingAccountTransferId`, `billingTransferTargetLocationId`, `billingAccountTransferStatus`, `reasonCodeId` (if present), full JSON under `billingTransferLastResponseJson`, and mirrors location as `locationId` for older flows. `BillingDataProvider` and negative tests use `BillingTransferResponseStore.resolveTargetLocationId(...)`. `getTransferById_usingStoredTransferId_expect2xx` reads `billingAccountTransferId` for the GET follow-up.

## Demo / Presentation Notes

- Run with `-Dgroups=Billing` for a focused suite.  
- Show Allure report: `reports/current/Allure_Offline_Single_Safe.html` (or `./gradlew generateAllureReport`).  
- Use `-Dbilling.account.id=<real-uuid>` against QA/Preprod for live demo.
