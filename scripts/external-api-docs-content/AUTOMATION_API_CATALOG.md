# External API catalog — api-automation-smart-ci-poc (root module)

Automation exercises the APIs below. Base URLs come from environment properties (`base.url`, `secure.base.url`, `merchant.base.url`, `transactions.base.url`, auth URLs from `ConfigManager`); paths below are relative unless noted.

## Authentication

| Flow | Method | Path / endpoint | Notes |
|------|--------|-----------------|-------|
| OAuth client credentials (standard) | POST | From `ConfigManager.getAuthUrl(false)` | `grant_type=client_credentials`, Basic auth, form body |
| OAuth secure client | POST | From `secure.auth.url` | Same grant; secure username/password |
| Glofox client token | POST | `{base.url without /api}/api/token` | Client credentials |
| Glofox secure client token | POST | `{secure.base.url}/api/token` | Client credentials |

## Common APIs (under configured base URLs)

| Area | Method | Path | Typical auth / headers |
|------|--------|------|-------------------------|
| Client | POST | `/api/client` | Bearer (secure) |
| Organization | POST | `/api/organization` | Bearer (Glofox client) |
| Location | POST | `/api/location` | Bearer (Glofox client), tenant context from store |
| Company | POST | `/api/abcpg/company` | Bearer (secure), `ABCFS-TENANT-ID` |
| Merchant | POST | `/api/abcpg/merchant` | Bearer (secure), `ABCFS-TENANT-ID` |
| Merchant | PATCH | `/api/abcpg/merchant/{merchantId}` | Bearer (secure), `ABCFS-TENANT-ID` |
| Processor | POST | `/api/abcpg/processor` | Bearer (secure), `ABCFS-TENANT-ID` |
| Terminal | POST | `/api/abcpg/terminal` | Bearer (secure), `ABCFS-TENANT-ID` |
| Payment session consumer | POST | `/api/ors/payment-session-consumer` | Bearer (Glofox client) |
| Bank payment token | POST | `/api/abcpg/payment-token/bank` | Bearer (secure), `ABCFS-TENANT-ID` |
| Client profile | POST | `/api/abcblg/client-profile` | Bearer (Glofox client) |
| Client bank account | POST | `/api/abcblg/bank-account` | Bearer (Glofox client), `ABCFS-TENANT-ID` |

## Charge / payment gateway

| Method | Path | Notes |
|--------|------|-------|
| GET | `{merchant.base.url}/merchantDetails/{merchantId}` | Bearer (secure) |
| GET | `{transactions.base.url}/transactionDetails/{transactionId}` | Bearer (secure) |
| POST | `/api/ors/payment-token/session/add-token` **or** `/api/abcpg/payment-session` | Variant selected in tests |
| POST | `/api/abcpg/payments/charge` | ACH charge (`secure.base.url`) |
| POST | `/api/payment-gateway/payments/refund` | Currently hardcoded host in tests — should be config-driven for non-dev |

## Billing

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/abcpg/billing-accounts/{billingAccountId}/transfer` | Query `dryRun`; body `locationId`, optional `metadata` |
| GET | `/api/abcpg/billing-accounts/{billingAccountId}/transfers/{transferId}` | Follow-up to create transfer |

## TestNG coverage (root suite)

Classes registered in `testng.xml`: auth (`Authorization_TokenTests`, `Secure_TokenTests`, `Glofox_ClientTokenTests`, `Glofox_SecureClientTokenTests`), common create/update flows, charge and refund tests, `CreateBillingAccountTransferTests`.
