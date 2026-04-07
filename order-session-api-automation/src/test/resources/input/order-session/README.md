# Order Session JSON scenarios

These files drive **scenario metadata only** (e.g. `ownerType`, expected status range, optional wallet flags).

**Do not put organization UUIDs, consumer IDs, or secrets here.** Set them in:

- `src/test/resources/env/<env>.properties`
- `src/test/resources/env/<env>.local.properties` (gitignored)

Java resolves bodies via `OrderSessionDataProvider`, which reads `test.*` keys from config.

| File | Purpose |
|------|---------|
| `create_payment_token_session_data.json` | `ownerType` per row (PAYOR / LOCATION / MEMBER) |
| `create_wallet_entry_session_data.json` | Flags for optional wallet fields (`includePaymentMethods`, etc.) |
