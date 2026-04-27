# Test buckets (TestNG groups)

| Group | Purpose |
|--------|---------|
| `Regression` | All tests in the suite (default full run) |
| `Smoke` / `MT_Smoke` | OAuth token only (fast) |
| `Sanity` / `MT_Sanity` | Token + key inquiry and status read |
| `Progression` / `MT_Progression` / `E2E` | Chained: token → inquiry → GET status |
| `MemberTransfer` | Entire member-transfer scope |

**Examples**

```bash
./gradlew test -Denv=qa -Dgroups=MemberTransfer
./gradlew test -Denv=qa -Dgroups=MT_Progression
./gradlew test -DtestClass=com.membertransfer.tests.MemberTransferE2EProgressionTests
```

`build.gradle` generates a dynamic `build/dynamic-suite.xml` when `-Dgroups=…` or `-DtestClass=…` is set (see order-session module).
