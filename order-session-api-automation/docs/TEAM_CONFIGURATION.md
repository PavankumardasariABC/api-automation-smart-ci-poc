# Team configuration — change only config, reuse automation

Everyone uses the **same code**. Switch **environment** and **credentials** only via properties files (no Java changes).

## Two config layers

### 1) Order Session module (`order-session-api-automation/`)

| What | Where | Committed? |
|------|--------|------------|
| Environment URLs (QA / Dev / Stage / Beta), defaults, `external.api.env.path` | `src/test/resources/env/{qa,dev,stage,beta}.properties` | Yes (no secrets) |
| **Organization UUID, test data, optional JWT, optional OAuth overrides** | `src/test/resources/env/{env}.local.properties` | **No** — gitignored; copy from `*.local.properties.example` |

**Precedence:** `{env}.local.properties` → `{env}.properties` → external merge file (see below).

### 2) OAuth merge file (inside this folder)

Used for **token fetch** when `auth.bearer.token=__FETCH_FROM_EXTERNAL__` and `order.session.auth.source=GLOFOX|CLIENT|SECURE`.

| What | Where | Committed? |
|------|--------|------------|
| `base.url`, `auth.url`, `credentials`, `secure.*`, etc. | File at `external.api.env.path` (default **`config/oauth-env.local.properties`**) | **Do not commit** real secrets — file is gitignored |

**Setup:** copy [`config/oauth-env.local.properties.example`](../config/oauth-env.local.properties.example) → `config/oauth-env.local.properties` and fill values. All env files (`qa`, `dev`, `stage`, `beta`) default to that path; override in `*.local.properties` only if you need a different file per machine.

---

## Switch environment (QA vs Dev vs Stage vs Beta)

```bash
cd order-session-api-automation
./gradlew test -Denv=qa -Dgroups=Smoke
./gradlew test -Denv=dev -Dgroups=Regression
```

Use **one** OAuth file for every `-Denv` unless you maintain separate copies (e.g. different `base.url` for stage vs QA) and point `external.api.env.path` in the matching `*.local.properties`.

---

## Keys you normally set (company-specific)

### In `{env}.local.properties` (recommended for secrets)

| Key | Purpose |
|-----|--------|
| `abcfs.organization.id` | Tenant header `ABCFS-ORGANIZATION-ID` (real UUID). **Primary key.** |
| `organization.id` | **Alias:** if `abcfs.organization.id` is still a template (`REPLACE_…`, `YOUR_…`, `TODO`), this value is used. You can leave the committed placeholder and set only `organization.id` in `*.local.properties`. |
| `test.consumer.id` | Order Session consumer (integration tests) |
| `test.location.id` | Location UUID |
| `test.owner.id` | Wallet owner |
| `test.wallet.user.id` / `test.wallet.user.external.id` | Wallet user |
| `auth.bearer.token` | Optional: paste JWT instead of OAuth fetch |
| `external.api.env.path` | Optional: override path to OAuth file |

### In module `{env}.properties` (shared non-secret defaults)

| Key | Purpose |
|-----|--------|
| `order.session.base.url` | Order Session API base (e.g. `https://api-qa.../api/order-session`) |
| `order.session.auth.source` | `GLOFOX`, `CLIENT`, or `SECURE` |
| `external.api.env.path` | Default: `config/oauth-env.local.properties` |

### In OAuth merge file

| Key | Purpose |
|-----|--------|
| `base.url` | Platform base (Glofox token URL derived from this) |
| `auth.url` | Client-credentials token endpoint (`CLIENT` mode) |
| `credentials` | `clientId:clientSecret` for `CLIENT` mode |
| `secure.auth.url`, `secure.username`, `secure.password` | `SECURE` mode |

---

## CI (GitHub Actions)

- Set secret **`ORDER_SESSION_JWT`** to skip OAuth fetch, **or**
- Generate `config/oauth-env.local.properties` in a CI step from secrets and run tests (path already matches defaults).

---

## Quick start for a new teammate

1. Clone or unzip so `order-session-api-automation` is the project root.
2. Copy `src/test/resources/env/qa.local.properties.example` → `src/test/resources/env/qa.local.properties`.
3. Fill `abcfs.organization.id` and any `test.*` IDs.
4. Copy `config/oauth-env.local.properties.example` → `config/oauth-env.local.properties` and fill OAuth keys, **or** set `ORDER_SESSION_JWT`.
5. Run QA Smoke:

```bash
cd order-session-api-automation
./gradlew test -Denv=qa -Dgroups=Smoke
```

See also `src/test/resources/env/README.md`.
