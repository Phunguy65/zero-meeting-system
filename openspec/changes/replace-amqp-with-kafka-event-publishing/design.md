# Context

The zero-meeting-system is a microservices platform with three active backend
services (`user-management`, `meeting-management`, `chat-management`) built on
Spring Boot 4 / Java 25 using Hexagonal Architecture. All services currently
include `spring-boot-starter-amqp` (RabbitMQ) via the `build-logic` service base
convention plugin, but no messaging is actually implemented — the dependency is
unused dead weight.

The `shared` module already provides `AggregateRoot<ID>` with in-memory domain
event collection (`registerEvent`, `getDomainEvents`, `clearDomainEvents`) and a
minimal `DomainEvent` interface. This infrastructure is in place but never
activated.

The goal is to replace AMQP with Kafka, activate the domain event pattern, and
establish a consistent, reliable event publishing mechanism across all three
services.

## Goals / Non-Goals

**Goals:**

- Remove all AMQP/RabbitMQ dependencies from the build
- Add Kafka (KRaft, no Zookeeper) to local dev `compose.yaml`
- Add `PublishableEvent` and `EventPublisher` abstractions to `shared` for reuse
- Implement Transactional Outbox pattern in each service for at-least-once
  delivery
- Publish domain events using CloudEvents 1.0 binary content mode via the
  official CloudEvents Java SDK (no Schema Registry)
- Cover `user-management` domain events: `UserRegistered`, `UserLoggedIn`,
  `UserDeleted`
- Cover `meeting-management` and `chat-management` with the same pattern (events
  TBD per service)

**Non-Goals:**

- Implementing Kafka consumers in any service (publisher-only scope)
- Exactly-once semantics / Kafka transactions (at-least-once + idempotent
  consumers is sufficient)
- Confluent Schema Registry or Avro/Protobuf serialization
- Kafka topic ACLs or production-grade security (TLS, SASL)
- Migrating existing RabbitMQ messages or queues (none exist — AMQP was unused)

## Decisions

### Decision 1: KRaft over Zookeeper

**Choice**: Single-node Kafka in KRaft mode (`confluentinc/cp-kafka:7.8.0` with
`KAFKA_PROCESS_ROLES=broker,controller`).

**Rationale**: Zookeeper is deprecated since Kafka 3.x and requires a second
container. KRaft mode runs Kafka standalone, simplifying `compose.yaml` and
reducing local dev startup time. For a single-node dev environment there is no
operational downside.

**Alternative considered**: Bitnami Kafka image — rejected because Confluent's
image has better documentation and is more consistent with production Confluent
Platform usage.

---

### Decision 2: Transactional Outbox over Direct KafkaTemplate.send()

**Choice**: Save an `outbox_event` row in the same JPA transaction as the domain
entity, then publish to Kafka asynchronously via a `@Scheduled` poller.

**Rationale**: Direct `kafkaTemplate.send()` after `repository.save()` risks
losing events if Kafka is unavailable at the moment of the call — the DB commit
succeeds but the event is never published. The Outbox pattern makes event
persistence atomic with domain persistence, guaranteeing at-least-once delivery
even across service restarts or Kafka downtime.

**Alternative considered**: `@TransactionalEventListener(AFTER_COMMIT)` calling
`kafkaTemplate.send()` directly — rejected because it still loses events if
Kafka is down at commit time. The outbox adds a durable buffer.

---

### Decision 3: CloudEvents 1.0 via official SDK, binary content mode

**Choice**: Use the official CloudEvents Java SDK
(`io.cloudevents:cloudevents-core:4.0.1` and
`io.cloudevents:cloudevents-kafka:4.0.1`) to produce CloudEvents-compliant Kafka
messages in **binary content mode**. Use `type` field for versioning (e.g.,
`io.github.phunguy65.zms.user.registered.v1`). Configure
`KafkaTemplate<String, CloudEvent>` with `CloudEventSerializer` as the value
serializer.

In binary content mode, CloudEvents attributes are placed in Kafka message
**headers** (prefixed `ce_`) and the event `data` payload is the Kafka message
**value** (raw JSON bytes):

```
Kafka Headers:
  ce_specversion: 1.0
  ce_id: <UUIDv7>
  ce_type: io.github.phunguy65.zms.user.registered.v1
  ce_source: user-management
  ce_time: 2026-03-07T12:00:00Z
  ce_subject: <aggregateId>
  ce_datacontenttype: application/json

Kafka Value (message body):
  {"userId":"...","email":"...","fullName":"...","registeredAt":"..."}
```

**Rationale**: The official SDK (`io.cloudevents`, CNCF graduated project)
guarantees spec compliance and eliminates hand-rolled serialization bugs. Binary
content mode is the Kafka protocol binding default — it is more efficient than
structured mode (no envelope overhead in the value) and is natively supported by
`CloudEventSerializer`. Attributes in headers are still visible in Kafka UI.

**Alternative considered**: Hand-rolled `CloudEventEnvelope` POJO serialized to
a single JSON body (structured mode) — rejected because it requires manual
maintenance, is error-prone, and duplicates what the SDK already provides
correctly. Structured mode was also considered but rejected in favour of binary
for efficiency and SDK-default alignment.

**Dependencies added** (to `gradle/libs.versions.toml`):

- `cloudevents-core = { module = "io.cloudevents:cloudevents-core", version = "4.0.1" }`
- `cloudevents-kafka = { module = "io.cloudevents:cloudevents-kafka", version = "4.0.1" }`

---

### Decision 4: `PublishableEvent` extends `DomainEvent` in shared (not modifying `DomainEvent`)

**Choice**: Introduce a new `PublishableEvent` interface that extends the
existing `DomainEvent`. Only events intended for Kafka implement
`PublishableEvent`. `DomainEvent` remains unchanged.

**Rationale**: `DomainEvent` is a minimal marker used by `AggregateRoot`.
Modifying it to add `aggregateId()`, `eventId()`, `topic()` etc. would be a
breaking change requiring all existing implementations to update. A
sub-interface keeps backward compatibility and makes the intent explicit — not
all domain events need to be published externally.

**`EventPublisher` port** lives in `shared.domain` so any service can inject it.
The Kafka adapter (`KafkaEventPublisher`) lives in each service's
`infrastructure/messaging/` — it is not shared because it depends on
`KafkaTemplate` which is service-specific config.

A `NoOpEventPublisher` in `shared.infrastructure.messaging` serves as the
default `@ConditionalOnMissingBean` fallback so services compile and run without
Kafka configured (e.g., during unit tests).

---

### Decision 5: At-least-once delivery with producer idempotence

**Choice**: `acks=all`, `enable.idempotence=true`, `retries=3`. No Kafka
transactions.

**Rationale**: Kafka producer idempotence prevents duplicate messages caused by
producer retries (network blips). Combined with the Outbox pattern, this gives
strong at-least-once guarantees without the complexity and ~15% throughput
overhead of exactly-once Kafka transactions. Consumers are expected to be
idempotent using the CloudEvents `id` field.

---

### Decision 6: Topic naming convention

**Pattern**: `{service-name}.{aggregate-type}.{event-name}`

**Examples**:

```
user-management.user.registered
user-management.user.logged-in
user-management.user.deleted
meeting-management.meeting.created
meeting-management.meeting.ended
chat-management.message.sent
```

Hyphens throughout (no dots in segment names) to avoid Kafka partition key
issues. Lowercase only.

---

### Decision 7: Outbox poller interval and retry cap

**Choice**: Poll every 1 second (`@Scheduled(fixedDelay = 1000)`). Max 3 retries
before moving to a Dead Letter Topic (`{topic}-dlt`).

**Rationale**: 1-second polling gives acceptable latency for domain events (not
real-time chat messages). After 3 failures the event is moved to DLT and marked
published to unblock the poller — a separate DLT monitor can alert ops.

## Risks / Trade-offs

| Risk                                                                                   | Mitigation                                                                                                                        |
| -------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Outbox table grows unbounded if Kafka is persistently down                             | Add a cleanup job to archive/delete rows older than 7 days with `published_at IS NOT NULL`                                        |
| `@Scheduled` poller competes across multiple service instances                         | Use `ShedLock` or Postgres advisory lock on the poller; acceptable to skip for initial implementation with single-instance deploy |
| CloudEvents `id` field (UUIDv7) requires consumers to track seen IDs for deduplication | Document this contract in the event spec; consumers use `id` as idempotency key                                                   |
| Removing AMQP breaks any future code that was planned to use it                        | AMQP was unused — no migration needed. If AMQP is needed later it can be re-added per-service                                     |
| KRaft `CLUSTER_ID` must be stable across container restarts                            | Mount Kafka data volume and set a fixed `CLUSTER_ID` env var in `compose.yaml`                                                    |
| H2 test database may not support `outbox_event` JSON column type                       | Use `TEXT` column type in H2-compatible Flyway migration under `db/h2-migration/`                                                 |

## Migration Plan

1. Update `gradle/libs.versions.toml`: add `spring-kafka`, `cloudevents-core`,
   `cloudevents-kafka`; remove AMQP entries
2. Update `build-logic` convention plugin: swap AMQP for Kafka dependency
3. Update `shared` module: add `PublishableEvent`, `EventPublisher`,
   `NoOpEventPublisher`
4. Update `compose.yaml`: replace Zookeeper+Kafka (if present) with KRaft
   Kafka + Kafka UI
5. Implement per-service: domain events → outbox entity → Kafka adapter → Flyway
   migration → config
6. Order: `user-management` first (most complete domain model), then
   `meeting-management`, then `chat-management`

**Rollback**: Since AMQP was unused, reverting means removing `spring-kafka` and
restoring `spring-boot-starter-amqp` in `build-logic`. No data migration
required.
