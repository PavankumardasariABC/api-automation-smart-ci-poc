# Environment properties

## Files

| File pattern | Role |
|--------------|------|
| `qa.properties`, `dev.properties`, `stage.properties`, `beta.properties` | **Committed** defaults: Order Session base URL, auth mode, path to OAuth merge file. |
| `qa.local.properties`, `dev.local.properties`, … | **Not committed** (gitignored). Put **organization UUID**, **test data**, optional **JWT**, optional **credential overrides** here. |
| `*.local.properties.example` | **Committed** templates — copy to `*.local.properties` and fill in. |

## Run QA

```bash
# From order-session-api-automation/
# OAuth: create config/oauth-env.local.properties from config/oauth-env.local.properties.example (or set ORDER_SESSION_JWT)
./gradlew test -Denv=qa -Dgroups=Smoke
```

`-Denv` picks `qa.properties` + optional `qa.local.properties`.

**Organization:** set `abcfs.organization.id` or `organization.id` only in properties — all tests use `OrderSessionTestConfig.organizationId()` (no hardcoded tenant in Java or JSON under `input/order-session/`).

## Full guide

See **`docs/TEAM_CONFIGURATION.md`**.
