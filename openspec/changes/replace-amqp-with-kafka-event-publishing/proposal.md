# Why

The system currently uses RabbitMQ (Spring AMQP) as its messaging layer, but
lacks a domain event publishing strategy. As the platform grows across
`user-management`, `meeting-management`, and `chat-management`, services need a
reliable, decoupled way to broadcast domain events (e.g., `UserRegistered`,
`MeetingCreated`) to downstream consumers. Kafka's durable log model,
partition-based ordering, and replay capability make it a better fit for
event-driven inter-service communication than RabbitMQ's transient queue model.

## What Changes

- **BREAKING**: Remove `spring-boot-starter-amqp`, `spring-rabbit-stream`, and
  `spring-integration-amqp` from all services (via `build-logic` convention
  plugin)
- Add `spring-kafka` to the `build-logic` service base plugin so all services
  get Kafka support
- Add `PublishableEvent` interface and `EventPublisher` port to the `shared`
  module for reuse across all services
- Add `NoOpEventPublisher` default implementation in `shared` (used when no
  Kafka bean is present)
- Implement `KafkaEventPublisher` adapter + Transactional Outbox pattern in
  `user-management`
- Add domain events (`UserRegisteredEvent`, `UserLoggedInEvent`,
  `UserDeletedEvent`) to `user-management`
- Implement `KafkaEventPublisher` adapter + domain events in
  `meeting-management`
- Implement `KafkaEventPublisher` adapter + domain events in `chat-management`
- Replace Zookeeper-based Kafka with KRaft single-node setup in `compose.yaml`
- Add Kafka UI container to `compose.yaml` for local development debugging
- Add `outbox_event` Flyway migration to each service that publishes events

## Capabilities

### New Capabilities

- `kafka-event-publishing`: Cross-service domain event publishing via Kafka
  using the Transactional Outbox pattern, CloudEvents JSON envelope, and a
  shared `EventPublisher` port abstraction

### Modified Capabilities

<!-- No existing spec-level requirements are changing — this is a new infrastructure capability -->

## Impact

- **`services/shared`**: New `PublishableEvent`, `EventPublisher` interfaces in
  `domain/`; new `NoOpEventPublisher` in `infrastructure/messaging/`
- **`services/user-management`**: New domain events, Kafka adapter, outbox
  table, Kafka config; remove AMQP deps
- **`services/meeting-management`**: New domain events, Kafka adapter, outbox
  table, Kafka config; remove AMQP deps
- **`services/chat-management`**: New domain events, Kafka adapter, outbox
  table, Kafka config; remove AMQP deps
- **`build-logic`**: Replace `spring-boot-starter-amqp` with `spring-kafka` in
  service base convention plugin
- **`gradle/libs.versions.toml`**: Add `spring-kafka` library entry; remove AMQP
  entries
- **`compose.yaml`** (root or per-service): Replace Zookeeper+Kafka with KRaft
  Kafka; add Kafka UI
- **No API contract changes** — event publishing is internal infrastructure;
  REST endpoints unchanged
- **No frontend impact** — Android and Web clients are unaffected
