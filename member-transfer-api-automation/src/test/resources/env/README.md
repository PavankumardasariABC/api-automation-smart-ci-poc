# Environment configuration (member-transfer-api-automation)

- **`{dev,qa,stage,beta,prod}.properties`** — committed defaults. Default Gradle env is `qa` (see `build.gradle` `test` → `systemProperty 'env'…`).

- **`{env}.local.properties`** — optional overrides (gitignored). Copy from `qa.local.properties.example` and set OAuth user/pass and UUIDs. Loaded **on top of** the committed file.
  - Optional negative-auth keys used by status scenarios:
    - `member.transfer.auth.credentials.no.client.authority`
    - `member.transfer.auth.credentials.no.user.permission`
    - `member.transfer.auth.credentials.no.inquiry.client.authority`
    - `member.transfer.auth.credentials.no.inquiry.user.permission`
  - Optional Kafka verification keys (for `MemberTransferKafkaPublishTests`):
    - `member.transfer.kafka.bootstrap.servers`
    - `member.transfer.kafka.topic` (default `bulk-account-transfer`)
    - `member.transfer.kafka.legacy.topic` (default `account-transfer`, used by negative test)
    - `member.transfer.kafka.negative.wrong.topic` (default `bulk-account-transfer-invalid`, used by negative test)
    - `member.transfer.kafka.security.protocol`, `member.transfer.kafka.sasl.mechanism`
    - `member.transfer.kafka.sasl.username`, `member.transfer.kafka.sasl.password` (or `member.transfer.kafka.sasl.jaas.config`)
    - `member.transfer.kafka.auto.offset.reset`, `member.transfer.kafka.max.wait.seconds`, `member.transfer.kafka.poll.interval.ms`

- **External merge** — `external.api.env.path` in a properties file; copy from `config/member-transfer-env.local.properties.example` to a gitignored path and point the property to it. Same idea as the sibling `order-session-api-automation` module.

- **CLI**: `./gradlew test -Denv=qa -Dgroups=MemberTransfer`
