# Order Session API — payment session (automation alignment)

Source: `order-session-api-automation` in api-automation-smart-ci-poc.

Base URL key: `order.session.base.url` (must include `/api/order-session` or equivalent service prefix as configured per environment).

## Operations

| operationId (OpenAPI naming) | Method | Path |
|------------------------------|--------|------|
| `createPaymentTokenSession` | POST | `/payment-session/tokens` |
| `getPaymentTokenSession` | GET | `/payment-session/tokens/{id}` |
| `createWalletEntrySession` | POST | `/payment-session/wallet-entries` |
| `getWalletEntrySession` | GET | `/payment-session/wallet-entries/{id}` |

## Headers

- `Authorization: Bearer <token>`
- `ABCFS-ORGANIZATION-ID` (tenant)
- `Content-Type: application/json` on POST bodies

## Automation mapping

See [order-session-openapi-test-matrix.md](order-session-openapi-test-matrix.md) for the full `operationId` → TestNG class/method matrix.

Hosted paypage completion is out of band; automation validates API contract, auth, validation, and create + GET pairs per OpenAPI.
