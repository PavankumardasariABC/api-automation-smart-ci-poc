# OpenAPI ↔ automation mapping (Order Session API)

Spec reference: `openapi-spec-5.yaml` (operationId and paths).

**Primary suite:** `OrderSessionApiE2ETests` (same structural pattern as root POC `CreateBillingAccountTransferTests`).  
JSON scenarios: `src/test/resources/input/order-session/*.json`.

| operationId | Method & path | Automation class / method | Groups | Notes |
|-------------|---------------|---------------------------|--------|--------|
| — | — | `ExternalApiAuthBootstrapTests.warmBearerFromOAuthOrJwt` | Smoke, Sanity, Regression | OAuth via `config/oauth-env.local.properties` or JWT. |
| — | — | `OrderSessionE2eHighLevelTest.openapiDocumented_endToEnd_flow` | Regression | Allure narrative only. |
| `createPaymentTokenSession` | `POST /payment-session/tokens` | `OrderSessionApiE2ETests.createPaymentTokenSession_expectedSuccess` | OrderSession, Sanity, Regression | `create_payment_token_session_data.json`; **401** retry. |
| `createPaymentTokenSession` | `POST /payment-session/tokens` | `OrderSessionApiE2ETests.createPaymentTokenSession_*_expectBadRequest` | OrderSession, Regression | Missing fields / invalid UUID. |
| `createPaymentTokenSession` | `POST /payment-session/tokens` | `OrderSessionApiE2ETests.createPaymentTokenSession_withoutAuth_expectUnauthorized` | OrderSession, Smoke, Regression | `401`. |
| `createPaymentTokenSession` | `POST /payment-session/tokens` | `OrderSessionApiE2ETests.createPaymentTokenSession_withoutOrganization_expectRejected` | OrderSession, Regression | No `ABCFS-ORGANIZATION-ID`. |
| `getPaymentTokenSession` | `GET /payment-session/tokens/{id}` | `OrderSessionApiE2ETests.getPaymentTokenSession_validId_expectedSuccess` | OrderSession, Sanity, Regression | Last POST id in `ResponseStore`. |
| `getPaymentTokenSession` | `GET /payment-session/tokens/{id}` | `OrderSessionApiE2ETests.getPaymentTokenSession_unknownId_expectNotFound` | OrderSession, Regression | `404` (real org UUID). |
| `getPaymentTokenSession` | `GET /payment-session/tokens/{id}` | `OrderSessionApiE2ETests.getPaymentTokenSession_withoutAuth_expectUnauthorized` | OrderSession, Smoke, Regression | `401`. |
| `createWalletEntrySession` | `POST /payment-session/wallet-entries` | `OrderSessionApiE2ETests.createWalletEntrySession_expectedSuccess` | OrderSession, Sanity, Regression | `create_wallet_entry_session_data.json`. |
| `createWalletEntrySession` | `POST /payment-session/wallet-entries` | `OrderSessionApiE2ETests.createWalletEntrySession_missing*_expectBadRequest` | OrderSession, Regression | Missing `ownerId` / `user`. |
| `createWalletEntrySession` | `POST /payment-session/wallet-entries` | `OrderSessionApiE2ETests.createWalletEntrySession_withoutAuth_expectUnauthorized` | OrderSession, Smoke, Regression | `401`. |
| `getWalletEntrySession` | `GET /payment-session/wallet-entries/{id}` | `OrderSessionApiE2ETests.getWalletEntrySession_validId_expectedSuccess` | OrderSession, Sanity, Regression | `200`; no `url` on GET. |
| `getWalletEntrySession` | `GET /payment-session/wallet-entries/{id}` | `OrderSessionApiE2ETests.getWalletEntrySession_unknownId_expectNotFound` | OrderSession, Regression | `404`. |
| `getWalletEntrySession` | `GET /payment-session/wallet-entries/{id}` | `OrderSessionApiE2ETests.getWalletEntrySession_withoutAuth_expectUnauthorized` | OrderSession, Smoke, Regression | `401`. |

Split test classes from earlier iterations remain under `com.ordersession.tests` for reference but are **not** in `testng.xml`.

## Headers (spec)

- `Authorization: Bearer <token>`
- `ABCFS-ORGANIZATION-ID` (tenant)
- `Content-Type: application/json` for POST bodies

## E2E scope

Hosted paypage completion is out of band; automation validates **API contract**, **auth**, **validation**, **create + GET** per OpenAPI.
